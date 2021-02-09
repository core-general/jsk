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

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.services.async.IExecutorService;
import sk.services.async.ISleep;
import sk.services.retry.utils.BatchRepeatResult;
import sk.services.retry.utils.IdCallable;
import sk.services.retry.utils.QueuedTask;
import sk.services.retry.utils.RetryOnAny;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.F0;
import sk.utils.functional.F0E;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class RepeatImpl implements IRepeat {
    protected @Inject ISleep sleep;

    @Override
    public <T> T repeat(@NotNull F0<T> toRun, @Nullable F0<T> onFail,
            int retryCount, long sleepBetweenTries,
            @NotNull Set<Class<? extends Throwable>> okExceptions) {

        RuntimeException exception = null;
        while (retryCount > 0) {
            try {
                return toRun.get();
            } catch (RuntimeException e) {
                if (exception == null) {
                    exception = e;
                    if (okExceptions.stream().noneMatch($ -> $.isAssignableFrom(e.getClass()))) {
                        return tryThrow(onFail, exception);
                    }
                }
                if (sleepBetweenTries > 0) {
                    sleep.sleep(sleepBetweenTries);
                }
            }
            retryCount--;
        }
        return tryThrow(onFail, Objects.requireNonNull(exception));
    }

    @Override
    public <T> T repeatE(@NotNull F0E<T> toRun, @Nullable F0<T> onFail, int retryCount, long sleepBetweenTries,
            @NotNull Set<Class<? extends Throwable>> okExceptions) throws Exception {

        Exception exception = null;
        while (retryCount > 0) {
            try {
                return toRun.get();
            } catch (Exception e) {
                if (exception == null) {
                    exception = e;
                    if (okExceptions.stream().noneMatch($ -> $.isAssignableFrom(e.getClass()))) {
                        return tryThrowE(onFail, exception);
                    }
                }
                if (sleepBetweenTries > 0) {
                    sleep.sleep(sleepBetweenTries);
                }
            }
            retryCount--;
        }
        return tryThrowE(onFail, Objects.requireNonNull(exception));
    }


    @SuppressWarnings("Convert2MethodRef")
    @Override
    public <ID, T, A extends IdCallable<ID, T>> BatchRepeatResult<ID, T, A> repeatAndReturnResults(List<A> tasks,
            int maxRetryCount, long sleepAfterFailMs, IExecutorService pool, Set<Class<? extends Throwable>> exceptRetries,
            CancelGetter cancel) {
        if (tasks.size() == 0) {
            return new BatchRepeatResult<>(Cc.m());
        }
        List<QueuedTask<ID, T, A>> finalTasks = tasks.stream().map((task) -> new QueuedTask<>(task)).collect(Cc.toL());
        BlockingQueue<QueuedTask<ID, T, A>> queue = new ArrayBlockingQueue<>(tasks.size(), false, finalTasks);

        while (true) {
            try {
                QueuedTask<ID, T, A> task = queue.take();
                if (task != QueuedTask.STOP) {
                    CompletableFuture.runAsync(() -> {
                        task.incTryCount();
                        try {
                            T call = task.getTask().call(cancel);
                            task.ok(call);
                        } catch (Exception e) {
                            if (e instanceof CancellationException) {
                                task.unknownExcept(e);
                            } else if (exceptRetries.stream().anyMatch($ -> $.isAssignableFrom(e.getClass())) &&
                                    task.getTryCount() > maxRetryCount || exceptRetries.contains(RetryOnAny.class)) {
                                task.repeatExcept(e);
                            } else if (exceptRetries.stream().noneMatch($ -> $.isAssignableFrom(e.getClass()))) {
                                task.unknownExcept(e);
                            } else {
                                sleep.sleep(sleepAfterFailMs);
                                queue.add(task);
                            }
                        } finally {
                            if (finalTasks.stream().allMatch($ -> $.getResult() != null)) {
                                //noinspection unchecked
                                queue.add(QueuedTask.STOP);
                            }
                        }
                    }, pool.getUnderlying());
                } else {
                    break;
                }
            } catch (InterruptedException e) {
                return Ex.thRow(e);
            }
        }

        return new BatchRepeatResult<>(finalTasks.stream().collect(Collectors.toMap($ -> $.getTask().getId(), $ -> $,
                (o, o2) -> o)));
    }

    private <T> T tryThrow(@Nullable Supplier<T> onFail, @NotNull RuntimeException exception) throws RuntimeException {
        return O.ofNullable(onFail).map(Supplier::get).orElseThrow(() -> exception);
    }

    private <T> T tryThrowE(@Nullable Supplier<T> onFail, @NotNull Exception exception) throws Exception {
        return O.ofNullable(onFail).map(Supplier::get).orElseThrow(() -> exception);
    }
}
