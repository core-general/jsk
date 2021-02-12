package sk.utils.statics.fortest;

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

import sk.utils.minmax.MinMaxAvg;
import sk.utils.statics.Cc;
import sk.utils.statics.Ti;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Profiler {
    private final static ThreadLocal<Map<String, X2<String, Long>>> currentLocks = new ThreadLocal<>();
    private final static ConcurrentMap<String, Map<String, MinMaxAvg>> profInfo = new ConcurrentHashMap<>();

    public static void mark(String group, String label) {
        privateMark(group, label, false);
    }

    public static void finishMark(String group, String label) {
        privateMark(group, label, true);
    }

    public static void mark(String label) {
        mark("DEFAULT", label);
    }

    public static void finishMark(String label) {
        finishMark("DEFAULT", label);
    }

    public static String getInfo() {
        return profInfo.entrySet().stream().sorted(Comparator.comparing($ -> $.getKey()))
                .map($ -> {
                    Set<Map.Entry<String, MinMaxAvg>> obj = $.getValue().entrySet();
                    String val = obj.stream().sorted(Comparator.comparing(x -> x.getKey()))
                            .map(x -> "    " + x.getKey() + " " + x.getValue().toString())
                            .collect(Collectors.joining("\n"));
                    return new StringBuilder($.getKey()).append("\n").append(val);
                }).collect(Collectors.joining("\n\n"));
    }

    public static void printInfo() {
        System.out.println(getInfo());
    }

    private static void privateMark(String group, String label, boolean finish) {
        Map<String, X2<String, Long>> map = currentLocks.get();
        if (map == null) {
            currentLocks.set(map = new HashMap<>());
        }

        X2<String, Long> byLabel = map.get(group);
        if (byLabel == null) {
            map.put(group, byLabel = X.x(label, System.nanoTime()));
        } else {
            String lbl = byLabel.i1();
            long lastMark = byLabel.i2();
            Cc.computeAndApply(profInfo, group,
                    (k, v) -> {
                        Cc.computeAndApply(v, lbl + " -> " + label, (k1, v1) -> v1.add((System.nanoTime() - lastMark) / 1000000f),
                                MinMaxAvg::new);
                        return v;
                    }, ConcurrentHashMap::new);
            if (!finish) {
                byLabel.i1(label);
                byLabel.i2(System.nanoTime());
            } else {
                map.remove(group);
            }
        }
    }


    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(100);

        CompletableFuture
                .allOf(IntStream.range(0, 100).mapToObj($ -> CompletableFuture.runAsync(() -> testCase(), pool))
                        .toArray(CompletableFuture[]::new)).thenRun(() -> {
            Profiler.printInfo();
            pool.shutdownNow();
        });


    }

    private static void testCase() {
        Profiler.mark("a");
        Ti.sleep((long) (100 + 1000 * Math.random()));
        Profiler.mark("b");
        Ti.sleep((long) (200 + 1000 * Math.random()));

        System.out.println("x");

        Profiler.mark("x", "a");
        Ti.sleep((long) (300 + 1000 * Math.random()));
        Profiler.mark("x", "b");
        Ti.sleep((long) (400 + 1000 * Math.random()));
        Profiler.mark("x", "c");
        Ti.sleep((long) (500 + 1000 * Math.random()));

        Profiler.mark("c");
        Ti.sleep((long) (600 + 1000 * Math.random()));
        Profiler.mark("d");
    }

}
