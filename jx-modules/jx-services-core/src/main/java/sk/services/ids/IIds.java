package sk.services.ids;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@SuppressWarnings("unused")
public interface IIds {
    UUID shortId();

    default String shortIdS() {
        return shortId().toString();
    }

    String customId(int length);

    UUID text2Uuid(String val);

    default String unique(String val) {
        return unique(val.getBytes(StandardCharsets.UTF_8), 8, false);
    }

    default String unique(String val, int iterations) {
        return unique(val.getBytes(StandardCharsets.UTF_8), iterations, false);
    }

    String unique(byte[] val, int iterations, boolean valIsCloned);
}
