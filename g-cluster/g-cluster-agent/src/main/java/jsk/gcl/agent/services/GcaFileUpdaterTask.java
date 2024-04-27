package jsk.gcl.agent.services;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import jsk.gcl.agent.GcaAgentMain;
import jsk.gcl.agent.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sk.aws.s3.S3JskClient;
import sk.services.ICoreServices;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.files.PathWithBase;
import sk.utils.functional.O;
import sk.utils.statics.*;

import java.io.OutputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static sk.utils.functional.O.empty;

@Slf4j
public class GcaFileUpdaterTask {
    private final S3JskClient s3;
    private final ICoreServices core;
    private final GcaUpdateFileProps conf;
    private final GcaFileStorage storage;

    private final O<ForeverThreadWithFinish> ftf;

    public GcaFileUpdaterTask(S3JskClient s3, ICoreServices core, GcaUpdateFileProps conf, boolean startBgThread) {
        this.s3 = s3;
        this.core = core;
        this.conf = conf;
        this.storage = new GcaFileStorage(conf, core.json(), core.bytes());
        runOnce();
        this.ftf = startBgThread ? O.of(createThreadAndStart()) : O.empty();
    }

    private void runOnce() {
        O<GcaMetaItem> betterVersion = isCloudVersionHigherThanOurWhichIsNotBadOnOurSide();
        if (betterVersion.isPresent()) {
            log.debug("'%s' has better version: '%s'".formatted(conf.localFilePath(), betterVersion.get().pathInBucket()));
            boolean permitToUpdate = true;
            boolean isRollingUpdate = conf.getRollingUpdate().isEnabled();
            if (isRollingUpdate) {
                log.debug("Trying rolling update...");
                boolean canLock = canILockInRollingUpdate();
                log.debug(canLock ? ":) Could be locked..." : ":( Lock is already obtained...");
                permitToUpdate = canLock && tryLock();
            }
            if (permitToUpdate) {
                log.debug("Updated permitted and started!..");
                doUpdate(betterVersion.get());
                if (isRollingUpdate) {
                    log.debug("Download finished, but rolling update and need to wait for service to start and unlock...");
                    waitUntilRollingUpdateFinishedAndUnLock();
                }
            }
        } else {
            log.trace("'%s' has no new version".formatted(conf.localFilePath()));
        }
        restartServiceIfNeeded(false);
    }


    private O<GcaMetaItem> isCloudVersionHigherThanOurWhichIsNotBadOnOurSide() {
        try {
            final GcaMeta meta = s3.getObjectFromS3(conf.getPathToMetaFile(), GcaMeta.class).get();
            GcaMetaItem actualFileInCloud = meta.currentFile();
            GcaVersionId actualFileInCloudVersionId = actualFileInCloud.getVersionId();
            log.trace("Actual version in cloud: '%s'".formatted(actualFileInCloudVersionId));
            GcaFileStorage.VersionStatus versionStatus = storage.getVersionStatus(actualFileInCloudVersionId);
            return switch (versionStatus) {
                case GOOD, UNKNOWN, BAD -> empty();
                case NEW -> O.of(actualFileInCloud);
            };
        } catch (Exception e) {
            log.error("", e);
            return empty();
        }
    }

    private boolean canILockInRollingUpdate() {
        if (conf.getRollingUpdate().isEnabled()) {
            //try lock to update - either file is not exist, or it's an old one, then lock
            O<GcaLockCls> lockFromS3 = s3.getObjectFromS3(conf.getS3LockPathFile(), GcaLockCls.class)
                    .filter($ -> $.getLockDate()
                            .plus(2 * conf.getRollingUpdate().getOkAfterMs(), ChronoUnit.MILLIS)
                            .isAfter(core.times().nowZ()));
            log.debug(lockFromS3.map($ -> "Someone already locked: %s".formatted($.toString())).orElse("No one locked!.."));
            return lockFromS3.map($ -> false).orElse(true);
        } else {
            return false;
        }
    }

