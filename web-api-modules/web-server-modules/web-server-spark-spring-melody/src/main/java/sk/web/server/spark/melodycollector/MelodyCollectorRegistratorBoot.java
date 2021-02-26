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

import lombok.extern.log4j.Log4j2;
import sk.services.async.IAsync;
import sk.services.boot.IBoot;
import sk.services.http.IHttp;
import sk.services.nodeinfo.INodeInfo;
import sk.services.shutdown.AppStopListener;
import sk.utils.async.GuaranteedOneTimeTask;
import sk.utils.functional.O;
import sk.web.server.params.WebServerParams;

import javax.inject.Inject;

@Log4j2
public class MelodyCollectorRegistratorBoot implements IBoot, AppStopListener {

    @Inject private INodeInfo nodeInfo;
    @Inject private IHttp http;
    @Inject private IAsync async;
    @Inject private WebMelodyParams moniParam;
    @Inject private WebMelodyCollectorParams moniColParam;
    @Inject private WebServerParams webSrv;
    private O<String> publicIp;

    @Override
    public void run() {
        publicIp = nodeInfo.getPublicIp();
        if (publicIp.isEmpty()) {
            log.error("Can't obtain public UP");
            return;
        }
        invokeCollectorTask(getUrlNodeWithAction("add_node"), 120_000, true);
    }

    @Override
    public void onStop() {
        if (publicIp.isEmpty()) {
            return;
        }
        invokeCollectorTask(getUrlNodeWithAction("remove_node"), 1000, false);
    }

    private String getUrlNodeWithAction(String action) {
        String port = String.valueOf(webSrv.getPort());
        String login = moniParam.getLogin();
        String password = moniParam.getPass();
        String appName = moniParam.getAppName();

        return String.format(
                "https://%s:%d/api/%s?" +
                        "app_name=%s&node_ip=%s&node_port=%s&node_login=%s&node_password=%s&use_https=false",
                moniColParam.getHost(), moniColParam.getPort(), action, appName, publicIp.get(), port, login, password);
    }


    private void invokeCollectorTask(String url, long delay, boolean continueOnSuccess) {
        final GuaranteedOneTimeTask<String> oneTimeTask = new GuaranteedOneTimeTask<>(
                () -> moniColParam.isMelodyCollectorOn()
                        ? http.get(url).login(moniColParam.getLogin()).password(moniColParam.getPass()).go().left()
                        : "", async.scheduledExec(), delay, 0);
        oneTimeTask.getFuture().thenApply(res -> {
            if (continueOnSuccess) {
                invokeCollectorTask(url, delay, continueOnSuccess);
            }
            return null;
        }).exceptionally(e -> {
            log.error(e);
            return null;
        });
    }

    @Override
    public long waitBeforeStopMs() {
        if (publicIp.isEmpty()) {
            return 0;
        }
        return 100;
    }
}
