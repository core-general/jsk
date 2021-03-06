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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import sk.utils.async.cancel.CallableWIthCancel;
import sk.utils.async.cancel.CancelGetter;

@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class IdCallableImpl<ID, V> implements IdCallable<ID, V> {
    final @Getter ID id;
    final CallableWIthCancel<V> callable;

    @Override
    public V call(CancelGetter cancel) throws Exception {
        return callable.call(cancel);
    }
}
