package sk.utils.collections.task_queue.impl;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.collections.task_queue.JtqPTQPriority;
import sk.utils.collections.task_queue.JtqPriorityTask;
import sk.utils.collections.task_queue.JtqPriorityTaskQueue;
import sk.utils.collections.task_queue.model.JtqTaskFromQueueInfo;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.statics.Fu;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.IntStream;

@Log4j2
@AllArgsConstructor
public class JtqPriorityTaskQueueImpl<PRIORITY extends JtqPTQPriority> implements JtqPriorityTaskQueue<PRIORITY> {
    private final PriorityBlockingQueue<PTQTaskAndItsCompletion<?>> queue = new PriorityBlockingQueue<>();
    private final ConcurrentMap<String, JtqTaskFromQueueInfo<PRIORITY>> inWork = new ConcurrentHashMap<>();

    private final List<PTQExecutor> executors;
    private final F0<ZonedDateTime> nowProvider;
    private final String taskQueueName;

    public JtqPriorityTaskQueueImpl(String taskQueueName, int executorCount, F0<ZonedDateTime> nowProvider) {
        this.taskQueueName = taskQueueName;
        executors = IntStream.range(0, executorCount)
                .mapToObj($ -> new PTQExecutor($ + ""))
                .toList();
        this.nowProvider = nowProvider;
    }

    @Override
    public <OUT, TASK extends JtqPriorityTask<PRIORITY, OUT>>
    CompletableFuture<OUT> addTask(TASK task) {
        CompletableFuture<OUT> toRet = new CompletableFuture<>();
        queue.add(new PTQTaskAndItsCompletion<>(toRet, task, nowProvider.apply()));
        return toRet;
    }

    @Override
    public List<JtqTaskFromQueueInfo<PRIORITY>> getProcessingNowTasks(boolean orderByAddDate) {
        return inWork.values().stream().sorted(getTaskListComparator(orderByAddDate)).toList();
    }

    @Override
    public List<JtqTaskFromQueueInfo<PRIORITY>> getTasksInQueueInfo(boolean orderByAddDate/*otherwise order by id*/) {
        return queue.stream().map($ -> $.getInfo()).sorted(getTaskListComparator(orderByAddDate)).toList();
    }


    @Override
    public boolean removeTaskFromQueueById(String taskId) {
        final String finalTaskId = taskId.trim();
        return queue.removeIf(task -> Fu.equal(task.getTask().getId().trim(), finalTaskId));
    }

    private class PTQExecutor {
        final ForeverThreadWithFinish thread;

        public PTQExecutor(String executorId) {
            thread = new ForeverThreadWithFinish(() -> execute(),
                    "JskPriorityTaskQueueExecutor_%s_%s".formatted(taskQueueName, executorId), true);
            thread.start();
        }

        private <OUT> void execute() {
            try {
                final PTQTaskAndItsCompletion<OUT> taskAndCompletion = (PTQTaskAndItsCompletion<OUT>) queue.take();
                OUT output = null;
                final JtqTaskFromQueueInfo<PRIORITY> info = taskAndCompletion.getInfo();
                try {
                    info.setWorkStartedAt(O.of(nowProvider.get()));
                    inWork.put(info.getTaskId(), info);
                    output = taskAndCompletion.getTask().process();
                } catch (Exception e) {
                    taskAndCompletion.getOut().completeExceptionally(e);
                } finally {
                    try {
                        taskAndCompletion.getOut().complete(output);
                    } finally {
                        inWork.remove(info.getTaskId());
                    }
                }
            } catch (InterruptedException e) {
                //most probably we stop JVM
                log.info("Processing queue thread %s is interrupted".formatted(thread.getName()));
            }
        }
    }

    private Comparator<JtqTaskFromQueueInfo<PRIORITY>> getTaskListComparator(boolean orderByAddToQueueDate) {
        final Comparator<JtqTaskFromQueueInfo<PRIORITY>> comparator =
                orderByAddToQueueDate
                ? Comparator.comparing($ -> $.getAddedToQueueAt())
                : Comparator.comparing($ -> $.getPriority().ordinal());
        return comparator;
    }

    @Getter
    @AllArgsConstructor
    private class PTQTaskAndItsCompletion<OUT> implements Comparable<PTQTaskAndItsCompletion<?>> {
        CompletableFuture<OUT> out;
        JtqPriorityTask<PRIORITY, OUT> task;
        ZonedDateTime addedAt;

        @Override
        public int compareTo(@NotNull PTQTaskAndItsCompletion<?> o) {
            return Fu.compare(this.task, o.task);
        }

        public JtqTaskFromQueueInfo<PRIORITY> getInfo() {
            return new JtqTaskFromQueueInfo<>(
                    this.getTask().getId(),
                    this.getAddedAt(),
                    (PRIORITY) this.getTask().getPriority()
            );
        }
    }
}
