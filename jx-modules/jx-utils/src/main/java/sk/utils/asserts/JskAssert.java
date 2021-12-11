package sk.utils.asserts;

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

import sk.exceptions.JskProblemException;
import sk.utils.functional.R;
import sk.utils.statics.Fu;
import sk.utils.statics.Ti;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class JskAssert {
    public static <T> void checkEquals(T expected, T obtained, String errorMessage) throws JskAssertException {
        if (!Fu.equal(expected, obtained)) {
            throw new JskAssertException(errorMessage);
        }
    }

    public static <T> void checkEquals(T expected, T obtained) throws JskAssertException {
        checkEquals(expected, obtained, "\n" + Objects.toString(expected) + " != \n" + Objects.toString(obtained));
    }

    public static <T> void checkNotEquals(T expected, T obtained, String errorMessage) throws JskAssertException {
        if (Fu.equal(expected, obtained)) {
            throw new JskAssertException(errorMessage);
        }
    }

    public static <T> void checkNotEquals(T expected, T obtained) throws JskAssertException {
        checkNotEquals(expected, obtained, "\n" + Objects.toString(expected) + " == \n" + Objects.toString(obtained));
    }

    public static <T> void checkGreaterThen(Number expected, Number obtained, String errorMessage)
            throws JskAssertException {
        if (!(obtained.doubleValue() > expected.doubleValue())) {
            throw new JskAssertException(errorMessage);
        }
    }

    public static <T> void checkGreaterThen(Number expected, Number obtained) throws JskAssertException {
        checkGreaterThen(expected, obtained, obtained + "!>" + expected);

    }

    public static <T> void checkNull(T obtained) throws JskAssertException {
        checkEquals(null, obtained);
    }

    public static void checkNullAll(Object... values) throws JskAssertException {
        Arrays.stream(values).forEach($ -> checkNull($));
    }

    public static <T> void checkNotNull(T obtained) throws JskAssertException {
        checkNotEquals(null, obtained);
    }

    public static <T> void checkNull(T obtained, String errorMessage) throws JskAssertException {
        checkEquals(null, obtained, errorMessage);
    }

    public static <T> void checkNotNull(T obtained, String errorMessage) throws JskAssertException {
        checkNotEquals(null, obtained, errorMessage);
    }

    public static void checkFail(String message) throws JskAssertException {
        throw new JskAssertException(message);
    }

    public static void checkFail() throws JskAssertException {
        throw new JskAssertException();
    }

    public static void checkFalse(Boolean obtained, String message) throws JskAssertException {
        checkEquals(false, obtained, message);
    }

    public static void checkFalse(Boolean obtained) throws JskAssertException {
        checkEquals(false, obtained);
    }

    public static void checkTrue(Boolean obtained, String message) throws JskAssertException {
        checkEquals(true, obtained, message);
    }

    public static void checkTrue(Boolean obtained) throws JskAssertException {
        checkEquals(true, obtained);
    }

    public static void checkAndWaitForOk(int maxWaitSeconds, R toRun) {
        JskAssertException lastException = null;
        final long started = System.currentTimeMillis() / 1000;
        while (System.currentTimeMillis() / 1000 - started < maxWaitSeconds) {
            try {
                toRun.run();
                return;
            } catch (JskAssertException e) {
                lastException = e;
            } finally {
                Ti.sleep(500);
            }
        }
        if (lastException != null) {
            throw lastException;
        }
    }

    public static void checkCatchOrFail(Runnable r, String errorMessage, String compareWithServerError) {
        checkCatchOrFail(r, errorMessage, (Consumer<? super Exception>) e -> {
            final JskProblemException e1 = (JskProblemException) e;
            if (!Fu.equal(e1.getProblem().getCode(), compareWithServerError)) {
                checkFail("\n" + errorMessage + "\n" + e1.getProblem().getCode() + "!=" + compareWithServerError);
            }
        });
    }

    public static <T extends Enum<T>> void checkCatchOrFail(Runnable r, String errorMessage, T compareWithServerError) {
        checkCatchOrFail(r, errorMessage, (Consumer<? super Exception>) e -> {
            final JskProblemException e1 = (JskProblemException) e;
            if (!Fu.equal(e1.getProblem().getCode(), compareWithServerError.name())) {
                checkFail("\n" + errorMessage + "\n" + e1.getProblem().getCode() + "!=" + compareWithServerError);
            }
        });
    }

    public static void checkCatchOrFail(Runnable r, String errorMessage) {
        checkCatchOrFail(r, errorMessage, (Consumer<? super Exception>) e -> {
        });
    }

    public static void checkCatchOrFail(Runnable r) {
        checkCatchOrFail(r, e -> {
        });
    }

    public static void checkCatchOrFail(Runnable r, String errorMessage, Consumer<? super Exception> consumeException) {
        try {
            r.run();
        } catch (Exception e) {
            consumeException.accept(e);
            return;
        }
        checkFail(errorMessage);
    }

    public static void checkCatchOrFail(Runnable r, Consumer<? super Exception> consumeException) {
        try {
            r.run();
        } catch (Exception e) {
            consumeException.accept(e);
            return;
        }
        checkFail();
    }
}
