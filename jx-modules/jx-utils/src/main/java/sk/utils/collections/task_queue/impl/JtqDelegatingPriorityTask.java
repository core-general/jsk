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

import lombok.Getter;
import lombok.SneakyThrows;
import sk.utils.collections.task_queue.JtqPTQPriority;
import sk.utils.collections.task_queue.JtqPriorityTask;
import sk.utils.functional.F1E;

@Getter
public class JtqDelegatingPriorityTask<PRIORITY extends JtqPTQPriority, OUT> implements JtqPriorityTask<PRIORITY, OUT> {
    private final String id;
    private final PRIORITY priority;
    private final F1E<JtqDelegatingPriorityTask<PRIORITY, OUT>, OUT> processor;

    public JtqDelegatingPriorityTask(String id, PRIORITY priority, F1E<JtqDelegatingPriorityTask<PRIORITY, OUT>, OUT> processor) {
        this.id = id;
        this.priority = priority;
        this.processor = processor;
    }

    @Override
    @SneakyThrows
    public OUT process() {
        return processor.apply(this);
    }
}
