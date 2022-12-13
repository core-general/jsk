package sk.utils.async;

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

import lombok.extern.log4j.Log4j2;
import sk.utils.async.cancel.CancelGSetter;
import sk.utils.functional.C1;
import sk.utils.functional.C2;
import sk.utils.functional.R;
import sk.utils.statics.Fu;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Log4j2
public class ForeverThreadWithFinish extends Thread {
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final Semaphore finishSemaphore = new Semaphore(1, true);

    protected final C1<CancelGSetter> foreverTarget;
    protected final C2<Throwable, ForeverThreadWithFinish> onError;

    public ForeverThreadWithFinish(R target, boolean daemon) {
        this(ct -> target.run(), daemon);
    }

    public ForeverThreadWithFinish(R target, String name, boolean daemon) {
        this(ct -> target.run(), name, daemon);
    }

    public ForeverThreadWithFinish(C1<CancelGSetter> target, boolean daemon) {
        this(target, daemon, (throwable, foreverThreadWithFinish) -> IAsyncUtil.processErrorOnDefaultHandler(false, throwable));
    }

    public ForeverThreadWithFinish(C1<CancelGSetter> target, String name, boolean daemon) {
        this(target, name, daemon,
                (throwable, foreverThreadWithFinish) -> IAsyncUtil.processErrorOnDefaultHandler(false, throwable));
    }

    public ForeverThreadWithFinish(R target, boolean daemon, C2<Throwable, ForeverThreadWithFinish> onError) {
        this(ct -> target.run(), daemon, onError);
    }

    public ForeverThreadWithFinish(R target, String name, boolean daemon, C2<Throwable, ForeverThreadWithFinish> onError) {
        this(ct -> target.run(), name, daemon, onError);
    }

    public ForeverThreadWithFinish(C1<CancelGSetter> target, boolean daemon, C2<Throwable, ForeverThreadWithFinish> onError) {
        super((Runnable) null);
        setDaemon(daemon);
        foreverTarget = target;
        this.onError = onError;
    }

    public ForeverThreadWithFinish(C1<CancelGSetter> target, String name, boolean daemon,
            C2<Throwable, ForeverThreadWithFinish> onError) {
        super((Runnable) null, name);
        setDaemon(daemon);
        this.foreverTarget = target;
        this.onError = onError;
    }

    @Override
    public final void run() {
        final CancelGSetter cancellationToken = new CancelGSetter() {
            @Override
            public boolean isCancelled() {
                return finished.get();
            }

            @Override
            public void setCancelled(boolean cancelled) {
                if (cancelled) {
                    finishThread();
                }
            }
        };

        Fu.run4ever(() -> {
            try {
                finishSemaphore.acquire();
                foreverTarget.accept(cancellationToken);
            } finally {
                finishSemaphore.release();
            }
        }, throwable -> onError.accept(throwable, this), finished::get).run();
    }

    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public CompletableFuture<Boolean> finishThread() {
        return finishThread(Long.MAX_VALUE);
    }

    public CompletableFuture<Boolean> finishThread(long timeout) {
        finished.set(true);
        return CompletableFuture.supplyAsync(() -> {
            boolean locked = false;
            try {
                locked = finishSemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
                completed.set(true);
                return locked;
            } catch (Exception e) {
                return false;
            } finally {
                if (locked) {finishSemaphore.release();}
            }
        }, target -> new Thread(target).start());
    }

    public boolean isFinished() {
        return completed.get();
    }
}
