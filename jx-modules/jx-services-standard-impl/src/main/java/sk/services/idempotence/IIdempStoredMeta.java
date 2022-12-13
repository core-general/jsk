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
import lombok.NoArgsConstructor;
import sk.utils.functional.O;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class IIdempStoredMeta {
    boolean lockSign;
    char type;
    String requestHash;
    String meta;

    O<String> additionalData = O.empty();

    public static IIdempStoredMeta lock(String requestHash, O<String> additionalData) {
        return new IIdempStoredMeta(true, '0', requestHash, null, additionalData);
    }

    public IIdempStoredMeta result(char type, String requestHash, String meta) {
        this.type = type;
        this.requestHash = requestHash;
        this.meta = meta;

        lockSign = false;

        return this;
    }
}
