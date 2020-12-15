package sk.services.idempotence;

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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;

import static sk.utils.functional.OneOf.left;
import static sk.utils.functional.OneOf.right;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IdempotenceLockResult<META> {
    /**
     * left is either value or in case of wrong requestHash void, right is either lock success if true, or lock failed (lock is
     * hold by someone else) if false
     */
    OneOf<O<IdempotentValue<META>>, Boolean> valueOrLockSuccessStatus;

    public static <META> IdempotenceLockResult<META> cachedValue(IdempotentValue<META> val) {
        return new IdempotenceLockResult<>(left(O.of(val)));
    }

    public static <META> IdempotenceLockResult<META> badParams() {
        return new IdempotenceLockResult<>(left(O.empty()));
    }

    public static <META> IdempotenceLockResult<META> lockOk() {
        return new IdempotenceLockResult<>(right(true));
    }

    public static <META> IdempotenceLockResult<META> lockBad() {
        return new IdempotenceLockResult<>(right(false));
    }
}
