package sk.services.async;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import sk.utils.functional.F0;

import javax.inject.Inject;

@RequiredArgsConstructor
public class ISizedSemaphoreImpl implements ISizedSemaphore {
    private final Object sync = new Object();
    private final long maxSize;
    private final long maxCount;
    private final long waitTime;

    @Inject ISleep sleeper;

    @Getter volatile long curSize = 0;
    @Getter volatile long count = 0;

    public ISizedSemaphoreImpl(long maxSize, long maxCount, long waitTime, ISleep sleeper) {
        this.maxSize = maxSize;
        this.maxCount = maxCount;
        this.waitTime = waitTime;
        this.sleeper = sleeper;
    }

    @Override
    public <T> T acquireLockAndReturn(long lockSize, F0<T> toReturn) {
        try (Lock lock = lock(lockSize)) {
            return toReturn.apply();
        }
    }

    private Lock lock(long lockSize) {
        while (true) {
            synchronized (sync) {
                if (count == 0 || curSize + lockSize <= maxSize && count + 1 <= maxCount) {
                    curSize += lockSize;
                    count += 1;
                    return new Lock(lockSize);
                }
            }
            sleeper.sleep(waitTime);
        }
    }

    @AllArgsConstructor
    private class Lock implements AutoCloseable {
        long lockSize;

        @Override
        public void close() {
            synchronized (sync) {
                curSize -= lockSize;
                count -= 1;
            }
        }
    }
    //
    //public static void main(String[] args) {
    //    SizedSemaphore ss = new SizedSemaphore(1000, () -> Ti.sleep(100));
    //    ForkJoinPool fjp = new ForkJoinPool(200);
    //    AtomicLong lng = new AtomicLong();
    //    fjp.submit(() -> {
    //        IntStream.range(0, 1000).parallel().mapToObj($ -> $)
    //                .sorted((o1, o2) -> (int) Math.round(Math.random() * 1000))
    //                .map($ ->
    //                        X.x($, (R) () -> {
    //                            try {
    //                                System.out
    //                                        .println("Processing " + $ + ". Size:" + ss.getCurSize() + " Count:" + ss
    //                                        .getCount());
    //                                Ti.sleep($);
    //                                System.out.println("Stopping " + $ + ". Size:" + ss.getCurSize() + " Count:" + ss
    //                                .getCount());
    //                                lng.addAndGet($);
    //                            } catch (Exception e) {
    //                                e.printStackTrace();
    //                            }
    //                        }))
    //                .forEach($ -> ss.acquireLockAndRun($.i1, $.i2));
    //    }).join();
    //
    //    System.out.println(lng.get());
    //}
}
