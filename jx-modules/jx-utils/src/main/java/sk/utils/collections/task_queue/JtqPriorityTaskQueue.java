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

import sk.utils.collections.task_queue.model.JtqTaskFromQueueInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface JtqPriorityTaskQueue<PRIORITY extends JtqPTQPriority> {
    <OUT, TASK extends JtqPriorityTask<PRIORITY, OUT>> CompletableFuture<OUT> addTask(TASK task);

    List<JtqTaskFromQueueInfo<PRIORITY>> getProcessingNowTasks(boolean orderByAddDate/*otherwise order by id*/);

    List<JtqTaskFromQueueInfo<PRIORITY>> getTasksInQueueInfo(boolean orderByAddDate/*otherwise order by id*/);

    boolean removeTaskFromQueueById(String id);
}
