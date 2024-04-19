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


import sk.services.async.IExecutorService;
import sk.services.retry.utils.BatchRepeatResult;
import sk.services.retry.utils.IdCallable;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.F0;
import sk.utils.functional.F0E;
import sk.utils.functional.R;
import sk.utils.functional.RE;
import sk.utils.statics.Cc;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public interface IRepeat {

    Set<Class<? extends Throwable>> ALL_EXCEPTIONS_ALLOWED = Cc.s(Exception.class);

    //region F0
    <T> T repeat(
            F0<T> toRun,
            F0<T> onFail,
            int count,
            long sleepBetweenTries,
            Set<Class<? extends Throwable>> allowedExceptions
    );

    default <T> T repeat(
            F0<T> toRun, int count, long sleepBetweenTries, Set<Class<? extends Throwable>> allowedExceptions
    ) {
        return repeat(toRun, null, count, sleepBetweenTries, allowedExceptions);
    }

    default <T> T repeat(F0<T> toRun, int count, Set<Class<? extends Throwable>> allowedExceptions) {
        return repeat(toRun, null, count, 0, allowedExceptions);
    }

    default <T> T repeat(F0<T> toRun, int count) {
        return repeat(toRun, null, count, 0, ALL_EXCEPTIONS_ALLOWED);
    }

    default <T> T repeat(F0<T> toRun, int count, long sleepBetweenTries) {
        return repeat(toRun, null, count, sleepBetweenTries, ALL_EXCEPTIONS_ALLOWED);
    }

    default <T> T repeat(F0<T> toRun, F0<T> onFail, int count) {
        return repeat(toRun, onFail, count, 0, ALL_EXCEPTIONS_ALLOWED);
    }
    //endregion


    //region R
    @SuppressWarnings("SameReturnValue")
    default void repeat(
            R toRun,
            R onFail,
            int count,
            long sleepBetweenTries,
            Set<Class<? extends Throwable>> allowedExceptions
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
        repeat(toRun, null, count, 0, ALL_EXCEPTIONS_ALLOWED);
    }

    default void repeat(R toRun, int count, Set<Class<? extends Throwable>> allowedExceptions) {
        repeat(toRun, null, count, 0, allowedExceptions);
    }

    default void repeat(R toRun, int count, long sleepBetweenTries) {
        repeat(toRun, null, count, sleepBetweenTries, ALL_EXCEPTIONS_ALLOWED);
    }

    default void repeat(R toRun, R onFail, int count) {
        repeat(toRun, onFail, count, 0, ALL_EXCEPTIONS_ALLOWED);
    }
    //endregion


    //region F0E
    <T> T repeatE(
            F0E<T> toRun,
            F0<T> onFail,
            int count,
            long sleepBetweenTries,
            Set<Class<? extends Throwable>> allowedExceptions
    ) throws Exception;

    default <T> T repeatE(
            F0E<T> toRun, int count, long sleepBetweenTries, Set<Class<? extends Throwable>> allowedExceptions
    ) throws Exception {
        return repeatE(toRun, null, count, sleepBetweenTries, allowedExceptions);
    }

    default <T> T repeatE(F0E<T> toRun, int count, Set<Class<? extends Throwable>> allowedExceptions) throws Exception {
        return repeatE(toRun, null, count, 0, allowedExceptions);
    }

    default <T> T repeatE(F0E<T> toRun, int count) throws Exception {
        return repeatE(toRun, null, count, 0, ALL_EXCEPTIONS_ALLOWED);
    }

    default <T> T repeatE(F0E<T> toRun, int count, long sleepBetweenTries) throws Exception {
        return repeatE(toRun, null, count, sleepBetweenTries, ALL_EXCEPTIONS_ALLOWED);
    }

    default <T> T repeatE(F0E<T> toRun, F0<T> onFail, int count) throws Exception {
        return repeatE(toRun, onFail, count, 0, ALL_EXCEPTIONS_ALLOWED);
    }
    //endregion


    //region RE
    @SuppressWarnings("SameReturnValue")
    default void repeatE(
            RE toRun,
            R onFail,
            int count,
            long sleepBetweenTries,
            Set<Class<? extends Throwable>> allowedExceptions
    ) throws Exception {
        repeatE(
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

    default void repeatE(RE toRun, int count) throws Exception {
        repeatE(toRun, null, count, 0, ALL_EXCEPTIONS_ALLOWED);
    }

    default void repeatE(RE toRun, int count, Set<Class<? extends Throwable>> allowedExceptions) throws Exception {
        repeatE(toRun, null, count, 0, allowedExceptions);
    }

    default void repeatE(RE toRun, int count, long sleepBetweenTries) throws Exception {
        repeatE(toRun, null, count, sleepBetweenTries, ALL_EXCEPTIONS_ALLOWED);
    }

    default void repeatE(RE toRun, R onFail, int count) throws Exception {
        repeatE(toRun, onFail, count, 0, ALL_EXCEPTIONS_ALLOWED);
    }
    //endregion


    <ID, T, A extends IdCallable<ID, T>> BatchRepeatResult<ID, T, A> repeatAndReturnResults(
            List<A> tasks, int maxRetryCount, long sleepAfterFailMs, IExecutorService pool,
            Set<Class<? extends Throwable>> exceptRetries, CancelGetter cancel);

    default <ID, T, A extends IdCallable<ID, T>> BatchRepeatResult<ID, T, A> repeatAndReturnResults(
            List<A> tasks, int maxRetryCount, long sleepAfterFailMs, IExecutorService pool,
            Set<Class<? extends Throwable>> exceptRetries) {
        return repeatAndReturnResults(tasks, maxRetryCount, sleepAfterFailMs, pool, exceptRetries, () -> false);
    }
}
