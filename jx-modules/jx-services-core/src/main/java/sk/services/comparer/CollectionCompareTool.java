package sk.services.comparer;

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

import sk.services.comparer.model.CompareResult;
import sk.utils.functional.F1;
import sk.utils.ifaces.Identifiable;
import sk.utils.statics.Cc;

import java.util.Collection;
import java.util.List;

public class CollectionCompareTool<C extends Collection<A>, A extends Identifiable<String>> extends CompareTool<A, Void> {
    protected CompareResult<A, Void> innerCompare(C col1, C col2) {
        F1<C, List<CompareItem<A>>> converter = col -> col.stream().map($ -> new CompareItem<A>() {
            @Override
            public String getId() {
                return $.getId();
            }

            @Override
            public A getItemInfo() {
                return $;
            }
        }).collect(Cc.toL());

        return this.innerCompare(
                () -> converter.apply(col1),
                () -> converter.apply(col2),
                as -> null
        );
    }

}
