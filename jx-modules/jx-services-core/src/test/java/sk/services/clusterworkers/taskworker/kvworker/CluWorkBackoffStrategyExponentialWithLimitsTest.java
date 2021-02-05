package sk.services.clusterworkers.taskworker.kvworker;

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

import lombok.val;
import org.junit.Test;
import sk.utils.functional.O;
import sk.utils.ifaces.Identifiable;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.Assert.assertEquals;

public class CluWorkBackoffStrategyExponentialWithLimitsTest {

    @Test
    public void getWaitDurationForTask() {
        val task1 = getTask(0);
        val task2 = getTask(10);

        val ebof1 = new CluWorkBackoffStrategyExponentialWithLimits<>(10, O.empty(), O.empty());
        val ebof2 = new CluWorkBackoffStrategyExponentialWithLimits<>(10, O.of(Duration.of(20000, MILLIS)), O.empty());
        val ebof3 = new CluWorkBackoffStrategyExponentialWithLimits<>(10, O.empty(), O.of(Duration.of(5000, MILLIS)));
        val ebof4 = new CluWorkBackoffStrategyExponentialWithLimits<>(10, O.of(Duration.of(10000, MILLIS)),
                O.of(Duration.of(20000, MILLIS)));


        assertEquals(Duration.of(10, MILLIS), ebof1.getWaitDurationForTask(task1));
        assertEquals(Duration.of(20000, MILLIS), ebof2.getWaitDurationForTask(task1));
        assertEquals(Duration.of(10, MILLIS), ebof3.getWaitDurationForTask(task1));
        assertEquals(Duration.of(10000, MILLIS), ebof4.getWaitDurationForTask(task1));

        assertEquals(Duration.of(10240, MILLIS), ebof1.getWaitDurationForTask(task2));
        assertEquals(Duration.of(20000, MILLIS), ebof2.getWaitDurationForTask(task2));
        assertEquals(Duration.of(5000, MILLIS), ebof3.getWaitDurationForTask(task2));
        assertEquals(Duration.of(10240, MILLIS), ebof4.getWaitDurationForTask(task2));
    }

    private CluKvSplitWorkPartInfo<Identifiable<String>, Object> getTask(int tryCount) {
        final CluKvSplitWorkPartInfo<Identifiable<String>, Object> task = new CluKvSplitWorkPartInfo<>(() -> "x");
        task.setTryCount(tryCount);
        return task;
    }
}
