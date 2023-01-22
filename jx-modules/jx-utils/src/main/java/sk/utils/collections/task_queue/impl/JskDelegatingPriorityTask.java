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
import sk.utils.collections.task_queue.JskPTQPriority;
import sk.utils.collections.task_queue.JskPriorityTask;
import sk.utils.functional.F1;

@Getter
public class JskDelegatingPriorityTask<PRIORITY extends JskPTQPriority, OUT> implements JskPriorityTask<PRIORITY, OUT> {
    private final PRIORITY priority;
    private final F1<JskDelegatingPriorityTask<PRIORITY, OUT>, OUT> processor;

    public JskDelegatingPriorityTask(PRIORITY priority, F1<JskDelegatingPriorityTask<PRIORITY, OUT>, OUT> processor) {
        this.priority = priority;
        this.processor = processor;
    }

    @Override
    public OUT process() {
        return processor.apply(this);
    }
}
