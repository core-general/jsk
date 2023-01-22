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
import sk.utils.collections.task_queue.JskPTQPriority;
import sk.utils.collections.task_queue.JskPriorityTask;
import sk.utils.collections.task_queue.JskPriorityTaskQueue;
import sk.utils.collections.task_queue.model.JskPTQTaskFromQueueInfo;
import sk.utils.functional.F0;
import sk.utils.statics.Fu;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.IntStream;

@Log4j2
@AllArgsConstructor
public class JskPriorityTaskQueueImpl<PRIORITY extends JskPTQPriority> implements JskPriorityTaskQueue<PRIORITY> {
    private final PriorityBlockingQueue<PTQTaskAndItsCompletion<?>> queue = new PriorityBlockingQueue<>();
    private final List<PTQExecutor> executors;
    private final F0<ZonedDateTime> nowProvider;
    private final String taskQueueName;

    public JskPriorityTaskQueueImpl(String taskQueueName, int executorCount, F0<ZonedDateTime> nowProvider) {
        this.taskQueueName = taskQueueName;
        executors = IntStream.range(0, executorCount)
                .mapToObj($ -> new PTQExecutor($ + ""))
                .toList();
        this.nowProvider = nowProvider;
    }

    @Override
    public <OUT, TASK extends JskPriorityTask<PRIORITY, OUT>>
    CompletableFuture<OUT> addTask(TASK task) {
        CompletableFuture<OUT> toRet = new CompletableFuture<>();
        queue.add(new PTQTaskAndItsCompletion<>(toRet, task, nowProvider.apply()));
        return toRet;
    }

    @Override
    public List<JskPTQTaskFromQueueInfo<PRIORITY>> getTasksInQueueInfo(boolean orderByAddDate/*otherwise order by id*/) {
        final Comparator<JskPTQTaskFromQueueInfo<PRIORITY>> comparator =
                orderByAddDate
                ? Comparator.comparing($ -> $.getAddedAt())
                : Comparator.comparing($ -> $.getPriority().ordinal());
        return queue.stream().map($ -> new JskPTQTaskFromQueueInfo<>(
                $.getTask().getId(),
                $.getAddedAt(),
                (PRIORITY) $.getTask().getPriority()
        )).sorted(comparator).toList();
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
                try {
                    output = taskAndCompletion.getTask().process();
                } catch (Exception e) {
                    taskAndCompletion.getOut().completeExceptionally(e);
                } finally {
                    taskAndCompletion.getOut().complete(output);
                }
            } catch (InterruptedException e) {
                //most probably we stop JVM
                log.info("Processing queue thread %s is interrupted".formatted(thread.getName()));
            }
        }
    }

    @Getter
    @AllArgsConstructor
    private class PTQTaskAndItsCompletion<OUT> implements Comparable<PTQTaskAndItsCompletion<?>> {
        CompletableFuture<OUT> out;
        JskPriorityTask<PRIORITY, OUT> task;
        ZonedDateTime addedAt;

        @Override
        public int compareTo(@NotNull PTQTaskAndItsCompletion<?> o) {
            return Fu.compare(this.task, o.task);
        }
    }
}
