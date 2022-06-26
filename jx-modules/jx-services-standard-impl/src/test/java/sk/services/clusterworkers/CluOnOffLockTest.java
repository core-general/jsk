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
import sk.services.time.TimeUtcImpl;
import sk.utils.functional.F1;
import sk.utils.statics.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CluOnOffLockTest {
    static final Map<String, Boolean> lock = new HashMap<>();

    public static void main(String[] args) {
        System.setProperty("AsyncImpl.scheduledPoolSize", "5");//lower as much as possible

        final AtomicBoolean onOff = new AtomicBoolean(true);
        AsyncImpl async = new AsyncImpl();
        TimeUtcImpl time = new TimeUtcImpl();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        F1<String, CluOnOffWithLockWorker> w1 =
                (s) -> {
                    return new CluOnOffWithLockWorker<CluOnOffWithLockWorker.IConf>(s, async, time) {
                        @Override
                        public synchronized void start(IConf conf) throws RuntimeException {
                            super.start(new Config(
                                    3, 1000, false,
                                    CluDelay.fixed(1000l),
                                    cancel -> {
                                        System.out.println(s);
                                        try {
                                            if (Ma.rand(0, 1000) < 500) {
                                                throw new RuntimeException();
                                            }
                                            if (Ma.rand(0, 1000) < 900) {
                                                System.out.println("Oops sleep! " + s);

                                                try {
                                                    async.sleep(10_000);
                                                    System.out.println("Sleep ok! " + s);

                                                } catch (RuntimeException e) {
                                                    release(s);
                                                    System.out.println("Stop sleep! " + s);
                                                }
                                            }
                                        } catch (Exception e) {
                                            release(s);
                                            System.out.println("Fail sleep! " + s);
                                            throw e;
                                        }
                                    },
                                    () -> onOff.get(),
                                    () -> tryAcquire(s),
                                    () -> {
                                        if (Ma.rand(0, 10000) < 1) {
                                            System.out.println("Renew failed! " + s);
                                            release(s);
                                            return false;
                                        } else {
                                            return true;
                                        }
                                    },
                                    w -> System.out.println(Ex.getInfo(w))

                            ));
                        }
                    };
                };
        List<CluOnOffWithLockWorker> x = Cc.l();
        int coun = 100;
        for (int i = 0; i < coun; i++) {
            CluOnOffWithLockWorker apply = w1.apply("T - " + i);
            apply.start(null);
            x.add(apply);
        }

        Io.endlessReadFromKeyboard("stop", s -> {
            if (Fu.equal(s, "on")) {
                lock.clear();
                onOff.set(true);
            } else {
                lock.clear();
                onOff.set(false);
            }
        });


        for (int i = 0; i < coun; i++) {
            System.out.println("Stoppin:" + i);
            x.get(i).stop(1000);
        }
    }

    private static synchronized boolean tryAcquire(String s) {
        boolean ok = lock.values().stream().allMatch($ -> !$);
        if (ok) {
            lock.put(s, true);
            System.out.println("Lock obtained " + s);
        }
        return ok;
    }

    private static synchronized void release(String s) {
        boolean isLocked = lock.getOrDefault(s, false);
        if (isLocked) {
            lock.put(s, false);
            System.out.println("Lock released " + s);
        }
    }
}
