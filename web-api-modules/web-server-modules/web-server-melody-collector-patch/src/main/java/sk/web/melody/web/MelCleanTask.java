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

import sk.services.async.IAsync;
import sk.services.http.IHttp;
import sk.services.http.model.CoreHttpResponse;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Fu;
import sk.utils.statics.Ti;
import sk.utils.tuples.X2;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static sk.web.melody.web.MelNodeManagementService.removeNode;

public class MelCleanTask extends ForeverThreadWithFinish {
    public MelCleanTask(IAsync async, IHttp http) {
        super(() -> {
            try {
                List<X2<String, URL>> toTest = MelNodeManagementService.getAllNodes();
                final List<F0<O<X2<String, URL>>>> toDeleteTasks = toTest.stream()
                        .map(nodeInfo -> (F0<O<X2<String, URL>>>) () -> {
                                    final URL uurl = nodeInfo.i2;
                                    final String url = uurl.toString();
                                    System.out.println("Trying:" + url);
                                    final String[] loginPas = uurl.getUserInfo().split(":");

                                    final OneOf<CoreHttpResponse, Exception> response = http
                                            .get(url)
                                            .login(loginPas[0])
                                            .password(loginPas[1])
                                            .tryCount(10).goResponse();

                                    System.out.println(
                                            "Result:" + response.map($ -> $.code() + "",
                                                    e -> e.getMessage() + ""));
                                    return response.collect(x -> x.code() == 200
                                                    ? O.empty() // if ok, we do not delete, else or if exception delete
                                                    : O.of(nodeInfo),
                                            e -> O.of(nodeInfo));
                                }
                        )
                        .collect(Collectors.toList());

                final List<O<X2<String, URL>>> toDeleteFinished = async.supplyParallel(toDeleteTasks);

                System.out.println("Removing nodes: " +
                        toDeleteFinished.stream().filter($ -> $.isPresent()).map($ -> $.get().toString()).collect(joining(", ")));

                toDeleteFinished
                        .stream()
                        .filter($ -> $.isPresent())
                        .map($ -> $.get())
                        .forEach($ -> {
                            removeNode($.i1(), $$ -> Fu.equal($$.toString(), $.i2().toString()));
                        });

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                async.sleep(5 * Ti.minute);
            }
        }, "mel_cleaner", true);
    }
}
