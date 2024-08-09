package sk.web.server.spark.melodycollector;

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

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sk.services.async.IAsync;
import sk.services.boot.IBoot;
import sk.services.http.IHttp;
import sk.services.nodeinfo.INodeInfo;
import sk.services.shutdown.AppStopListener;
import sk.utils.async.GuaranteedOneTimeTask;
import sk.utils.functional.O;
import sk.web.server.params.WebServerParams;

@Slf4j
public class MelodyCollectorRegistratorBoot implements IBoot, AppStopListener {

    @Inject private INodeInfo nodeInfo;
    @Inject private IHttp http;
    @Inject private IAsync async;
    @Inject private WebMelodyParams moniParam;
    @Inject private WebMelodyCollectorParams moniColParam;
    @Inject private WebServerParams webSrv;
    private volatile O<String> ip = O.empty();

    @Override
    public void run() {
        if (moniColParam.isMelodyCollectorOn()) {
            ip = moniColParam.isUsePrivateIp() ? nodeInfo.getPrivateIp() : nodeInfo.getPublicIp();
            if (ip.isEmpty()) {
                log.error("Can't obtain Ip");
                return;
            }
            invokeCollectorTask(getUrlNodeWithAction("add_node"), 120_000, true);
        }
    }

    @Override
    public void onStop() {
        if (moniColParam.isMelodyCollectorOn()) {
            if (ip.isEmpty()) {
                return;
            }
            invokeCollectorTask(getUrlNodeWithAction("remove_node"), 1000, false);
        }
    }

    private String getUrlNodeWithAction(String action) {
        String port = String.valueOf(webSrv.getPort());
        String login = moniParam.getLogin();
        String password = moniParam.getPass();
        String appName = moniParam.getAppName();

        return String.format(
                "%s:%d/api/%s?" +
                "app_name=%s&node_ip=%s&node_port=%s&node_login=%s&node_password=%s&use_https=false",
                moniColParam.getHost().startsWith("http") ? moniColParam.getHost() : "https://" + moniColParam.getHost(),
                moniColParam.getPort(), action, appName, ip.get(), port, login, password);
    }


    private void invokeCollectorTask(String url, long delay, boolean continueOnSuccess) {
        final GuaranteedOneTimeTask<String> oneTimeTask = new GuaranteedOneTimeTask<>(
                () -> moniColParam.isMelodyCollectorOn()
                      ? http.get(url).login(moniColParam.getLogin()).password(moniColParam.getPass()).go().left()
                      : "", async.scheduledExec().getUnderlying(), delay, 0);
        oneTimeTask.getFuture().thenApply(res -> {
            if (continueOnSuccess) {
                invokeCollectorTask(url, delay, continueOnSuccess);
            }
            return null;
        }).exceptionally(e -> {
            log.error("", e);
            return null;
        });
    }

    @Override
    public long waitBeforeStopMs() {
        if (ip.isEmpty()) {
            return 0;
        }
        return 100;
    }
}
