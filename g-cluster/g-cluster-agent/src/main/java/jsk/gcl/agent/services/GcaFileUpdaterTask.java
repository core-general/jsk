package jsk.gcl.agent.services;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import jsk.gcl.agent.model.GcaMeta;
import jsk.gcl.agent.model.GcaUpdateFileProps;
import lombok.extern.slf4j.Slf4j;
import sk.aws.s3.S3JskClient;
import sk.services.ICoreServices;
import sk.services.http.CrcAndSize;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.files.PathWithBase;
import sk.utils.functional.O;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;
import sk.utils.statics.St;
import sk.utils.statics.Ti;

import java.time.Duration;

@Slf4j
public class GcaFileUpdaterTask {
    private final S3JskClient s3;
    private final ICoreServices core;
    private final GcaUpdateFileProps conf;

    private final O<ForeverThreadWithFinish> ftf;

    private volatile O<CrcAndSize> currentFileMeta = O.empty();

    public GcaFileUpdaterTask(S3JskClient s3, ICoreServices core, GcaUpdateFileProps conf, boolean startBgThread) {
        this.s3 = s3;
        this.core = core;
        this.conf = conf;
        executePayload();
        if (startBgThread) {
            ForeverThreadWithFinish ftf = new ForeverThreadWithFinish(() -> {
                try {
                    executePayload();
                } catch (Throwable e) {
                    log.error("", e);
                } finally {
                    Ti.sleep(this.conf.recheckDuration().orElse(Duration.ofSeconds(10)).toMillis());
                }
            }, true);
            ftf.start();
            this.ftf = O.of(ftf);
        } else {
            ftf = O.empty();
        }
    }

    /**
     * 0. Prepare local meta if file exist
     * 1. Get meta file
     * 2. If meta has file info that changed then we need to update
     * 3. To update - download new file, put it in the right place
     * 4. Restart service
     */
    private void executePayload() {
        prepareCurrentFileMetaIfExist();

        final GcaMeta meta = core.json()
                .from(new String(getFileFromS3(conf.metaFileBucket(), conf.metaFilePathInBucket()), St.UTF8), GcaMeta.class);
        if (!Fu.equal(meta.currentFile().crcAndSize(), currentFileMeta.orElse(null))) {
            update(meta);
        }
        restartServiceIfNeeded(false);
    }

    private void update(GcaMeta meta) {
        //synchronizing so that other threads do not break other's files during service restart
        synchronized (GcaFileUpdaterTask.class) {
            log.info("\n\nUpdating file: \n%s\n\n%s\n".formatted(core.json().to(meta.currentFile(), true),
                    core.json().to(conf, true)));
            final byte[] data = getFileFromS3(meta.currentFile().bucket(), meta.currentFile().pathInBucket());
            Io.reWriteBin(conf.localFilePath(), w -> w.append(data));
            updateCurrentFileMeta(data);
            log.info("File updated: restarting service \"" + conf.serviceToReboot().orElse("NONE") + "\"...\n\n");
            restartServiceIfNeeded(true);
        }
    }

    private void prepareCurrentFileMetaIfExist() {
        if (currentFileMeta.isEmpty()) {
            final O<byte[]> data = Io.bRead(conf.localFilePath()).oBytes();
            if (data.isPresent()) {
                updateCurrentFileMeta(data.get());
            }
        }
    }

    private void updateCurrentFileMeta(byte[] bytes) {
        currentFileMeta = O.of(core.bytes().calcCrcAndSize(bytes));
    }

    private void restartServiceIfNeeded(boolean forceRestart) {
        conf.serviceToReboot().ifPresent(service -> {
            if (forceRestart) {
                log.info("Trying to REstart service..." + service);
                final boolean restarted = Io.serviceRestart(service);
                if (!restarted) {
                    log.info("Trying to start service..." + service);
                    final boolean started = Io.serviceStart(service);
                    if (!started) {
                        log.error("Service restart failed!" + service);
                        return;
                    }
                }
                log.info("Service restarted..." + service);
            } else {
                if (Io.serviceStatus(service) != Io.ServiceStatus.ACTIVE) {
                    log.info("Trying to start service..." + service);
                    final boolean started = Io.serviceStart(service);
                    if (!started) {
                        log.info("Trying to REstart service..." + service);
                        final boolean restarted = Io.serviceRestart(service);
                        if (!restarted) {
                            log.error("Service restart failed!" + service);
                            return;
                        }
                    }
                    log.info("Service restarted..." + service);
                }
            }

        });
    }

    private byte[] getFileFromS3(String bucket, String pathInBucket) {
        return s3.getFromS3(new PathWithBase(bucket, pathInBucket)).get();
    }
}
