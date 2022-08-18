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

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import sk.mvn.ApiClassUtil;
import sk.services.nodeinfo.IBeanInfoSubscriber;
import sk.services.nodeinfo.IIpProvider;
import sk.services.nodeinfo.INodeInfo;
import sk.services.nodeinfo.model.ApiBuildInfo;
import sk.services.nodeinfo.model.IServerInfo;
import sk.services.rand.IRand;
import sk.services.shutdown.AppStopListener;
import sk.services.shutdown.AppStopService;
import sk.services.time.ITime;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.St;
import sk.utils.tuples.X;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class WebServerNodeInfo implements INodeInfo, AppStopListener {
    @Inject IRand rnd;
    @Inject ITime times;
    @Inject ApiClassUtil clsUtil;
    @Inject AppStopService appStop;
    @Inject Optional<IIpProvider> ipProvider = Optional.empty();

    @Getter String nodeId;
    @Getter String nodeVersion;
    @Getter ZonedDateTime buildTime;

    @Inject List<IBeanInfoSubscriber<?>> beanInfos = Cc.l();
    Map<String, F0<?>> beanInfosProcessed;
    IServerInfo serverInfo;

    @PostConstruct
    void init() {
        nodeId = rnd.rndString(3, St.engENGDig) + "-" + (System.currentTimeMillis());
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
                                    (a, b) -> Ex.thRow(String.format("keys equals: %s==%s", a, b)), TreeMap::new));

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

/*    @Override
    public SortedMap<String, Object> getCurrentServerInfo(O<List<String>> filter) {
        final Map<String, Object> collect = beanInfos.stream()
                .map($ -> {
                    try {
                        return (X2<String, Object>) $.gatherDiagnosticInfo();
                    } catch (Exception e) {
                        log.error(e);
                        return null;
                    }
                })
                .filter(Fu.notNull())
                .collect(Collectors.groupingBy($ -> $.i1(), Collectors.mapping($ -> $.i2(), Cc.toL())))
                .entrySet().stream()
                .map($ -> X.<String, Object>x($.getKey(), $.getValue().size() == 0
                                                          ? ""
                                                          : $.getValue().size() == 1
                                                            ? $.getValue().get(0)
                                                            : $.getValue()))
                .collect(Cc.toMX2());
        collect.put("_nodeId", Cc.l(nodeId));
        collect.put("_nodeVersion", Cc.l(nodeVersion));
        collect.put("_buildTime", Cc.l(Ti.yyyyMMddHHmmss.format(buildTime)));

        return new TreeMap<>(collect);
    }*/

    @Override
    public boolean isShuttingDown() {
        return appStop.isStopped();
    }

    @Override
    public long waitBeforeStopMs() {
        return 100;
    }
}
