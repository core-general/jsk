package sk.services.async;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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
import sk.utils.functional.F0;
import sk.utils.statics.Cc;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

public class AsyncSingleThreadedImpl implements IAsync {
    private final IExecutorServiceImpl iExecutorService = new IExecutorServiceImpl(new NoThreadingExecutorService());

    @Override
    public IExecutorService fixedExec() {
        return iExecutorService;
    }

    @Override
    public IExecutorService fixedExec(int threads) {
        return iExecutorService;
    }

    @Override
    public IExecutorService bufExec() {
        return iExecutorService;
    }

    @Override
    public IExecutorService singleExec() {
        return iExecutorService;
    }

    @Override
    public IScheduledExecutorService scheduledExec() {
        throw new NotImplementedException();
    }

    @Override
    public IExecutorService coldTaskFJP() {
        return iExecutorService;
    }

    @Override
    public IScheduledExecutorService newDedicatedScheduledExecutor(String name) {
        throw new NotImplementedException();
    }

    @Override
    public <T> T coldTaskFJPGet(int threads, F0<T> toRun) {
        return toRun.get();
    }

    @Override
    public void stop() {

    }

    private static class NoThreadingExecutorService extends AbstractExecutorService {
        @Override
        public void shutdown() {

        }


        @Override
        public List<Runnable> shutdownNow() {
            return Cc.l();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
