package sk.gcl.checker;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import lombok.*;
import sk.jfree.JGraphHelp;
import sk.math.data.MDataSet;
import sk.math.data.MDataSets;
import sk.services.CoreServicesRaw;
import sk.services.ICoreServices;
import sk.services.http.HttpImpl;
import sk.services.http.model.CoreHttpResponse;
import sk.utils.async.locks.JLock;
import sk.utils.async.locks.JLockDecorator;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.R;
import sk.utils.ids.IdUuid;
import sk.utils.ifaces.Identifiable;
import sk.utils.javafixes.argparser.ArgParser;
import sk.utils.javafixes.argparser.ArgParserConfigProvider;
import sk.utils.statics.*;

import java.awt.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.of;
import static sk.utils.statics.Cc.ts;

public class GccMain {
    static volatile EvaluatingActivity activity = null;

    final static ICoreServices core = new CoreServicesRaw();
    final static JLock activityChangeLock = new JLockDecorator();


    public static void main(String[] args) {
        ArgParser<GccMain.ARGS> params = ArgParser.parse(args, GccMain.ARGS.URL);
        System.out.println("Starting activity:" + params);
        activity = new GccMain.EvaluatingActivity(
                params.getRequiredArg(GccMain.ARGS.URL),
                params.getArg(GccMain.ARGS.NUM_OF_THREADS).map(Ma::pi).orElse(8),
                params.getArg(GccMain.ARGS.SLEEP_BETWEEN_REQUESTS_SEC).map(Ma::pi).orElse(1),
                params.getArg(GccMain.ARGS.MAX_PERIOD_SEC).map(Ma::pi).orElse(60),
                params.getArg(ARGS.RETRY).map(Ma::pi).orElse(1)
        );
        while (!activity.finished) {
            Ti.sleep(5000);
        }
    }

    static class EvaluatingActivity {
        public static final DateTimeFormatter GROUPING_DATE_FORMAT = Ti.HHmmss;
        final String url;
        final int sleepBetweenSec;
        final int maxPeriodSec;
        final int retryCount;

        final Map<GccEventId, GccHttpEvent> events = new ConcurrentHashMap<>();

        volatile boolean finished = false;

        final ZonedDateTime startTime = core.times().nowZ();
        ZonedDateTime finishTime;

