package sk.services.http;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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
import lombok.Data;
import sk.utils.javafixes.BadCharReplacer;
import sk.utils.statics.St;

@Data
@AllArgsConstructor
public class EtagAndSize {
    final static BadCharReplacer replacer = BadCharReplacer.hashSetReplacer(St.engENGDig);

    String etag;
    long size;

    @Override
    public String toString() {
        return toString("-");
    }

    public String toString(String specialChar) {
        return replacer.replaceChars(etag, specialChar) + specialChar + size;
    }
}
