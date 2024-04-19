package sk.web.server.spark.spring;

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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sk.mvn.ApiClassUtil;
import sk.services.ids.IIds;
import sk.services.nodeinfo.IBeanInfoSubscriber;
import sk.services.nodeinfo.IIpProvider;
import sk.services.nodeinfo.INodeInfo;
import sk.services.nodeinfo.model.ApiBuildInfo;
import sk.services.nodeinfo.model.IServerInfo;
import sk.services.shutdown.AppStopListener;
import sk.services.shutdown.AppStopService;
import sk.services.shutdown.INodeRestartStorage;
import sk.services.time.ITime;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.tuples.X;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Slf4j
public class WebServerNodeInfo implements INodeInfo, AppStopListener {
    @Inject IIds ids;
    @Inject ITime times;
    @Inject ApiClassUtil clsUtil;
    @Inject INodeRestartStorage nodeRestartStorage;
    @Inject AppStopService appStop;
    @Inject Optional<IIpProvider> ipProvider = Optional.empty();

    @Getter String nodeId;
    @Getter String nodeVersion;
    @Getter ZonedDateTime buildTime;
    @Getter ZonedDateTime startTime;

    @Inject List<IBeanInfoSubscriber<?>> beanInfos = Cc.l();
    Map<String, F0<?>> beanInfosProcessed;
    IServerInfo serverInfo;

    @PostConstruct
    void init() {
        startTime = times.nowZ();

        nodeId = nodeRestartStorage.getStringAfterRestart("node_id").orElseGet(() -> {
            final String nodeId = "%d-%s".formatted(times.toMilli(startTime), ids.tinyHaiku());
            nodeRestartStorage.setDataForRestart("node_id", nodeId);
            return nodeId;
        });
        final O<ApiBuildInfo> bi = clsUtil.getVersionAndBuildTimeFromResources();
        buildTime = bi.map($ -> times.toZDT($.getBuildTime())).orElseGet(() -> times.toZDT(0));
        nodeVersion = bi.map($ -> $.getVersion()).orElse("?") + "-" + times.toMilli(buildTime);
        beanInfosProcessed = beanInfos.stream().map($ -> $.gatherDiagnosticInfo())
                .collect(Collectors.toMap($ -> $.getName().trim(), $ -> (F0<?>) $.getInfoProvider()));
        final List<String> allCategories = Collections.unmodifiableList(Cc.sort(new ArrayList<>(beanInfosProcessed.keySet())));
        serverInfo = new IServerInfo(
                allCategories,
                (filterList) -> {
                    return (filterList.size() == 0 ? allCategories : filterList).stream()
                            .map($ -> X.x($.trim(), beanInfosProcessed.getOrDefault($.trim(), () -> null).apply()))
                            .filter($ -> $.i2() != null)
                            .collect(Collectors.toMap($ -> $.i1(), $ -> $.i2(),
                                    new BinaryOperator<Object>() {
                                        @Override
                                        public Object apply(Object a, Object b) {
                                            return Ex.thRow(String.format("keys equals: %s==%s", a, b));
                                        }
                                    }, TreeMap::new));
                }
        );
    }

    @Override
    public O<String> getPublicIp() {
        return O.of(ipProvider).flatMap($ -> $.getMyIp());
    }

    @Override
    public O<String> getPrivateIp() {
        return O.of(ipProvider).flatMap($ -> $.getMyPrivateIp());
    }

    @Override
    public IServerInfo getCurrentServerInfo() {
        return serverInfo;
    }

    @Override
    public boolean isShuttingDown() {
        return appStop.isStopped();
    }

    @Override
    public long waitBeforeStopMs() {
        return 100;
    }
}
