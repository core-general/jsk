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
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import sk.services.async.ISizedSemaphore;
import sk.services.async.ISizedSemaphoreImpl;
import sk.services.kv.IKvLocal4Test;
import sk.services.kv.IKvUnlimitedStore;
import sk.services.profile.IAppProfileType;
import sk.services.retry.IRepeat;
import sk.services.retry.RepeatImpl;
import sk.utils.functional.F0;
import sk.utils.functional.F0E;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.web.exceptions.IWebExcept;
import sk.web.renders.inst.WebJsonRender;
import sk.web.tools.partuploader.api.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sk.services.CoreServicesRaw.services;
import static sk.utils.asserts.JskAssert.checkCatchOrFail;

public class PupApiImplTest {
    IRepeat repeat = new RepeatImpl(services().async()) {
        @Override
        public <T> T repeatE(F0E<T> toRun, F0<T> onFail, int retryCount, long sleepBetweenTries,
                Set<Class<? extends Throwable>> okExceptions) throws Exception {
            return toRun.get();
        }
    };
    IKvUnlimitedStore metaStore = new IKvLocal4Test(services().json(), services().times());
    PupIByteStorage byteStorage = new PupIByteStorage() {
        Map<PupUploadPartId, byte[]> data = new HashMap<>();

        @Override
        public synchronized void upload(PupUploadPartId id, byte[] bytes) throws IOException {
            data.put(id, bytes);
            if (bytes.length == 111) {
                throw new IOException();
            }
        }

        @Override
        public synchronized O<byte[]> getBytes(PupUploadPartId id) {
            return O.ofNull(data.get(id));
        }
    };
    PupIUserProvider<UploadMeta> userProvider = new PupIUserProvider<UploadMeta>() {
        @Override
        public boolean isUploadAllowed(String _userToken, PupMUploadMeta<UploadMeta> partMeta, byte[] content) {
            return "1".equalsIgnoreCase(_userToken);
        }
    };
    PubIConfig config = new PubIConfig() {
        @Override
        public Integer getMaxPartCount() {
            return 10;
        }

        @Override
        public Long getMaxOverallSize() {
            return 1000L;
        }
    };

    IWebExcept webExcept = () -> new WebJsonRender(services().json(), () -> new IAppProfileType() {
        @Override
        public String name() {
            return "";
        }

        @Override
        public boolean isForDefaultTesting() {
            return false;
        }

        @Override
        public boolean isForProductionUsage() {
            return false;
        }
    });

    ISizedSemaphore semaphore = new ISizedSemaphoreImpl(1000, 10);

    PupApiImpl<UploadMeta, Finish> impl = new PupApiImpl<UploadMeta, Finish>(
            metaStore, byteStorage, userProvider, config, services().times(), repeat, services().async(),
            webExcept, services().except(), semaphore,
            UploadMeta.class, Finish.class
    ) {
        @Override
        protected Finish processFinishAndReturnMeta(PupMFullUploadInfo<UploadMeta, Finish> uploaded) {
            final byte[] last = Cc.last(uploaded.getFinishedData()).get().i2;
            return new Finish(
                    Cc.first(uploaded.getFinishedData()).get().i2[0],
                    last[last.length - 1],
                    Cc.join("", uploaded.getFinishedData().stream().filter($ -> $.i1.getAdditionalData().isPresent())
                            .map($ -> $.i1.getAdditionalData().get().getMeta()))
            );
        }

        @Override
        public String getId() {
            return "test-uploader";
        }
    };


    PupPublicApi<UploadMeta, Finish> api = impl;


