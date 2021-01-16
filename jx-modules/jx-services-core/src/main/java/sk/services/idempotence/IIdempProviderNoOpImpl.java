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

import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;

import java.time.Duration;

public class IIdempProviderNoOpImpl implements IIdempProvider {

    public static final IdempLockResult<?> NO_OP_LOCK = IdempLockResult.lockOk();

    @Override
    public <META> IdempLockResult<META> tryLock(String key, String requestHash, TypeWrap<META> meta, Duration lockDuration,
            O<String> additionalData4Lock) {
        return (IdempLockResult<META>) NO_OP_LOCK;
    }

    @Override
    public <META> void cacheValue(String key, String requestHash, IdempValue<META> valueToCache, Duration cacheDuration) { }

    @Override
    public void unlockOrClear(String key) { }
}
