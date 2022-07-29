package sk.aws.s3.comparetool;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import sk.aws.s3.S3ItemMeta;
import sk.aws.s3.S3JskClient;
import sk.aws.s3.comparetool.model.S3CompareInput;
import sk.aws.s3.comparetool.model.S3CompareMeta;
import sk.services.async.IAsync;
import sk.services.async.ISizedSemaphore;
import sk.services.comparer.CompareTool;
import sk.services.comparer.model.CompareResult;
import sk.services.json.IJson;
import sk.services.retry.IRepeat;
import sk.services.time.ITime;
import sk.utils.async.AtomicNotifier;
import sk.utils.files.PathWithBase;
import sk.utils.functional.O;
import sk.utils.functional.R;
import sk.utils.statics.*;
import sk.utils.tuples.X1;
import sk.utils.tuples.X2;

import javax.inject.Inject;
import java.util.List;

import static sk.utils.statics.Cc.join;
import static sk.utils.statics.Cc.stream;

@Log4j2
@NoArgsConstructor
@AllArgsConstructor
public class S3SyncTool extends CompareTool<S3ItemMeta, S3CompareMeta> {
    @Inject S3CompareTool compTool;
    @Inject ITime times;
    @Inject IJson json;
    @Inject IAsync async;
    @Inject IRepeat repeat;
    @Inject ISizedSemaphore sizeLock;

    public CompareResult<S3ItemMeta, S3CompareMeta> sync(
            int tryTimes,
            S3CompareInput i1, S3CompareInput i2, boolean filesMustBePublic) {
        CompareResult<S3ItemMeta, S3CompareMeta> result = this.compTool.compare(i1, i2, filesMustBePublic);
        X1<SyncVariant> sv = new X1<>();

        while (result.hasDifferences() && --tryTimes >= 0) {
            log.error(result.getShortInfo());
            final String path =
                    "/tmp/compare_tool/" + (i1.getShortDescription() + "_VS_" + i2.getShortDescription()).replace("/", "_")
                            + "__" + Ti.yyyyMMddHHmmss.format(times.nowZ()) + ".json";
            final String fullDifInfo = json.to(result, true);
            Io.reWrite(path, w -> w.append(fullDifInfo));
            log.error("You can find dif in:" + path);
            log.error("\nYou have options:\n" + St.addTabsLeft(SyncVariant.getVariants(), 1));
            log.error("\nType 'exit' to exit");

            if (sv.get() == null) {
                Io.endlessReadFromKeyboard(command -> {
                    if (Fu.equalIgnoreCase("exit", command.trim())) {
                        return false;
                    }
                    try {
                        sv.setI1(SyncVariant.valueOf(command));
                        return false;
                    } catch (IllegalArgumentException e) {
                        log.error(command + " is an unknown variant\n Possible options: " + join(stream(SyncVariant.values())));
                        return true;
                    }
                });
            }

            CompareResult<S3ItemMeta, S3CompareMeta> finalResult = result;
            async.coldTaskFJPRun(() -> trySync(i1, i2, finalResult, sv.get()));

            result = this.compTool.compare(i1, i2, filesMustBePublic);
        }


        return result;
    }

    private void trySync(
            S3CompareInput first, S3CompareInput second,
            CompareResult<S3ItemMeta, S3CompareMeta> result,
            SyncVariant syncVariant) {
        final S3JskClient clientFirst = compTool.createClient(first);
        final S3JskClient clientSecond = compTool.createClient(second);
        final AtomicNotifier an = new AtomicNotifier(getCopyCount(result, syncVariant), 100, log::info);
        Cc.<R>l(
                () -> {
                    if (syncVariant.copyToFirst) {
                        runTasks(result.getIn2NotIn1().getNotExistingInOther(),
                                clientSecond, clientFirst, second.getRoot(), first.getRoot(), an);
                    }
                },
                () -> {
                    if (syncVariant.copyToSecond) {
                        runTasks(result.getIn1NotIn2().getNotExistingInOther(),
                                clientFirst, clientSecond, first.getRoot(), second.getRoot(), an);
                    }
                },
                () -> {
                    if (syncVariant.firstPriority) {
                        runTasks(result.getExistButDifferent().stream().map(X2::i1).collect(Cc.toL()),
                                clientFirst, clientSecond, first.getRoot(), second.getRoot(), an);
                    } else {
                        runTasks(result.getExistButDifferent().stream().map(X2::i2).collect(Cc.toL()),
                                clientSecond, clientFirst, second.getRoot(), first.getRoot(), an);
                    }
                }
        ).parallelStream().forEach(R::run);


    }

    private void runTasks(
            List<S3ItemMeta> toCopy,
            S3JskClient clientFrom, S3JskClient clientTo,
            PathWithBase rootFrom, PathWithBase rootTo,
            AtomicNotifier an
    ) {
        toCopy = toCopy.stream().filter($ -> !$.isFailed()).collect(Cc.toL());
        if (toCopy.size() > 0) {
            toCopy.parallelStream().forEach(item -> {
                try {
                    this.repeat.repeat(() -> {
                        final String path = item.getPath();
                        final O<S3ItemMeta> ometa = clientFrom.getMeta(rootFrom, path, false);
                        if (ometa.isPresent()) {
                            final S3ItemMeta meta = ometa.get();
                            sizeLock.acquireLockAndRun(meta.getSize(), () -> {
                                clientFrom.getFromS3(rootFrom.replacePath(path))
                                        .ifPresentOrElse(bytes -> {
                                            clientTo.putPublic(rootTo.replacePath(path), bytes);
                                        }, () -> {throw new RuntimeException("");});
                            });
                        } else {
                            throw new RuntimeException("Problem with " + rootFrom + ":" + path);
                        }
                    }, 50, 2000);

                    an.incrementAndNotify();
                } catch (Exception e) {
                    log.error("", e);
                }
            });
        }
    }

    private int getCopyCount(CompareResult<S3ItemMeta, S3CompareMeta> result, SyncVariant syncVariant) {
        return result.getExistButDifferent().size() +
                (syncVariant.isCopyToFirst() ? result.getIn2NotIn1().getNotExistingInOther().size() : 0) +
                (syncVariant.isCopyToSecond() ? result.getIn1NotIn2().getNotExistingInOther().size() : 0);
    }

    @AllArgsConstructor
    @Getter
    enum SyncVariant {
        FIRST2SECOND_COPY(
                "Copies data from first to second. If conflict between files - files from first have priority",
                false, true, true
        ),
        SECOND2FIRST_COPY(
                "Copies data from second to first. If conflict between files - files from second have priority",
                true, false, false
        ),
        FIRST2SECOND_SYNC(
                "Copies data in both ways. If conflict between files - files from first have priority",
                true, true, true
        ),
        SECOND2FIRST_SYNC(
                "Copies data in both ways. If conflict between files - files from second have priority",
                true, true, false
        );

        String info;
        boolean copyToFirst;
        boolean copyToSecond;
        boolean firstPriority;

        public static String getVariants() {
            return join("\n", stream(values()), $ -> "'" + $.name() + "'" + " " + $.getInfo());
        }
    }
}
