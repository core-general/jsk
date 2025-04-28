package sk.utils.events.pubsub.impl;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
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

import sk.utils.async.locks.JReadWriteLock;
import sk.utils.async.locks.JReadWriteLockDecorator;
import sk.utils.events.pubsub.JskPublisher;
import sk.utils.events.pubsub.JskSubListener;
import sk.utils.events.pubsub.JskSubscriber;
import sk.utils.events.pubsub.JskSubscription;
import sk.utils.functional.C1;
import sk.utils.functional.F0;
import sk.utils.functional.R;
import sk.utils.statics.Cc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Semaphore;


/**
 * @param <T>
 */
public class JskPubSubNoState<T> implements JskPublisher<T>, JskSubscriber<T> {
    private final C1<R> threadRunner;
    private final boolean waitUntilFirstSubscriber;
    protected final F0<ZonedDateTime> now;
    //not volatile intentionally because we write before unlock and access in locks
    private List<JskSubDecorator<T>> subscriberAction = Cc.l();
    private final JReadWriteLock workLock = new JReadWriteLockDecorator();
    private final JReadWriteLock startLock = new JReadWriteLockDecorator();
    //not volatile intentionally because we write and read in locks
    protected boolean finished = false;
    protected ZonedDateTime finishedDate;
    private boolean started = false;

    public JskPubSubNoState(C1<R> threadRunner, boolean waitUntilFirstSubscriber, F0<ZonedDateTime> now) {
        this.threadRunner = threadRunner;
        this.waitUntilFirstSubscriber = waitUntilFirstSubscriber;
        this.now = now;
        if (waitUntilFirstSubscriber) {
            workLock.writeLock().lock();//we lock everything until start is invoked
        }
    }

    protected ZonedDateTime onEventInternal(T event) {
        return now.apply();
    }

    protected void onSubscribeInternal(JskSubDecorator<T> tJskSubDecorator) {
        if (finished) {
            throw new RuntimeException("System is already finished");
        }
    }

    @Override
    public JskPublisher<T> publish(T event) {
        workLock.readLock().runInLock(() -> {
            if (!finished) {
                ZonedDateTime nnn = onEventInternal(event);
                subscriberAction.forEach($ -> $.onEvent(event, nnn));
            } else {
                throw new RuntimeException("System is already finished");
            }
        });
        return this;
    }

    @Override
    public JskPublisher<T> finish() {
        workLock.writeLock().runInLock(() -> {
            finished = true;
            finishedDate = now.apply();
            subscriberAction.forEach($ -> {
                $.onFinish(finishedDate);
                $.getSemaphore().release();
            });

        });
        return this;
    }

    @Override
    public JskSubscription subscribe(JskSubListener<T> consumer, boolean failFast) {
        return startLock.writeLock().getInLock(() -> {
            Semaphore sema = new Semaphore(1);
            sema.acquireUninterruptibly();

            JskSubDecorator<T> tJskSubDecorator =
                    new JskSubDecorator<>(consumer, sema, failFast,
                            dec -> threadRunner.accept(() -> workLock.writeLock().runInLock(() -> {
                                subscriberAction.remove(dec);
                                sema.release();
                            })), now);

            if (!started && waitUntilFirstSubscriber) {
                started = true;
                subscriberAction.add(tJskSubDecorator);
                workLock.writeLock().unlock();//when started - we unlock everything
            } else {
                workLock.writeLock().runInLock(() -> {
                    onSubscribeInternal(tJskSubDecorator);
                    if (!finished) {
                        subscriberAction.add(tJskSubDecorator);
                    }
                });
            }
            return tJskSubDecorator;
        });
    }
}
