package sk.services.clusterworkers.taskworker.kvworker;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.taskworker.model.CluTaskBatchResult;
import sk.services.kv.IKvLimitedStore;
import sk.services.kv.IKvLocal4Test;
import sk.services.retry.utils.QueuedTask;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.ConverterImpl;
import sk.utils.functional.Gett;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.ifaces.Identifiable;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.fortest.Profiler;
import sk.utils.tuples.X;

import java.time.Duration;
import java.util.stream.Collectors;

import static sk.services.CoreServicesRaw.services;
import static sk.utils.asserts.JskAssert.checkEquals;

public class CluKvSplitTaskWorkerTest {
    @Spy IKvLimitedStore store = new IKvLocal4Test(services().json(), services().times());

    CluKvSplitTaskWorker<CluTestCustomMeta, CluTestTaskInput, CluTestResult,
            CluKvSplitTaskWorker.Config<CluTestCustomMeta, CluTestTaskInput, CluTestResult>>
            worker = new CluKvSplitTaskWorker<>("",
            services().async(), services().times(), services().ids(), services().json(),
            store);

    @BeforeEach
    public void before() {

    }

    @Test
    public void fullScenario() {
        Profiler.mark("start");
        worker.start(new CluKvSplitTaskWorker.Config<CluTestCustomMeta, CluTestTaskInput, CluTestResult>(
                CluDelay.fixed(1),
                1,
                System.out::println,
                this::mainProcessor,
                1,
                new ConverterImpl<>(String::new, String::getBytes),
                (tasks, cancelGetter) -> new CluTaskBatchResult<>(tasks.stream().map($ -> X.x($.getId(), new QueuedTask<>($)
                        .ok(Ex.toRuntime(() -> $.call(cancelGetter)))))
                        .collect(Cc.toMX2())),
                CluTestCustomMeta.class,
                1,
                CluTestTaskInput.class,
                1000000,
                CluTestResult.class,
                task -> Duration.ZERO
        ) {
            @Override
            public Gett<Boolean> getOnOffSwitchChecker() {
                return () -> true;
            }
        });
        worker.restart(O.of(new CluTestCustomMeta("NEW RUN")), Cc.l(new CluTestTaskInput("IN1"), new CluTestTaskInput("IN2")));

        String result = worker.getFullWorkInfoAfterFinish(50).join()
                .stream()
                .map($ -> $.i1().getId() + "_" + $.i2().left().getResult())
                .sorted()
                .collect(Collectors.joining("-"));

        checkEquals(result, "IN1_RES_IN1-IN2_RES_IN2");
    }

    private CluWorkChunkResult<CluTestResult> mainProcessor(CluTestCustomMeta meta,
            CluTestTaskInput taskInput, CancelGetter cancelGetter) {
        return new CluWorkChunkResult<>(taskInput.getId(), OneOf.left(new CluTestResult("RES_" + taskInput.getId())));
    }

    @Data
    @AllArgsConstructor
    public static class CluTestCustomMeta {
        String task;
    }

    @Data
    @AllArgsConstructor
    public static class CluTestTaskInput implements Identifiable<String> {
        final String id;
    }

    @Data
    @AllArgsConstructor
    public static class CluTestResult {
        String result;
    }
}
