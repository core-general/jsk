package jsk.gcl.srv.scaling.schedulers;

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

import jsk.gcl.srv.GclAppProperties;
import jsk.gcl.srv.jpa.GclNode;
import jsk.gcl.srv.jpa.GclNodeId;
import jsk.gcl.srv.scaling.GclNodeArchiveManager;
import jsk.gcl.srv.scaling.GclOOMManager;
import jsk.gcl.srv.scaling.model.GclAvgStats;
import jsk.gcl.srv.scaling.model.GclNodeHistoryItem;
import jsk.gcl.srv.scaling.model.GclNodeInfo;
import jsk.gcl.srv.scaling.storage.GclNodeStorage;
import jsk.gcl.srv.scaling.workers.GclScalingLocalWorkerManager;
import lombok.extern.log4j.Log4j2;
import sk.services.boot.IBoot;
import sk.services.clusterworkers.kvonoff.CluKvBasedOnOffWorker;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.json.IJson;
import sk.services.nodeinfo.INodeInfo;
import sk.services.shutdown.INodeRestartStorage;
import sk.si.JxJavaProcessMemoryUsage;
import sk.si.JxProcessorLoadData;
import sk.si.JxSystemInfoService;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.collections.DequeWithLimit;
import sk.utils.functional.O;
import sk.utils.minmax.MinMaxAvg;
import sk.utils.statics.Ma;
import sk.utils.statics.Ti;
import sk.utils.tuples.X;
import sk.utils.tuples.X1;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 1. Gathers data about node,
 * 2. Informs that node is alife
 * 3. Decides how many workers the node should have (and also limits)
 */
