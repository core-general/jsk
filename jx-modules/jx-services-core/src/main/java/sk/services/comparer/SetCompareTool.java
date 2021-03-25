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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import sk.services.comparer.model.CompareResult;
import sk.services.comparer.model.CompareResultDif;
import sk.services.comparer.model.SetCompareResult;
import sk.utils.functional.F1;
import sk.utils.ifaces.Identifiable;
import sk.utils.statics.Cc;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SetCompareTool<A extends Identifiable<String>> extends CompareTool<A, Void> {
    public static <X> SetCompareResult<X> compare(Set<X> col1, Set<X> col2) {
        F1<Set<X>, Set<ToStringWrapper<X>>> to = xes -> xes.stream().map(ToStringWrapper::new).collect(Cc.toS());
        F1<CompareResultDif<ToStringWrapper<X>, Void>, List<X>> from =
                xes -> xes.getNotExistingInOther().stream().map($ -> $.val).collect(Collectors.toList());

        final CompareResult<ToStringWrapper<X>, Void> result =
                new SetCompareTool<ToStringWrapper<X>>().innerCompare(to.apply(col1), to.apply(col2));

        return new SetCompareResult<>(
                from.apply(result.getFirstDif()),
                from.apply(result.getSecondDif())
        );
    }

    private CompareResult<A, Void> innerCompare(Set<A> col1, Set<A> col2) {
        F1<Set<A>, List<CompareItem<A>>> converter = col -> col.stream().map($ -> new CompareItem<A>() {
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

    @AllArgsConstructor
    private static class ToStringWrapper<X> implements Identifiable<String> {
        X val;

        @Override
        public String getId() {
            return val.toString();
        }
    }
}
