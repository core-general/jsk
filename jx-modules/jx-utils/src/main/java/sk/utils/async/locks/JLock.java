package sk.utils.async.locks;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import sk.utils.functional.Gett;
import sk.utils.functional.O;
import sk.utils.functional.R;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public interface JLock extends Lock {
    //public default boolean runIfLockFree(R toRun) {
    //    return getIfLockFree(() -> {
    //        toRun.run();
    //        return true;
    //    }).orElse(false);
    //}

    public default void runInLock(R toRun) {
        getInLock(() -> {
            toRun.run();
            return null;
        });
    }

    public default <T> O<T> getIfLockFree(Gett<T> getter) {
        if (tryLock()) {
            try {
                return O.of(getter.get());
            } finally {
                unlock();
            }
        } else {
            return O.empty();
        }
    }

    public default <T> O<T> getIfLockFree(Gett<T> getter, Duration duration) {
        try {
            if (tryLock(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                try {
                    return O.of(getter.get());
                } finally {
                    unlock();
                }
            } else {
                return O.empty();
            }
        } catch (InterruptedException e) {
            return O.empty();
        }
    }

    public default <T> T getInLock(Gett<T> toRun) {
        try {
            lock();
            return toRun.get();
        } finally {
            unlock();
        }
    }
}