        public EvaluatingActivity(String url, int numOfThreads, int sleepBetweenSec, int maxPeriodSec, int retryCount) {
            this.url = url;
            this.sleepBetweenSec = sleepBetweenSec;
            this.maxPeriodSec = maxPeriodSec;
            this.retryCount = retryCount;

            System.out.println();
            System.out.println("/tmp/g-cluster-checker/%s/NVER.png".formatted(startTime.format(Ti.yyyyMMddHHmmss)));
            System.out.println();

            core.async()
                    .runAsyncDontWait(IntStream.range(0, numOfThreads)
                            .mapToObj((i) -> (R) new GccWorker())
                            .toList())
                    .thenRun(() -> finish());

            //draw periodically
            core.async().runBuf(() -> {
                while (!finished) {
                    try {
                        activityChangeLock.runInLock(() -> {
                            draw(false);
                            System.out.println("Periodic draw finished...events:" + events.size());
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        Ti.sleep(7500);
                    }
                }
            });
        }


        public void finish() {
            activityChangeLock.runInLock(() -> {
                if (!finished) {
                    finished = true;
                    finishTime = core.times().nowZ();
                    System.out.println("Finishing activity %s...\n%d events in DB...".formatted(url, events.size()));
                    Ti.sleep(Ti.second);
                    draw(true);
                    System.out.println("Activity finished");
                }
            });
        }

        private void draw(boolean usefinishTime) {
            ZonedDateTime timeToCheck = usefinishTime ? finishTime : core.times().nowZ();
            core.async().runAsyncDontWait(Cc.l(
                    () -> fullDataProcessing($ -> $.code + "", code -> O.ofNull(Ma.pi(code) == 200
                                                                                ? Color.GREEN
                                                                                : Ma.inside(Ma.pi(code), 201, 299)
                                                                                  ? Color.BLUE
                                                                                  : Ma.inside(Ma.pi(code), 400, 499)
                                                                                    ? Color.YELLOW
                                                                                    : Ma.inside(Ma.pi(code), 500, 599)
                                                                                      ? Color.RED
                                                                                      : null), "HTTP_CODES", timeToCheck),
                    () -> fullDataProcessing($ -> $.exception.isPresent() + "",
                            code -> O.ofNull(Ma.pb(code) ? Color.RED : Color.GREEN),
                            "EXCEPTIONS", timeToCheck),
                    () -> fullDataProcessing($ -> $.exception.map($$ -> $$.getMessage()) + "",
                            code -> O.ofNull(St.isNotNullOrEmpty(code) ? Color.RED : Color.GREEN),
                            "EXCEPTION_TEXT", timeToCheck),
                    () -> fullDataProcessing($ -> $.timeout + "", code -> O.ofNull(Ma.pb(code) ? Color.RED : Color.GREEN),
                            "TIMEOUT", timeToCheck),
                    () -> fullDataProcessing($ -> ($.responseTimeMs / 100) * 100 + "",
                            code -> O.empty(),
                            "RESPONSE_TIMES", timeToCheck),
                    () -> fullDataProcessing($ -> $.getNVer(),
                            code -> O.empty(),
                            "NVER", timeToCheck)
            )).join();
        }

        void fullDataProcessing(F1<GccHttpEvent, String> grouping, F1<String, O<Color>> coloring, String name,
                ZonedDateTime timeToCheck) {
            Map<String, TreeMap<String, List<GccHttpEvent>>> byCode = groupingBy(grouping);
            MDataSets datasets = datasets(byCode, coloring, timeToCheck);
            datasets.setName(name);
            String file = "/tmp/g-cluster-checker/%s/%s.png.zgc".formatted(startTime.format(Ti.yyyyMMddHHmmss), name);
            JGraphHelp.save(file, JGraphHelp.lineChartX1(datasets));
            //done for smooth change in the linux image viewer
            Io.move(file, file.replace(".zgc", ""));
        }

        Map<String, TreeMap<String, List<GccHttpEvent>>> groupingBy(F1<GccHttpEvent, String> grouper) {
            return events.values().parallelStream()
                    .collect(Collectors.groupingBy($ -> grouper.apply($),
                            Collectors.groupingBy($ -> $.getHourMinAndSec(), () -> new TreeMap<>(), Collectors.toList())));
        }

        MDataSets datasets(Map<String, TreeMap<String, List<GccHttpEvent>>> data, F1<String, O<Color>> coloring,
                ZonedDateTime timeToCheck) {
            SortedSet<String> allSortedDates = new TreeSet<>();
            ZonedDateTime current = startTime;
            while (current.isBefore(timeToCheck)) {
                allSortedDates.add(current.format(GROUPING_DATE_FORMAT));
                current = current.plusSeconds(1);
            }

            List<MDataSet> timeLine = data.entrySet().parallelStream().map(dsTarget -> {
                MDataSet set = new MDataSet(
                        dsTarget.getKey(),
                        "Count",
                        "Time line",
                        allSortedDates.stream()
                                .map(xDate -> O.ofNull(dsTarget.getValue().get(xDate)).map($ -> $.size()).orElse(0))
                                .mapToDouble($ -> $).toArray(),
                        Ar.getValuesIncrementedBy1(allSortedDates.size())
                );
                coloring.apply(dsTarget.getKey()).ifPresent($ -> set.withColor($));
                return set;
            }).toList();
            return new MDataSets(timeLine);
        }

        @Data
        @RequiredArgsConstructor
        class GccWorker implements R, Identifiable<GccWorkerId> {
            final GccWorkerId id = new GccWorkerId(core.ids().shortId());

            long startTime = core.times().now();

            @Override
            public void run() {
                int delay = core.rand().rndInt(3000);
                System.out.println("Starting worker:" + id + " delay:" + delay);
                Ti.sleep(delay);
                long now = core.times().now();
                while (now - startTime < maxPeriodSec * Ti.second && !finished) {
                    try {
                        CoreHttpResponse coreHttpResponse = core.http()
                                .get(url)
                                .tryCount(retryCount)
                                .trySleepMs((int) (sleepBetweenSec * Ti.second))
                                .goResponseAndThrow();
                        long newNow = core.times().now();
                        GccHttpEvent event =
                                new GccHttpEvent(coreHttpResponse.code(), false, newNow - now, core.times().toZDT(newNow),
                                        coreHttpResponse.getHeader("_nver").orElse("NO_NVER"),
                                        empty(), of(coreHttpResponse));
                        events.put(event.getId(), event);
                    } catch (HttpImpl.RetryException e) {
                        long newNow = core.times().now();
                        GccHttpEvent event =
                                new GccHttpEvent(e.getCode(), false, newNow - now, core.times().toZDT(newNow),
                                        e.getResp().getHeader("_nver").orElse("NO_NVER"),
                                        empty(), of(e.getResp()));
                        events.put(event.getId(), event);
                    } catch (Exception e) {
                        long newNow = core.times().now();
                        GccHttpEvent event = new GccHttpEvent("UNKNOWN", e, newNow - now, core.times().toZDT(newNow));
                        events.put(event.getId(), event);
                    } finally {
                        Ti.sleep(sleepBetweenSec * Ti.second);
                        now = core.times().now();
                    }
                }
                System.out.println("Finished Worker:" + id);
            }
        }

        @Data
        @RequiredArgsConstructor
        class GccHttpEvent implements Identifiable<GccEventId> {
            final GccEventId id = new GccEventId(core.ids().shortId());
            final int code;
            final boolean timeout;
            final long responseTimeMs;
            final ZonedDateTime now;
            final String nVer;
            final O<Exception> exception;
            final O<CoreHttpResponse> response;

            public GccHttpEvent(String nver, Exception e, long responseTimeMs, ZonedDateTime now) {
                this(-1, e.getMessage().toLowerCase().contains("timeout"), responseTimeMs, now, nver, of(e), empty());
            }

            public String getHourMinAndSec() {
                return now.format(GROUPING_DATE_FORMAT);
            }
        }

        @NoArgsConstructor
        static class GccWorkerId extends IdUuid {
            public GccWorkerId(UUID uuid) {
                super(uuid);
            }
        }

        @NoArgsConstructor
        static class GccEventId extends IdUuid {
            public GccEventId(UUID uuid) {
                super(uuid);
            }
        }
    }


    @AllArgsConstructor
    @Getter
    enum ARGS implements ArgParserConfigProvider<ARGS> {
        URL(O.empty(), true, "url to check aliveness"),
        NUM_OF_THREADS(of(ts("-t")), false, "Number of threads"),
        SLEEP_BETWEEN_REQUESTS_SEC(of(ts("-s")), false, "Sleep between requests seconds"),
        RETRY(of(ts("-r")), false, "Retry count"),
        MAX_PERIOD_SEC(of(ts("-p")), false, "Max period of evaluation seconds");

        final O<TreeSet<String>> commandPrefix;
        final boolean required;
        final String description;

        @Override
        public ARGS[] getArrConfs() {return values();}
    }
}
