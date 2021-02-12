package sk.services.clusterworkers.taskworker;

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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KINDither express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import sk.services.async.IAsync;
import sk.services.clusterworkers.CluOnOffWorker;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.taskworker.kvworker.CluWorkChunkResult;
import sk.services.clusterworkers.taskworker.model.CluSplitTask;
import sk.services.clusterworkers.taskworker.model.CluTaskBatchResult;
import sk.services.retry.utils.IdCallable;
import sk.services.retry.utils.IdCallableImpl;
import sk.services.time.ITime;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.C1;
import sk.utils.functional.Gett;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class CluSplitTaskWorker<CUSTOM_META, RESULT, CONFIG extends CluSplitTaskWorker.IConf<CUSTOM_META, RESULT>>
        extends CluOnOffWorker<CONFIG> {
    public CluSplitTaskWorker(String workerName) {
        super(workerName);
    }

    public CluSplitTaskWorker(String workerName, IAsync async, ITime times) {
        super(workerName, async, times);
    }

    @Override
    public synchronized void start(CONFIG config) throws RuntimeException {
        super.start(config);
    }

    public interface IConf<CUSTOM_META, RESULT> extends CluOnOffWorker.IConf {
        Gett<CluSplitTask<CUSTOM_META, RESULT>> getSplitTask();

        default void preRun(CluSplitTask<CUSTOM_META, RESULT> task) {
            //does nothing, but could be overridden
        }

        default C1<CancelGetter> getMainTaskRunner() {
            return cancel -> {
                try {
                    //TestProf.mark("start");

                    CluSplitTask<CUSTOM_META, RESULT> tasks = getSplitTask().get();
                    if (tasks.getTasksToProcess().isEmpty()) {
                        tasks.finishTasks(O.empty());
                    }

                    preRun(tasks);

                    //TestProf.mark("prepare");

                    List<IdCallable<String, CluWorkChunkResult<RESULT>>> toExecute = tasks.getTasksToProcess().stream()
                            .flatMap(Collection::stream)
                            .map($ -> new IdCallableImpl<>($.getId(), (cc) -> {
                                cc.throwIfCancelled();
                                return $.getExecutor().apply(cc);
                            }))
                            .collect(Collectors.toList());

                    O<CluTaskBatchResult<RESULT>> res = tasks.getExecutor().map($ -> $.apply(toExecute, cancel));

                    //TestProf.mark("done");

                    List<CluWorkChunkResult<RESULT>> results = res.stream().flatMap($ -> $.getAllTasks().values().stream())
                            .map($ -> $.getResult()
                                    .collect(r -> r, r -> new CluWorkChunkResult<RESULT>($.getTask().getId(), OneOf.right(r))))
                            .collect(Collectors.toList());

                    tasks.finishTasks(O.of(results));

                    //TestProf.finishMark("finish");

                    //TestProf.printInfo();
                } catch (Exception e) {
                    log.error("", e);
                    throw new RuntimeException(e);
                }
            };
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Config<CUSTOM_META, RESULT> implements IConf<CUSTOM_META, RESULT> {

        Gett<CluSplitTask<CUSTOM_META, RESULT>> splitTask;
        CluDelay mainTaskDelay;

        long onOffCheckPeriod;
        Gett<Boolean> onOffSwitchChecker;

        C1<Throwable> errorConsumer;
    }
}