    private boolean tryLock() {
        s3.putPublicNoUrl(conf.getS3LockPathFile(),
                core.json().to(new GcaLockCls(GcaAgentMain.nodeId, core.times().nowZ())).getBytes(St.UTF8),
                false, empty(), empty(), Cc.m("Cache-Control", "no-store"));
        Ti.sleep(5 * Ti.second);
        log.debug("Lock file put, waiting 5 seconds...");
        if (Fu.equal( //if lock is successfull
                s3.getObjectFromS3(conf.getS3LockPathFile(), GcaLockCls.class).map($ -> $.nodeId).orElse(null),
                GcaAgentMain.nodeId
        )) {
            log.debug("Lock SUCCESS!");
            return true;
        } else {
            log.debug("Lock FAILED!");
            return false;
        }
    }

    private void doUpdate(GcaMetaItem gcaMetaItem) {
        log.debug("Downloading new file...");
        try (OutputStream outputStream = storage.setNewVersionAndSetItAsCurrent(gcaMetaItem.getVersionId())) {
            s3.downloadToStream(new PathWithBase(gcaMetaItem.bucket(), gcaMetaItem.pathInBucket()), outputStream);
            log.debug("New file downloaded from '%s'".formatted(gcaMetaItem.pathInBucket()));
        } catch (Exception e) {
            log.error("", e);
        } finally {
            restartServiceIfNeeded(true);
        }
    }

    private ForeverThreadWithFinish createThreadAndStart() {
        ForeverThreadWithFinish ftf = new ForeverThreadWithFinish(() -> {
            long sleepTime = this.conf.recheckDuration().orElse(Duration.ofSeconds(10)).toMillis();
            log.trace("Sleeping: " + sleepTime);
            Ti.sleep(sleepTime);
            runOnce();
        }, conf.getInternalFileName() + "__updater", true, (e, f) -> log.error("", e));
        ftf.start();
        return ftf;
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
                    log.debug("Trying to start service..." + service);
                    final boolean started = Io.serviceStart(service);
                    if (!started) {
                        log.debug("Trying to REstart service..." + service);
                        final boolean restarted = Io.serviceRestart(service);
                        if (!restarted) {
                            log.error("Service restart failed!" + service);
                            return;
                        }
                    }
                    log.debug("Service restarted..." + service);
                }
            }
        });
    }

    private void waitUntilRollingUpdateFinishedAndUnLock() {
        try {
            final long maxTime = conf.getRollingUpdate().getOkAfterMs();
            final long oneRoundSleep = 5 * Ti.second;
            long curTime = 0;
            log.debug("Waiting for service to respond on %s".formatted(conf.getRollingUpdate().getUrl()));
            while (curTime < maxTime) {
                log.debug("Sleeping %d".formatted(oneRoundSleep));
                Ti.sleep(oneRoundSleep);
                curTime += oneRoundSleep;
                var coreHttpResponse = core.http()
                        .get(conf.getRollingUpdate().getUrl())
                        .timeout(3000)
                        .tryCount(5)
                        .trySleepMs(1000)
                        .goResponse();
                if (coreHttpResponse.isLeft() && coreHttpResponse.left().is2XX()) {
                    log.debug("Service is up successfully!");
                    storage.setCurrentVersionIsOk();
                    return;
                }
                log.debug("%s not yet responded, overall time: %d/%d".formatted(conf.getRollingUpdate().getUrl(), curTime,
                        maxTime));
            }
            throw new RuntimeException("Waited more than allowed %d/%d and %s did not respond".formatted(curTime, maxTime,
                    conf.getRollingUpdate().getUrl()));
        } catch (Exception e) {
            log.error("Version seems bad", e);
            try {
                storage.setCurrentVersionIsBadSwitchToLastGood();
                restartServiceIfNeeded(true);
            } catch (Exception ex) {
                try {
                    storage.setCurrentVersionIsBadSwitchToLastGood();
                    restartServiceIfNeeded(true);
                } catch (Exception exc) {
                    log.error("", exc);
                }
            }
        } finally {
            log.debug("Deleting lock %s...".formatted(conf.getS3LockPathFile()));
            s3.deleteOne(conf.getS3LockPathFile());
            log.debug("Lock deleted!");
        }
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class GcaLockCls {
        GcaNodeId nodeId;
        ZonedDateTime lockDate;

        @Override
        public String toString() {
            return "%s %s".formatted(nodeId, Ti.yyyyMMddHHmmssSSS.format(lockDate));
        }
    }
}
