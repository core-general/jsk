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
public class IdempLockResult<META> {
    /**
     * left is either value or in case of wrong requestHash void, right is either lock success if true, or lock failed (lock is
     * hold by someone else) if false
     */
    OneOf<O<IdempValue<META>>, Boolean> valueOrLockSuccessStatus;

    public static <META> IdempLockResult<META> cachedValue(IdempValue<META> val) {
        return new IdempLockResult<>(left(O.of(val)));
    }

    public static <META> IdempLockResult<META> badParams() {
        return new IdempLockResult<>(left(O.empty()));
    }

    public static <META> IdempLockResult<META> lockOk() {
        return new IdempLockResult<>(right(true));
    }

    public static <META> IdempLockResult<META> lockBad() {
        return new IdempLockResult<>(right(false));
    }
}
