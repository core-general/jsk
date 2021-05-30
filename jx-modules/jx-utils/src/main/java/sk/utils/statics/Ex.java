package sk.utils.statics;

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

import sk.exceptions.NotImplementedException;
import sk.utils.functional.F0E;
import sk.utils.functional.RE;
import sk.utils.javafixes.BuilderStringWriter;

import java.io.PrintWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class Ex {
    public static boolean isInstanceOf(Throwable e, Class<? extends Throwable>... toTest) {
        if (e instanceof UndeclaredThrowableException) {
            e = ((UndeclaredThrowableException) e).getUndeclaredThrowable();
        }
        final Class<? extends Throwable> classOfSource = e.getClass();
        for (Class<? extends Throwable> exceptionClass : toTest) {
            if (exceptionClass.isAssignableFrom(classOfSource)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T notImplemented() {
        throw new NotImplementedException();
    }

    public static <T> T toRuntime(F0E<T> toRun) {
        try {
            return toRun.apply();
        } catch (Exception e) {
            return Ex.thRow(e);
        }
    }

    public static void toRuntime(RE toRun) {
        try {
            toRun.run();
        } catch (Exception e) {
            Ex.thRow(e);
        }
    }

    public static <T> T thRow(Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new RuntimeException(e);
        }
    }

    public static <T> T thRow(String message) {
        throw new RuntimeException(message);
    }

    public static <T> T thRow() {
        throw new RuntimeException();
    }

    public static String traceAsString(Throwable e) {
        BuilderStringWriter sw = new BuilderStringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String getInfo(Throwable e) {
        return e.getMessage() + "\n" + traceAsString(e);
    }

    public static String getInfo(Throwable e, int maxSymbols) {
        return St.raze3dots(e.getMessage() + "\n" + traceAsString(e), maxSymbols);
    }

    public static <T> T get(Callable<T> toCall) {
        try {
            return toCall.call();
        } catch (Exception e) {
            return thRow(e);
        }
    }

    public static <T> T getIgnore(Callable<T> toCall) {
        try {
            return toCall.call();
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> void run(RE s) {
        try {
            s.run();
        } catch (Exception e) {
            thRow(e);
        }
    }

    public static <T> void runIgnore(RE s) {
        try {
            s.run();
        } catch (Exception ignored) {

        }
    }

    public static void throwIf(boolean b, String desc) {
        if (b) {
            Ex.thRow(desc);
        }
    }
}
