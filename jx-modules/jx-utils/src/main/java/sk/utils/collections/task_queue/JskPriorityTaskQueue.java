package sk.utils.collections.task_queue;

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

import sk.utils.collections.task_queue.model.JskPTQTaskFromQueueInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface JskPriorityTaskQueue<PRIORITY extends JskPTQPriority> {
    <OUT, TASK extends JskPriorityTask<PRIORITY, OUT>> CompletableFuture<OUT> addTask(TASK task);

    List<JskPTQTaskFromQueueInfo<PRIORITY>> getProcessingNowTasks(boolean orderByAddDate/*otherwise order by id*/);

    List<JskPTQTaskFromQueueInfo<PRIORITY>> getTasksInQueueInfo(boolean orderByAddDate/*otherwise order by id*/);

    boolean removeTaskFromQueueById(String id);
}