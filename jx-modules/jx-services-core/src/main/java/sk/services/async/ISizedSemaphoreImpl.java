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
import sk.utils.functional.F0;
import sk.utils.statics.Ex;

public class ISizedSemaphoreImpl implements ISizedSemaphore {
    private final Object sync = new Object();
    private final long maxSize;
    private final long maxCount;

    @Getter long curSize = 0;
    @Getter long count = 0;

    public ISizedSemaphoreImpl(long maxSize, long maxCount) {
        this.maxSize = maxSize;
        this.maxCount = maxCount;
    }

    @Override
    public <T> T acquireLockAndReturn(long lockSize, F0<T> toReturn) {
        try (Lock lock = lock(lockSize)) {
            return toReturn.apply();
        }
    }

    private Lock lock(long lockSize) {
        synchronized (sync) {
            while (!(count == 0 || curSize + lockSize <= maxSize && count + 1 <= maxCount)) {
                Ex.toRuntime(() -> sync.wait());
            }
            curSize += lockSize;
            count += 1;
            return new Lock(lockSize);
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
                sync.notifyAll();
            }
        }
    }

    //public static void main(String[] args) {
    //    ISizedSemaphoreImpl ss = new ISizedSemaphoreImpl(Long.MAX_VALUE, 3);
    //    ForkJoinPool fjp = new ForkJoinPool(200);
    //    AtomicLong lng = new AtomicLong();
    //    Profiler.mark("1");
    //    XXX x = new XXX();
    //    fjp.submit(() -> {
    //        IntStream.range(0, 30_000_000).parallel()
    //                .forEach($ -> ss.acquireLockAndRun(200, () -> {
    //                    for (int j = 0; j < 1; j++) {
    //                        x.al.incrementAndGet();
    //                    }
    //                }));
    //    }).join();
    //    Profiler.mark("2");
    //    //double x = 5;
    //    //for (int i = 0; i < 30_000_000; i++) {
    //    //    for (int j = 0; j < 100; j++) {
    //    //        x *= x;
    //    //    }
    //    //}
    //    //Profiler.mark("3");
    //    System.out.println(Profiler.getInfo());
    //    System.out.println(x.al.get());
    //
    //}
    //
    //private static class XXX {
    //    AtomicLong al = new AtomicLong(0);
    //}
}
