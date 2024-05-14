package sk.services.retry;

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

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import sk.services.async.ISleep;
import sk.test.JskMockitoTest;
import sk.utils.statics.Cc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RepeatImplTest extends JskMockitoTest {
    @InjectMocks RepeatImpl repeat;
    @Mock ISleep sleep;

    @Test
    public void repeat() {
        AtomicInteger counter = new AtomicInteger();

        assertEquals("ok", repeat.repeat(() -> {
            return "ok";
        }, () -> {
            return "fail";
        }, 5, 0, Cc.s()));

        counter.set(0);
        assertEquals("fail", repeat.repeat(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new SomeException();
            }
            return "ok";
        }, () -> {
            return "fail";
        }, 5, 0, Cc.s()));

        counter.set(0);
        assertEquals("fail", repeat.repeat(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException();
            }
            return "ok";
        }, () -> {
            return "fail";
        }, 5, 0, Cc.s(SomeException.class)));

        counter.set(0);
        assertEquals("ok", repeat.repeat(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new SomeException();
            }
            return "ok";
        }, () -> {
            return "fail";
        }, 5, 0, Cc.s(RuntimeException.class)));

        counter.set(0);
        assertEquals("ok", repeat.repeat(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new SomeException();
            }
            return "ok";
        }, () -> {
            return "fail";
        }, 5, 0, Cc.s(SomeException.class)));

        counter.set(0);
        assertEquals("fail", repeat.repeat(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new SomeException();
            }
            return "ok";
        }, () -> {
            return "fail";
        }, 5, 0, Cc.s(SomeException2.class)));

        counter.set(0);
        assertEquals("ok", repeat.repeat(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new SomeException2();
            }
            return "ok";
        }, () -> {
            return "fail";
        }, 5, 0, Cc.s(SomeException.class)));

        counter.set(0);
        assertEquals("ok", repeat.repeat(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new SomeException2();
            }
            return "ok";
        }, () -> {
            return "fail";
        }, 5));
    }

    private static class SomeException extends RuntimeException {}

    private static class SomeException2 extends SomeException {}
}
