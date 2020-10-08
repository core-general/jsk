package sk.services.async;

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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public interface IExecutorService {
    public ExecutorService getUnderlying();

    default <T> Future<T> call(Callable<T> task) {
        return getUnderlying().submit(task);
    }

    default <T> Future<T> submit(Runnable task, T result) {
        return getUnderlying().submit(task, result);
    }

    default Future<?> submit(Runnable task) {
        return getUnderlying().submit(task);
    }

    default void execute(Runnable command) {
        getUnderlying().execute(command);
    }
}
