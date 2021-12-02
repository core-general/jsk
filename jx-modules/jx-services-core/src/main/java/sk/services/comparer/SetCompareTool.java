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
import lombok.NoArgsConstructor;
import sk.services.comparer.model.CompareResult;
import sk.services.comparer.model.CompareResultDif;
import sk.services.comparer.model.SetCompareResult;
import sk.utils.ifaces.Identifiable;
import sk.utils.statics.Cc;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SetCompareTool<A extends Identifiable<String>> extends CollectionCompareTool<List<A>, A> {
    public static <X> SetCompareResult<X> compare(Set<X> col1, Set<X> col2) {
        final CompareResult<ToStringWrapper<X>, Void> result =
                new SetCompareTool<ToStringWrapper<X>>().innerCompare(setTo(col1), setTo(col2));

        return new SetCompareResult<>(
                setFrom(result.getIn1NotIn2()),
                setFrom(result.getIn2NotIn1())
        );
    }

    private static <X> List<ToStringWrapper<X>> setTo(Set<X> xes) {
        return xes.stream().map(ToStringWrapper::new).collect(Cc.toL());
    }

    private static <X> List<X> setFrom(CompareResultDif<ToStringWrapper<X>, Void> xes) {
        return xes.getNotExistingInOther().stream().map($ -> $.val).collect(Collectors.toList());
    }
}
