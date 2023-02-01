package sk.services.async;

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

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import sk.utils.functional.O;
import sk.utils.functional.R;
import sk.utils.statics.Cc;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.*;

@Log4j2
public class AsyncImpl implements IAsync {
    private final ConcurrentMap<String, Long> nameIterator = new ConcurrentHashMap<>();

    private final IExecutorService f =
            new IExecutorServiceImpl(
                    newFixedThreadPool(getRuntime().availableProcessors(), r -> getDaemon(r::run, "FixedExecutor")));
    private final IExecutorService b = new IExecutorServiceImpl(newCachedThreadPool(r -> getDaemon(r::run, "CachedExecutor")));
    private final IExecutorService s =
            new IExecutorServiceImpl(newSingleThreadExecutor(r -> getDaemon(r::run, "SingleThreadExecutor")));
    private final IScheduledExecutorService sc = new IScheduledExecutorServiceImpl(newScheduledThreadPool(
            O.ofNullable(System.getProperty("AsyncImpl.scheduledPoolSize")).map(Integer::parseInt).orElse(5),
            r -> getDaemon(r::run, "ScheduledExecutor")));
    private final Map<Integer, IExecutorService> fixPerCore = new ConcurrentHashMap<>();
    private final Map<String, IScheduledExecutorService> dedicatedSchedulerExecutors = new ConcurrentHashMap<>();
    private final IExecutorService coldTaskFJP = new IExecutorServiceImpl(
            new ForkJoinPool(O.ofNullable(System.getProperty("AsyncImpl.coldTaskFJPSize")).map(Integer::parseInt).orElse(75)));

    @Override
    public IExecutorService fixedExec() {
        return f;
    }

    @SuppressWarnings("unused")
    @Override
    public IExecutorService fixedExec(int cores) {
        return fixPerCore
                .computeIfAbsent(cores, (i) -> new IExecutorServiceImpl(
                        newFixedThreadPool(cores, r -> getDaemon(r::run, "FixedPerCore-" + cores))));
    }

    @Override
    public IExecutorService bufExec() {
        return b;
    }

    @Override
    public IExecutorService singleExec() {
        return s;
    }

    @Override
    public IScheduledExecutorService scheduledExec() {
        return sc;
    }

    @Override
    public IExecutorService coldTaskFJP() {
        return coldTaskFJP;
    }

    @Override
    public IScheduledExecutorService newDedicatedScheduledExecutor(String name) {
        return Cc.compute(dedicatedSchedulerExecutors, name, (k, old) -> {
            log.debug("Replacing scheduled executor: " + name);
            old.getUnderlying().shutdownNow();
            return createScheduledExecutorService(name);
        }, () -> createScheduledExecutorService(name));
    }

    @NotNull
    private IScheduledExecutorService createScheduledExecutorService(String name) {
        return new IScheduledExecutorServiceImpl(
                newScheduledThreadPool(1, r -> getDaemon(r::run, "ScheduledDedicatedExecutor-" + name)));
    }

    private Thread getDaemon(R r, String name) {
        Thread t = defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        t.setName(name + "-" + Cc.compute(nameIterator, name, (k, v) -> v + 1, () -> 0L));
        return t;
    }

    @Override
    @PreDestroy
    public void stop() {
        f.getUnderlying().shutdownNow();
        b.getUnderlying().shutdownNow();
        s.getUnderlying().shutdownNow();
        sc.getUnderlying().shutdownNow();
        fixPerCore.values().forEach($ -> $.getUnderlying().shutdown());
        dedicatedSchedulerExecutors.values().forEach($ -> $.getUnderlying().shutdown());
    }
}
