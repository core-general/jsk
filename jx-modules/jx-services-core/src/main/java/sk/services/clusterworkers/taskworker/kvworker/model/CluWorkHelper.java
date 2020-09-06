package sk.services.clusterworkers.taskworker.kvworker.model;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import lombok.extern.log4j.Log4j2;
import sk.services.clusterworkers.taskworker.kvworker.model.backoff.CluWorkBackoffStrategy;
import sk.services.clusterworkers.taskworker.model.CluWorkMetaInfo;
import sk.services.kv.KvAllValues;
import sk.utils.functional.Converter;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.ifaces.Identifiable;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Fu;
import sk.utils.statics.St;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class CluWorkHelper<M, T extends Identifiable<String>, R> {
    private String taskId;
    CluWorkMetaInfo<M> meta;
    CluWorkFullInfo<T, R> raw;
    private Converter<O<byte[]>, CluWorkFullInfo<T, R>> converter;
    private ZonedDateTime now;

    public CluWorkHelper(String taskId, KvAllValues<CluWorkMetaInfo<M>> data,
            Converter<O<byte[]>, CluWorkFullInfo<T, R>> converter,
            ZonedDateTime now, long maxLockTime) {
        this.taskId = taskId;
        this.meta = data.getValue();
        this.converter = converter;
        this.now = now;
        this.raw = converter.convertThere(data.getRawValue());

        //clear bad locks
        Iterator<Map.Entry<String, CluWorkFullInfo.WorkPartInfo<T, R>>> lockIterator =
                raw.getWorkLocked().entrySet().iterator();
        while (lockIterator.hasNext()) {
            Map.Entry<String, CluWorkFullInfo.WorkPartInfo<T, R>> kv = lockIterator.next();
            if (kv.getValue().getLock().isPresent()) {
                if (kv.getValue().getLock().get().isObsolete(now, maxLockTime)) {
                    log.error("Some CluWork lock was old");
                    kv.getValue().setLock(O.empty());
                    kv.getValue().setTryCount(kv.getValue().getTryCount() + 1);
                    raw.getWorkActive().addFirst(kv.getValue());
                    lockIterator.remove();
                }
            } else {
                log.error("Some problem with CluWorkHelper locks");
                raw.getWorkActive().addFirst(kv.getValue());
                lockIterator.remove();
            }
        }


        //check retry allowance
        final Iterator<Map.Entry<Long, List<CluWorkFullInfo.WorkPartInfo<T, R>>>>
                backoffIterator = raw.getIdleTasks().entrySet().iterator();
        while (backoffIterator.hasNext()) {
            final Map.Entry<Long, List<CluWorkFullInfo.WorkPartInfo<T, R>>> retryValue = backoffIterator.next();
            if (now.toInstant().toEpochMilli() > retryValue.getKey()) {
                retryValue.getValue().forEach($ -> raw.getWorkActive().addFirst($));
                backoffIterator.remove();
            } else {
                break;
            }
        }
    }

    public void initWork(String newRunId, O<M> additionalMeta, List<T> toDo) {
        meta.restart(newRunId, now, toDo.size(), additionalMeta);

        raw.setWorkActive(toDo.stream().map($ -> new CluWorkFullInfo.WorkPartInfo<T, R>($))
                .collect(Collectors.toCollection(ArrayDeque::new)));

        raw.setWorkFail(Cc.l());
        raw.setWorkLocked(Cc.m());
        raw.setWorkSuccess(Cc.l());
    }

    public boolean noMoreActiveWork() {
        return !meta.isWorkActive() || meta.getReadyToBeProcessedChunkCountWithlLocks() == 0;
    }

    public boolean isWorkActive() {
        return meta.isWorkActive();
    }

    public KvAllValues<CluWorkMetaInfo<M>> serializeBack() {
        meta.setLockedChunkCount(raw.getWorkLocked().size());
        meta.setIdleChunkCount(raw.getIdleTasks().values().stream().mapToInt($ -> $.size()).sum());
        meta.setLastBackoffIdleWait(O.ofNull(raw.getIdleTasks().lastEntry()).map($ -> $.getKey()));
        meta.setFinalDoneChunkCount(raw.getWorkSuccess().size());
        meta.setFinalFailChunkCount(raw.getWorkFail().size());
        meta.setLastMetaCountUpdate(O.of(now));

        return new KvAllValues<>(meta, converter.convertBack(raw), O.empty());
    }

    public List<CluWorkFullInfo.WorkPartInfo<T, R>> getActualWork(int numberOfTasksInSplit) {
        List<CluWorkFullInfo.WorkPartInfo<T, R>> workParts =
                Cc.fillFun(numberOfTasksInSplit, i -> raw.getWorkActive().pollLast());

        workParts.stream().filter(Objects::nonNull).forEach($ -> {
            $.setLock(O.of(new CluWorkFullInfo.LockInfo(taskId, now)));
            $.setTryCount($.getTryCount() + 1);
            raw.getWorkLocked().put($.getWorkDescription().getId(), $);
        });

        return workParts;
    }

    public void finishProcessing(List<CluWorkChunkResult<R>> finishedWork, int maxRetryCount,
            CluWorkBackoffStrategy<T, R> backoff) {
        if (finishedWork.size() == 0) {
            return;
        }
        ListIterator<CluWorkChunkResult<R>> li = finishedWork.listIterator();
        while (li.hasNext()) {
            CluWorkChunkResult<R> workResult = li.next();
            CluWorkFullInfo.WorkPartInfo<T, R> lockedWorkPart = raw.getWorkLocked().get(workResult.getId());
            if (lockedWorkPart == null) {
                continue;
            }
            if (!(lockedWorkPart.getLock().isPresent() && Fu.equal(lockedWorkPart.getLock().get().getLockId(), taskId))) {
                continue;
            }

            lockedWorkPart.setLock(O.empty());
            raw.getWorkLocked().remove(workResult.getId());
            OneOf<R, Exception> result = workResult.getResult();
            if (result.isLeft()) {
                lockedWorkPart.setLastResult(O.of(result.left()));
                lockedWorkPart.setLastError(O.empty());
                raw.getWorkSuccess().add(lockedWorkPart);
            } else {
                lockedWorkPart.setLastError(O.of(Ex.getInfo(result.right(), 1000)));
                meta.setCurrentFailCount(meta.getCurrentFailCount() + 1);
                meta.getRetryCounts().add(lockedWorkPart.getTryCount(), lockedWorkPart.getWorkDescription().getId());
                if (lockedWorkPart.getTryCount() < maxRetryCount) {
                    Duration waitUntilRestart = backoff.getWaitDurationForTask(lockedWorkPart);
                    if (!waitUntilRestart.isZero()) {
                        Cc.computeAndApply(raw.getIdleTasks(), now.plus(waitUntilRestart).toInstant().toEpochMilli(),
                                (k, oldV) -> Cc.add(oldV, lockedWorkPart), Cc::l);
                    } else {
                        raw.getWorkActive().addFirst(lockedWorkPart);
                    }
                } else {
                    raw.getWorkFail().add(lockedWorkPart);
                }
            }
        }

        //If all chunks either are finished or failed
        if (raw.getWorkActive().size() == 0 && raw.getWorkLocked().size() == 0 && raw.getIdleTasks().size() == 0) {
            meta.setFinishedAt(O.of(now));
            meta.setStatus(raw.getWorkFail().size() > 0 ? CluWorkMetaInfo.Status.FAILED : CluWorkMetaInfo.Status.FINISHED);
            if (meta.getStatus() == CluWorkMetaInfo.Status.FAILED) {
                meta.setFailMessage(O.of("Failed:" + raw.getWorkFail().size() + " \nExamples:\n" +
                        St.raze3dots(raw.getWorkFail().stream().map($ -> $.getLastError().orElse("")).collect(
                                Collectors.joining("\n")), 5000)));
            }
        }
    }
}
