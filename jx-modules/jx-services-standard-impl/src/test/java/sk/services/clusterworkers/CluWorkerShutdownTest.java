package sk.services.clusterworkers;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
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

import org.junit.jupiter.api.Test;
import sk.services.async.AsyncImpl;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.model.CluMessage;
import sk.services.clusterworkers.model.CluState;
import sk.services.time.TimeUtcImpl;
import sk.utils.functional.O;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CluWorkerShutdownTest {
    @Test
    void stoppingAnIdleWorkerWakesItsMessageQueue() throws Exception {
        var async = new AsyncImpl();
        var thread = new AtomicReference<Thread>();
        var entered = new CountDownLatch(1);
        var worker = new ProbeWorker(async, () -> {
            thread.set(Thread.currentThread());
            entered.countDown();
        });
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (thread.get().getState() != Thread.State.TIMED_WAITING && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }
            assertEquals(Thread.State.TIMED_WAITING, thread.get().getState());
            assertTrue(worker.stop(3000).get(1, TimeUnit.SECONDS));
            thread.get().join(1000);
            assertFalse(thread.get().isAlive());
        } finally {
            async.stop();
        }
    }

    @Test
    void stoppingStillWaitsForTheCurrentMessageWithoutInterruptingIt() throws Exception {
        var async = new AsyncImpl();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var interrupted = new AtomicBoolean();
        var worker = new ProbeWorker(async, () -> {
            entered.countDown();
            try { release.await(); }
            catch (InterruptedException e) { interrupted.set(true); }
        });
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            var stopped = worker.stop(3000);
            assertThrows(TimeoutException.class, () -> stopped.get(50, TimeUnit.MILLISECONDS));
            release.countDown();
            assertTrue(stopped.get(1, TimeUnit.SECONDS));
            assertFalse(interrupted.get());
        } finally {
            release.countDown();
            async.stop();
        }
    }

    private enum State implements CluState<State> { RUNNING }
    private enum Message implements CluMessage { READY }

    private static class ProbeWorker extends CluWorker<State, Message> {
        ProbeWorker(AsyncImpl async, Runnable process) {
            super("shutdown-probe", State.RUNNING, async, new TimeUtcImpl());
            var sent = new AtomicBoolean();
            addScheduler("message", CluDelay.fixed(10), O.empty(),
                    () -> sent.getAndSet(true) ? O.empty() : O.of(Message.READY), true);
            start((message, state) -> process.run(), error -> fail(error));
        }
    }
}
