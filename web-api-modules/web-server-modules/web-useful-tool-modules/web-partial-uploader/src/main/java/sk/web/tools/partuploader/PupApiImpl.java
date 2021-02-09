package sk.web.tools.partuploader;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import sk.exceptions.JskProblemException;
import sk.services.async.IAsync;
import sk.services.async.ISizedSemaphore;
import sk.services.except.IExcept;
import sk.services.kv.IKvUnlimitedStore;
import sk.services.kv.keys.KvKeyWithDefault;
import sk.services.kv.keys.KvSimpleKeyWithName;
import sk.services.retry.IRepeat;
import sk.services.time.ITime;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.ifaces.Identifiable;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Re;
import sk.utils.tuples.X;
import sk.utils.tuples.X1;
import sk.utils.tuples.X2;
import sk.web.exceptions.IWebExcept;
import sk.web.tools.partuploader.api.*;

import javax.inject.Inject;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RequiredArgsConstructor
@Log4j2
public abstract class PupApiImpl<META, FINISH>
        implements PupPublicApi<META, FINISH>, PupPrivateApi<META, FINISH>, Identifiable<String> {
    @Inject IKvUnlimitedStore metaStore;
    @Inject PupIByteStorage byteStorage;
    @Inject PupIUserProvider<META> userProvider;
    @Inject PubIConfig config;

    @Inject ITime times;
    @Inject IRepeat repeat;
    @Inject IAsync async;

    @Inject IWebExcept webExcept;
    @Inject IExcept except;

    @Inject ISizedSemaphore sizedSemaphore;

    private final Class<META> metaClass;
    private final Class<FINISH> finishClass;

    protected abstract FINISH processFinishAndReturnMeta(PupMFullUploadInfo<META, FINISH> uploaded);

    @Override
    public PupMUploadStatus<META, FINISH> uploadPart(String _userToken, PupMUploadMeta<META> partMeta, byte[] content) {
        final PupUploadPartId partId = partMeta.getPartId();
        final PupMUploadId uploadId = partId.getId();
        final ZonedDateTime now = times.nowZ();
        validateUploadOnStart(_userToken, partMeta, content);

        X1<Boolean> contentUploaded = X.x(false);
        final OneOf<O<PupStorageMeta<META, FINISH>>, Exception> saveResult =
                metaStore.updateObject(metaKey(uploadId), getMetaDataType(), meta -> {
                    try {
                        validateUploadWithMeta(meta, partMeta, content);
                        if (!contentUploaded.get()) {
                            repeat.repeatE(() -> byteStorage.upload(partId, content), 5, 1000L);
                        }
                        return O.of(meta.processPart(partMeta, now));
                    } catch (IOException e) {
                        return webExcept.throwBySubstatus(503, PupExc.PUP_CANT_UPLOAD, "");
                    } catch (JskProblemException e) {
                        return O.of(meta.exception(getExceptionCode(e.getProblem().getCode()), Ex.getInfo(e), partMeta, now));
                    } catch (Exception e) {
                        return O.of(meta.exception(PupExc.PUP_OTHER, Ex.getInfo(e), partMeta, now));
                    }
                });
        return saveResult.collect(O::get, Ex::thRow).getStatus();
    }

    @Override
    public PupMUploadStatus<META, FINISH> getUploadStatus(String _userToken, PupMUploadId id) {
        X1<PupMUploadStatus<META, FINISH>> toRet = new X1<>();
        metaStore.updateObject(metaKey(id), getMetaDataType(), meta -> {
            toRet.setI1(meta.getStatus());
            if (meta.getStatus().getStatus() == PupEUploadStatus.PRE_FINISHED) {
                return privatePrepareAndProcessFullInfo(oneOf -> {
                    final OneOf<FINISH, Exception> okOrError = oneOf.mapLeft(this::processFinishAndReturnMeta);
                    if (okOrError.isLeft()) {
                        meta.getStatus().setUploadFinish(O.of(times.nowZ()));
                        meta.getStatus().setUploadFinishMeta(okOrError.oLeft());
                        return O.of(meta);
                    } else {
                        return O.empty();
                    }
                }, meta);
            } else {
                return O.empty();
            }
        });
        return toRet.get();
    }

    private O<PupStorageMeta<META, FINISH>> privatePrepareAndProcessFullInfo(
            F1<OneOf<PupMFullUploadInfo<META, FINISH>, Exception>, O<PupStorageMeta<META, FINISH>>> processor,
            PupStorageMeta<META, FINISH> meta) {
        return sizedSemaphore.acquireLockAndReturn(meta.getStatus().getMaxSize() * 2, () -> {
            try {
                final List<X2<PupMUploadMeta<META>, byte[]>> data =
                        async.coldTaskFJPGet(() -> meta.getParts().entrySet().parallelStream()
                                .sorted(Map.Entry.comparingByKey())
                                .map($ -> X.x($.getValue(),
                                        repeat.repeat(() -> byteStorage.getBytes($.getValue().getPartId()).get(), 5, 1000L)))
                                .collect(Cc.toL()));
                return processor.apply(OneOf.left(new PupMFullUploadInfo<>(
                        meta.getStatus().getId(),
                        meta.getStatus(),
                        data
                )));
            } catch (Exception e) {
                return processor.apply(OneOf.right(e));
            }
        });
    }

    private boolean validateUploadOnStart(String userToken, PupMUploadMeta<META> partMeta, byte[] content) {
        if (!userProvider.isUploadAllowed(userToken, partMeta, content)) {
            return except.throwByCode(PupExc.PUP_UNAUTHORIZED);
        }
        if (config.getMaxPartCount() < partMeta.getMaxParts()) {
            return except.throwByCode(PupExc.PUP_PARTS_GT_THAN_ALLOWED);
        }
        if (config.getMaxOverallSize() < partMeta.getOverallSize()) {
            return except.throwByCode(PupExc.PUP_SIZE_GT_THAN_ALLOWED);
        }
        if (partMeta.getCurrentSize() != content.length) {
            return except.throwByCode(PupExc.PUP_SIZE_IS_NOT_AS_IN_META);
        }
        return true;
    }

    private boolean validateUploadWithMeta(PupStorageMeta<META, FINISH> meta, PupMUploadMeta<META> partMeta, byte[] content) {
        if (meta.getStatus().getStatus() == PupEUploadStatus.FAILED) {
            return except.throwByCode(PupExc.PUP_UPLOAD_ALREADY_FAILED);
        }
        if (meta.getStatus().getStatus() == PupEUploadStatus.FINISHED) {
            return except.throwByCode(PupExc.PUP_UPLOAD_ALREADY_FINISHED);
        }
        if (meta.getStatus().getUploadedSize() + partMeta.getCurrentSize() < meta.getStatus().getMaxSize()) {
            return except.throwByCode(PupExc.PUP_SIZE_GT_THAN_STATED);
        }
        if (meta.getStatus().getMaxSize() != partMeta.getOverallSize()) {
            return except.throwByCode(PupExc.PUP_WRONG_SIZE);
        }
        if (meta.getStatus().getMaxParts() != partMeta.getMaxParts()) {
            return except.throwByCode(PupExc.PUP_WRONG_PART_COUNT);
        }
        if (meta.getParts().containsKey(partMeta.getPartId().getCurrentPart())) {
            return except.throwByCode(PupExc.PUP_PART_ALREADY_PROCESSED);
        }
        return true;
    }

    private KvKeyWithDefault metaKey(PupMUploadId id) {
        return new KvSimpleKeyWithName("UPDATER-" + getId() + "_" + id, "{}");
    }

    private TypeWrap<PupStorageMeta<META, FINISH>> getMetaDataType() {
        return TypeWrap.getCustom(PupStorageMeta.class, metaClass, finishClass);
    }

    private PupExc getExceptionCode(String code) {
        return Re.findInEnum(PupExc.class, code).orElse(PupExc.PUP_OTHER);
    }
}
