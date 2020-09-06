package sk.services.clusterworkers;

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

import sk.services.async.AsyncImpl;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.rand.IRand;
import sk.services.rand.RandImpl;
import sk.services.time.TimeUtcImpl;
import sk.utils.functional.F1;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CluOnOffTest {
    public static void main(String[] args) {
        final AtomicBoolean onOff = new AtomicBoolean(true);
        AsyncImpl async = new AsyncImpl();
        TimeUtcImpl time = new TimeUtcImpl();
        IRand rnd = new RandImpl();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        F1<String, CluOnOffWorker<CluOnOffWorker.IConf>> w1 = s -> {
            return new CluOnOffWorker<CluOnOffWorker.IConf>(s, async, time) {
                @Override
                public synchronized void start(IConf config) throws RuntimeException {
                    super.start(new Config(
                            CluDelay.fixed(2000L),
                            //CluDelay.cron(time, "0/1 * * * * ?"),
                            cancelGetter -> {
                                System.out.println(dtf.format(time.nowZ()) + " " + s);
                            },
                            100,
                            () -> onOff.get(),
                            w -> System.out.println(Ex.getInfo(w))
                    ));
                }
            };
        };
        List<CluOnOffWorker> workers = Cc.l();
        int coun = 100;
        for (int i = 0; i < coun; i++) {
            CluOnOffWorker<CluOnOffWorker.IConf> worker = w1.apply(i + "");
            worker.start(null);
            workers.add(worker);
        }

        Io.endlessReadFromKeyboard("stop", s -> {
            if (Fu.equal(s, "on")) {
                onOff.set(true);
            } else {
                onOff.set(false);
            }
        });

        for (int i = 0; i < coun; i++) {
            System.out.println("Stoppin:" + i);
            workers.get(i).stop(1000);
        }
    }
}
