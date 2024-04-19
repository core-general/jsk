package sk.utils.async;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
public class IAsyncUtil {
    public static void throwableProcessing(Runnable command, boolean throwHere) {
        try {
            command.run();
        } catch (Throwable oom) {
            processErrorOnDefaultHandler(throwHere, oom);
        }
    }

    public static <T> T throwableProcessing(Callable<T> task, boolean throwHere) {
        try {
            return task.call();
        } catch (Throwable oom) {
            processErrorOnDefaultHandler(throwHere, oom);
        }
        return null;
    }

    public static void processErrorOnDefaultHandler(boolean throwHere, Throwable oom) {
        final Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        if (handler != null) {
            try {
                handler.uncaughtException(Thread.currentThread(), oom);
            } catch (Throwable e) {
                log.error("", e);
            }
        }

        if (throwHere) {
            if (oom instanceof RuntimeException) {
                throw (RuntimeException) oom;
            }
            if (oom instanceof Error) {
                throw (Error) oom;
            }
            throw new RuntimeException("", oom);
        }
    }

    public static <T> Collection<? extends Callable<T>> toThrowableTasks(Collection<? extends Callable<T>> tasks,
            boolean throwHere) {
        return tasks.stream().map(callable -> (Callable<T>) () -> throwableProcessing(callable, throwHere))
                .collect(Collectors.toList());
    }
}
