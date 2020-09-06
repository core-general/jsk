package sk.services.retry;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.services.retry.utils.BatchRepeatResult;
import sk.services.retry.utils.IdCallable;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.R;
import sk.utils.statics.Cc;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public interface IRepeat {
    <T> T repeat(
            @NotNull Supplier<T> toRun,
            @Nullable Supplier<T> onFail,
            int count,
            long sleepBetweenTries,
            @NotNull Set<Class<? extends Throwable>> allowedExceptions
    );

    default <T> T repeat(
            Supplier<T> toRun, int count, long sleepBetweenTries, Set<Class<? extends Throwable>> allowedExceptions
    ) {
        return repeat(toRun, null, count, sleepBetweenTries, allowedExceptions);
    }

    default <T> T repeat(Supplier<T> toRun, int count) {
        return repeat(toRun, null, count, 0, Cc.sEmpty());
    }

    default <T> T repeat(Supplier<T> toRun, int count, long sleepBetweenTries) {
        return repeat(toRun, null, count, sleepBetweenTries, Cc.sEmpty());
    }

    default <T> T repeat(Supplier<T> toRun, int count, Set<Class<? extends Throwable>> allowedExceptions) {
        return repeat(toRun, null, count, 0, allowedExceptions);
    }

    default <T> T repeat(Supplier<T> toRun, Supplier<T> onFail, int count) {
        return repeat(toRun, onFail, count, 0, Cc.sEmpty());
    }


    @SuppressWarnings("SameReturnValue")
    default void repeat(
            @NotNull R toRun,
            @Nullable R onFail,
            int count,
            long sleepBetweenTries,
            @NotNull Set<Class<? extends Throwable>> allowedExceptions
    ) {
        repeat(
                () -> {
                    toRun.run();
                    return null;
                },
                onFail != null ? () -> {
                    onFail.run();
                    return null;
                } : null,
                count, sleepBetweenTries, allowedExceptions
        );
    }

    default void repeat(R toRun, int count) {
        repeat(toRun, null, count, 0, Cc.sEmpty());
    }

    default void repeat(R toRun, int count, Set<Class<? extends Throwable>> allowedExceptions) {
        repeat(toRun, null, count, 0, allowedExceptions);
    }

    default void repeat(R toRun, int count, long sleepBetweenTries) {
        repeat(toRun, null, count, sleepBetweenTries, Cc.sEmpty());
    }

    default void repeat(R toRun, R onFail, int count) {
        repeat(toRun, onFail, count, 0, Cc.sEmpty());
    }


    <ID, T, A extends IdCallable<ID, T>> BatchRepeatResult<ID, T, A> repeatAndReturnResults(
            List<A> tasks, int maxRetryCount, long sleepAfterFailMs, ExecutorService pool,
            Set<Class<? extends Throwable>> exceptRetries, CancelGetter cancel);

    default <ID, T, A extends IdCallable<ID, T>> BatchRepeatResult<ID, T, A> repeatAndReturnResults(
            List<A> tasks, int maxRetryCount, long sleepAfterFailMs, ExecutorService pool,
            Set<Class<? extends Throwable>> exceptRetries) {
        return repeatAndReturnResults(tasks, maxRetryCount, sleepAfterFailMs, pool, exceptRetries, () -> false);
    }
}