@Log4j2
public class GclScalingDataGatherScheduler extends CluKvBasedOnOffWorker<CluKvBasedOnOffWorker.IConf>
        implements IBoot, GclOOMManager {
    public static final String CURRENT_MAX_WORKERS_FILE = "max_workers";
    @Inject JxSystemInfoService systemInfo;
    @Inject INodeInfo nodeInfo;
    @Inject INodeRestartStorage nodeRestartStorage;
    @Inject IJson json;
    @Inject GclAppProperties appProps;
    @Inject GclScalingLocalWorkerManager lwManager;
    @Inject GclNodeArchiveManager archiveManager;

    @Inject GclNodeStorage nodeStorage;

    public GclScalingDataGatherScheduler() {
        super("GclScalingDataGatherScheduler");
    }

    @Override
    public void run() {
        GclNodeInfo curInfo =
                nodeStorage.ensureNodeIsCreatedAndReturnConfig(new GclNodeId(nodeInfo.getNodeId()), () -> new GclNodeInfo(
                        times.nowZ(),
                        appProps.getMinWorkers(),
                        nodeRestartStorage.getStringAfterRestart(CURRENT_MAX_WORKERS_FILE)
                                .map($ -> Ma.pi($))
                                .orElseGet(() -> appProps.getMaxWorkers()),
                        appProps.getMaxWorkers(),
                        null,
                        null,
                        null,
                        null,
                        new DequeWithLimit<>(25),
                        new GclNodeInfo.OOMInfo(O.empty(), new DequeWithLimit<>(10))
                )).getNInnerState();


        lwManager.setDesiredLocalWorkerCount(
                (int) Ma.clamp(curInfo.getCurMaxWorkers() / 2, curInfo.getMinWorkers(), curInfo.getCurMaxWorkers()));

        start(new CluKvBasedOnOffWorker.Conf(
                30_000L, CluDelay.fixed(9_000L),
                cancel -> {
                    final GclNode oNodeInfo = gatherData(cancel);
                    scaleIfNeeded(oNodeInfo.getNInnerState());
                },
                e -> log.error("", e)
        ));
    }

    @PreDestroy
    private void preDestroy() {
        archiveManager.archiveNode(nodeInfo.getNodeId());
    }

    @Override
    public synchronized void onOOM(OutOfMemoryError error, Thread thread) {
        int currentWorkerCount = lwManager.getLocalWorkerCount();
        int newWorkerCount = currentWorkerCount / 2;

        try {
            nodeRestartStorage.setDataForRestart(CURRENT_MAX_WORKERS_FILE, newWorkerCount + "");

            nodeStorage.transactionWithSaveX1(() -> {
                GclNode node = nodeStorage.getNodeCurrentInfo(new GclNodeId(nodeInfo.getNodeId()));
                final GclNodeInfo old = node.getNInnerState();
                final GclNodeInfo.OOMInfo oom = old.getOom();
                final ZonedDateTime now = times.nowZ();
                oom.getFacts().addFirst(new GclNodeInfo.OOMFact(now, currentWorkerCount));
                oom.setLastMaxChange(O.of(now));
                old.setCurMaxWorkers(newWorkerCount);
                return X.x(node);
            });
        } catch (Throwable e) {
            //do nothing on error, since we don't need more OOMs
        }

        System.exit(1);
    }

    private final static Duration oneMin = Duration.ofMinutes(1);
    private final static Duration f4ever = Duration.ofDays(1_000_000_000);

    private synchronized GclNode gatherData(CancelGetter cancel) {
        final ZonedDateTime now = times.nowZ();
        final long start = times.toMilli(now);
        final int localWorkerCountStart = lwManager.getLocalWorkerCount();

        final JxJavaProcessMemoryUsage processMemoryUsedOnStart = systemInfo.getProcessMemoryUsed();

        final JxProcessorLoadData processorLoad = systemInfo.getProcessorLoad(4_000L);

        final JxJavaProcessMemoryUsage processMemoryUsedOnEnd = systemInfo.getProcessMemoryUsed();

        final int localWorkerCountEnd = lwManager.getLocalWorkerCount();
        final int avgLocalWorkers = (localWorkerCountEnd + localWorkerCountStart) / 2;
        final int desiredLocalWorkerCount = lwManager.getDesiredLocalWorkerCount();

        final long end = times.now();

        final X1<GclNode> nn = nodeStorage.transactionWithSaveX1(() -> {
            final GclNode nodeCurrentInfo = nodeStorage.getNodeCurrentInfo(new GclNodeId(nodeInfo.getNodeId()));
            final GclNodeInfo old = nodeCurrentInfo.getNInnerState();

            old.getHistory().addFirst(new GclNodeHistoryItem(
                    times.nowZ(), Duration.of(end - start, ChronoUnit.MILLIS),
                    processorLoad.avgProcessorLoad(), processorLoad.sortedLoadByCore(),
                    Math.min(processMemoryUsedOnStart.overallUsed(), processMemoryUsedOnEnd.overallUsed()),
                    Math.max(processMemoryUsedOnStart.overallUsed(), processMemoryUsedOnEnd.overallUsed()),
                    avgLocalWorkers,
                    desiredLocalWorkerCount
            ));

            {//must be after history insert
                final GclAvgStats avgNodeStat = calcAverageStats(old.getHistory(), now, oneMin);
                old.setLastMinNodeAvgStat(avgNodeStat);
                old.setLastMinPerWorkerAvgStat(avgNodeStat.divideBy(avgLocalWorkers));
            }

            {//must be after history insert
                final GclAvgStats avgWorkerStat = calcAverageStats(old.getHistory(), now, f4ever);
                old.setFullHistNodeAvgStat(avgWorkerStat);
                old.setFullHistPerWorkerAvgStat(avgWorkerStat.divideBy(avgLocalWorkers));
            }

            return X.x(nodeCurrentInfo);
        });

        return nn.getI1();
    }

    private synchronized void scaleIfNeeded(GclNodeInfo data) {
        data = scaleMaxWorkersUpIfOomWasLongAgo(data);
        scaleDesiredWorkersCountBasedOnLoadAndMinMax(data);
    }

    private void scaleDesiredWorkersCountBasedOnLoadAndMinMax(GclNodeInfo data) {
        final GclAvgStats lastAvgStats = data.getLastMinNodeAvgStat();
        final double maxProcessorLoadStart = 0.85;
        final double maxProcessorLoadEnd = 0.95;
        int newDesiredWorkers = lwManager.getDesiredLocalWorkerCount();
        if (lastAvgStats.avgProcLoad() < maxProcessorLoadStart) {
            final double multiplyer = maxProcessorLoadStart / lastAvgStats.avgProcLoad();
            newDesiredWorkers = (int) (multiplyer * data.getHistory().getFirst().currentWorkerCount());
        } else if (lastAvgStats.avgProcLoad() > maxProcessorLoadEnd) {
            newDesiredWorkers = (int) (data.getHistory().getFirst().currentWorkerCount() - 1);
        }
        lwManager.setDesiredLocalWorkerCount((int) Ma.clamp(newDesiredWorkers, data.getMinWorkers(), data.getCurMaxWorkers()));
    }

    private GclNodeInfo scaleMaxWorkersUpIfOomWasLongAgo(GclNodeInfo data) {
        final GclNodeInfo.OOMInfo oomInfo = data.getOom();
        final ZonedDateTime now = times.nowZ();
        if (data.getCurMaxWorkers() != data.getOverallMaxWorkers() && oomInfo.getLastMaxChange().isPresent()) {
            //OOM existed
            final ZonedDateTime lastChange = oomInfo.getLastMaxChange().get();
            if (Ti.between(lastChange, now).toMillis() > appProps.getDurationToScaleWorkersUp()) {
                final X1<GclNode> collect = nodeStorage.transactionWithSaveX1(() -> {
                    final GclNode nodeCurrentInfo = nodeStorage.getNodeCurrentInfo(new GclNodeId(nodeInfo.getNodeId()));
                    final GclNodeInfo old = nodeCurrentInfo.getNInnerState();
                    old.setCurMaxWorkers(old.getCurMaxWorkers() + (old.getOverallMaxWorkers() - old.getCurMaxWorkers()) / 2);
                    return X.x(nodeCurrentInfo);
                });
                return collect.i1.getNInnerState();
            }
        }
        return data;
    }


    private GclAvgStats calcAverageStats(DequeWithLimit<GclNodeHistoryItem> history, ZonedDateTime now, Duration timeBack) {
        final ZonedDateTime start = now.minus(timeBack);
        MinMaxAvg avgLoadCores = new MinMaxAvg();
        MinMaxAvg avgMinRam = new MinMaxAvg();
        MinMaxAvg avgMaxRam = new MinMaxAvg();
        for (GclNodeHistoryItem histItem : history.stream().takeWhile(item -> item.time().isAfter(start)).toList()) {
            avgLoadCores.add(histItem.avgLoad());
            avgMinRam.add(histItem.minUsedMemory());
            avgMaxRam.add(histItem.maxUsedMemory());
        }

        return new GclAvgStats(
                avgLoadCores.getAvg(),
                (long) avgMinRam.getAvg(),
                (long) avgMaxRam.getAvg()
        );
    }
}