    @Test
    @SneakyThrows
    public void failScenarios() {
        final String user = "1";
        final String user2 = "2";
        final PupMUploadId otherUploadId = new PupMUploadId(UUID.randomUUID());

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            checkCatchOrFail(() -> api.getUploadStatus(user, uploadId).getId(), "", PupExc.PUP_NO_UPLOAD);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 0), 6, 4, 24, O.of(new UploadMeta("B"))),
                    new byte[]{0, 1, 2, 3, 4, 5});
            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 6, 3, 24, O.of(new UploadMeta("B"))),
                    new byte[]{0, 1, 2, 3, 4, 5}), "", PupExc.PUP_WRONG_PART_COUNT);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 0), 6, 4, 24, O.of(new UploadMeta("B"))),
                    new byte[]{0, 1, 2, 3, 4, 5});
            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 6, 4, 23, O.of(new UploadMeta("B"))),
                    new byte[]{0, 1, 2, 3, 4, 5}), "", PupExc.PUP_WRONG_SIZE);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 13, 2, 23, O.of(new UploadMeta("B"))),
                    new byte[11]), "", PupExc.PUP_SIZE_IS_NOT_AS_IN_META);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 0), 12, 2, 24, O.of(new UploadMeta("B"))),
                    new byte[12]);
            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 13, 2, 23, O.of(new UploadMeta("B"))),
                    new byte[13]), "", PupExc.PUP_SIZE_GT_THAN_STATED);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 0), 12, 2, 24, O.of(new UploadMeta("B"))),
                    new byte[12]);
            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 13, 2, 23, O.of(new UploadMeta("B"))),
                    new byte[13]), "", PupExc.PUP_SIZE_GT_THAN_STATED);
            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 13, 2, 23, O.of(new UploadMeta("B"))),
                    new byte[13]), "", PupExc.PUP_UPLOAD_ALREADY_FAILED);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            checkCatchOrFail(() -> api.uploadPart(user2,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 13, 2, 23, O.of(new UploadMeta("B"))),
                    new byte[13]), "", PupExc.PUP_UNAUTHORIZED);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 13, 10000, 23, O.of(new UploadMeta("B"))),
                    new byte[13]), "", PupExc.PUP_PARTS_GT_THAN_ALLOWED);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 13, 1, 14, O.of(new UploadMeta("B"))),
                    new byte[13]), "", PupExc.PUP_FINISH_SIZE_WRONG);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 13, 2, 10000, O.of(new UploadMeta("B"))),
                    new byte[13]), "", PupExc.PUP_SIZE_GT_THAN_ALLOWED);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 0), 12, 2, 24, O.of(new UploadMeta("B"))),
                    new byte[12]);
            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(new PupUploadPartId(uploadId, 0), 12, 2, 24, O.of(new UploadMeta("B"))),
                    new byte[12]), "", PupExc.PUP_PART_ALREADY_PROCESSED);
        }

        {
            final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());
            final PupUploadPartId partId = new PupUploadPartId(uploadId, 0);

            checkCatchOrFail(() -> api.uploadPart(user,
                    new PupMUploadMeta<>(partId, 111, 2, 115, O.of(new UploadMeta("B"))),
                    new byte[111]), "", PupExc.PUP_CANT_UPLOAD);
        }
    }

    @Test
    public void fullScenario() {
        final String user = "1";
        final PupMUploadId uploadId = new PupMUploadId(UUID.randomUUID());

        final byte initial = 0b1010101;
        final byte finalB = 0b0101010;


        api.uploadPart(user,
                new PupMUploadMeta<>(new PupUploadPartId(uploadId, 2), 6, 4, 24, O.of(new UploadMeta("T"))),
                new byte[]{0, 1, 2, 3, 4, 5});


        api.uploadPart(user,
                new PupMUploadMeta<>(new PupUploadPartId(uploadId, 0), 6, 4, 24, O.of(new UploadMeta("B"))),
                new byte[]{initial, 1, 2, 3, 4, 5});

        assertEquals(api.getUploadStatus(user, uploadId).getStatus(), PupEUploadStatus.IN_PROCESS);

        api.uploadPart(user,
                new PupMUploadMeta<>(new PupUploadPartId(uploadId, 3), 6, 4, 24, O.of(new UploadMeta("A"))),
                new byte[]{0, 1, 2, 3, 4, finalB});


        api.uploadPart(user,
                new PupMUploadMeta<>(new PupUploadPartId(uploadId, 1), 6, 4, 24, O.of(new UploadMeta("E"))),
                new byte[]{0, 1, 2, 3, 4, 5});

        checkCatchOrFail(() -> api.uploadPart(user,
                new PupMUploadMeta<>(new PupUploadPartId(uploadId, 5), 6, 4, 24, O.of(new UploadMeta("B"))),
                new byte[6]), "", PupExc.PUP_UPLOAD_ALREADY_FINISHED);


        assertEquals(api.getUploadStatus(user, uploadId).getUploadFinishMeta().get(), new Finish(initial, finalB, "BETA"));
    }

    @Data
    @AllArgsConstructor
    static class UploadMeta {
        String meta;
    }

    @Data
    @AllArgsConstructor
    static class Finish {
        byte firstByte;
        byte lastByte;
        String finalMeta;
    }
}
