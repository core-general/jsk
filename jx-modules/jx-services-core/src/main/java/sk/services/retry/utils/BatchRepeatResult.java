package sk.services.retry.utils;

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

import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@AllArgsConstructor
public class BatchRepeatResult<ID, T, A extends IdCallable<ID, T>> {
    Map<ID, QueuedTask<ID, T, A>> result;

    public boolean isOk() {
        return result.values().stream().allMatch(ok());
    }

    public Map<ID, QueuedTask<ID, T, A>> getOkTasks() {
        return Collections.unmodifiableMap(result.entrySet().stream()
                .filter($ -> ok().test($.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public Map<ID, QueuedTask<ID, T, A>> getBadTasks() {
        return Collections.unmodifiableMap(result.entrySet().stream()
                .filter($ -> ok().negate().test($.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public Map<ID, QueuedTask<ID, T, A>> getAllTasks() {
        return Collections.unmodifiableMap(result);
    }

    private Predicate<QueuedTask<ID, T, A>> ok() {
        return ($) -> $.getResult() != null && $.getResult().isLeft();
    }
}
