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

import sk.utils.functional.C1;
import sk.utils.functional.F0;
import sk.utils.functional.R;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.time.ZonedDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @param <T>
 */
public class JskPubSubQueue<T> extends JskPubSubNoState<T> {
    private final Queue<X2<T, ZonedDateTime>> events = new ConcurrentLinkedQueue<>();

    public JskPubSubQueue(C1<R> threadRunner, F0<ZonedDateTime> now) {
        super(threadRunner, false, now);
    }

    @Override
    protected ZonedDateTime onEventInternal(T event) {
        ZonedDateTime nnn = now.get();
        events.add(X.x(event, nnn));
        return nnn;
    }

    @Override
    protected void onSubscribeInternal(JskSubDecorator<T> subscriber) {
        events.forEach($ -> subscriber.onEvent($.i1(), $.i2()));
        if (finished) {
            subscriber.onFinish(finishedDate);
            subscriber.getSemaphore().release();
        }
    }
}
