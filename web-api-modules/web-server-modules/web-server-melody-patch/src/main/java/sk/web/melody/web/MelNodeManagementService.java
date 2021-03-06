package sk.web.melody.web;

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

import net.bull.javamelody.internal.common.Parameters;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Fu;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class MelNodeManagementService {
    private static final String FORMATING_URL = "http://%s:%s@%s:%s";
    private static final X2<String, Integer> OK = X.x("OK", 200);

    public synchronized static X2<String, Integer> addNode(String appName, String nodeIp, String port, String login, String pas) {
        try {
            System.out.println("Adding node:" + nodeIp + ":" + port + " ...");
            URL url = new URL(String.format(FORMATING_URL, login, pas, nodeIp, port));
            List<URL> curAppUrls = O.ofNull(Parameters.getCollectorUrlsByApplications().get(appName)).orElseGet(() -> Cc.l());
            if (curAppUrls.stream().noneMatch($ -> Fu.equal($.getHost() + ":" + $.getPort(), nodeIp + ":" + port))) {
                curAppUrls.add(url);
                Parameters.addCollectorApplication(appName, curAppUrls);
                System.out.println("Added node:" + nodeIp + ":" + port);
                try {
                    System.out.println("Restarting due:" + nodeIp);
                    Runtime.getRuntime().exec("./restart_server.sh");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return OK;
        } catch (IOException e) {
            return Ex.thRow(e);
        }
    }

    public synchronized static X2<String, Integer> removeNode(String appName, String nodeIp, String port) {
        System.out.println("Removing node:" + nodeIp + ":" + port + " ...");
        final String urlPart = nodeIp + ":" + port;
        final Predicate<URL> equalityCheck = $ -> $.toString().contains(urlPart);
        return removeNode(appName, equalityCheck);
    }

    public synchronized static X2<String, Integer> removeNode(String appName, Predicate<URL> equalityCheck) {
        List<URL> curAppUrls =
                new ArrayList<>(O.ofNull(
                        Ex.toRuntime(() -> Parameters.getCollectorUrlsByApplications().get(appName))).orElseGet(() -> Cc.l()));
        final Optional<URL> toRemove = curAppUrls.stream()
                .filter(equalityCheck)
                .findFirst();
        if (!toRemove.isPresent()) {
            System.out.println("Nothing to remove");
            return OK;
        }
        final URL url = toRemove.get();
        if (curAppUrls.size() < 2) {
            System.out.println("We will not delete node since it's last: " + url);
            // do nothing to prevent removal of app from melody
            return OK;
        }
        curAppUrls.remove(url);
        try {
            Parameters.addCollectorApplication(appName, curAppUrls);
        } catch (IOException e) {
            return Ex.thRow(e);
        }
        System.out.println("Removed node:" + url);
        return OK;
    }

    public static synchronized List<X2<String, URL>> getAllNodes() {
        final Map<String, List<URL>> items;
        try {
            items = Parameters.getCollectorUrlsByApplications();
        } catch (IOException e) {
            return Ex.thRow(e);
        }
        return items.entrySet().stream()
                .flatMap($ -> $.getValue().stream().map($$ -> X.x($.getKey(), $$)))
                .collect(Cc.toL());
    }
}
