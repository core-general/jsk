package sk.outer.graph.parser;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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
import lombok.EqualsAndHashCode;
import sk.utils.statics.Fu;

import java.util.List;

@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Data
public class MgcParsedData {
    String id;
    String type;
    List<String> params;
    String text;

    public boolean typeOrParamsContains(String contain, boolean strict) {
        return strict
               ? Fu.equal(type, contain) || params.stream().anyMatch($ -> Fu.equal($, contain))
               : type.contains(contain) || params.stream().anyMatch($ -> $.contains(contain));
    }
}