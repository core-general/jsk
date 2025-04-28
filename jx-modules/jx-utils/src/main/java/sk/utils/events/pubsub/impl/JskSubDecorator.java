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

import lombok.Getter;
import lombok.SneakyThrows;
import sk.utils.async.locks.JLock;
import sk.utils.async.locks.JLockDecorator;
import sk.utils.events.pubsub.JskPubSubException;
import sk.utils.events.pubsub.JskSubListener;
import sk.utils.events.pubsub.JskSubscription;
import sk.utils.functional.C1;
import sk.utils.functional.F0;
import sk.utils.statics.Cc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Semaphore;

@Getter
class JskSubDecorator<T> implements JskSubListener<T>, JskSubscription {
    private final JskSubListener<T> subscriber;
    private final Semaphore semaphore;
    private final boolean failFast;
    private final C1<JskSubDecorator<T>> runIfUnsubscribed;
    private final F0<ZonedDateTime> now;
    private final List<Exception> exceptions = Cc.l();
    private final JLock exceptionLock = new JLockDecorator();

    private volatile boolean unsubscribed = false;

    public JskSubDecorator(JskSubListener<T> subscriber, Semaphore semaphore, boolean failFast,
            C1<JskSubDecorator<T>> runIfUnsubscribed, F0<ZonedDateTime> now) {
        this.subscriber = subscriber;
        this.semaphore = semaphore;
        this.failFast = failFast;
        this.runIfUnsubscribed = runIfUnsubscribed;
        this.now = now;
    }

    @Override
    public void onEvent(T event, ZonedDateTime zdt) {
        try {
            if (!unsubscribed) {
                subscriber.onEvent(event, zdt);
            }
        } catch (Exception e) {
            addException(e);
        }
    }

    @Override
    public void onFinish(ZonedDateTime date) {
        try {
            if (!unsubscribed) {
                try {
                    subscriber.onFinish(date);
                } catch (Exception e) {
                    addException(e);
                }
            }
        } finally {
            semaphore.release();
        }
    }

    @Override
    @SneakyThrows
    public void blockUntilFinished() throws JskPubSubException {
        semaphore.acquire();
        try {
            exceptionLock.lock();
            if (exceptions.size() > 0) {
                throw new JskPubSubException(exceptions);
            }
        } finally {
            exceptionLock.unlock();
        }
    }

    @Override
    public void removeSubscription() {
        unsubscribed = true;
    }

    @Override
    public String toString() {
        return subscriber.toString();
    }

    @Override
    public int hashCode() {
        return subscriber.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return subscriber.equals(obj);
    }

    private void addException(Exception e) {
        exceptionLock.runInLock(() -> {
            exceptions.add(e);
            if (failFast) {
                onFinish(now.apply());
            }
        });
    }
}
