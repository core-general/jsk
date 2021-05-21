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
import sk.services.comparer.model.CompareResultDif;
import sk.services.comparer.model.MapCompareResult;
import sk.utils.statics.Cc;

import java.util.List;
import java.util.Map;

public class MapCompareTool<K, V>
        extends CollectionCompareTool<List<ToStringKvWrapper<K, V>>, ToStringKvWrapper<K, V>> {
    public static <X, Y> MapCompareResult<X, Y> compare(Map<X, Y> col1, Map<X, Y> col2) {
        final CompareResult<ToStringKvWrapper<X, Y>, Void> result =
                new MapCompareTool<X, Y>().innerCompare(mapTo(col1), mapTo(col2));

        return new MapCompareResult<>(
                mapFrom(result.getFirstDif()),
                mapFrom(result.getSecondDif()),
                result.getExistButDifferent().stream()
                        .map($ -> sk.utils.tuples.X.x($.i1().getVal(), sk.utils.tuples.X.x($.i1().getAdd(), $.i2().getAdd())))
                        .collect(Cc.toMX2())
        );
    }

    private static <X, Y> List<ToStringKvWrapper<X, Y>> mapTo(Map<X, Y> xes) {
        return xes.entrySet().stream().map((v) -> new ToStringKvWrapper<>(v.getKey(), v.getValue())).collect(Cc.toL());
    }

    private static <X, Y> Map<X, Y> mapFrom(CompareResultDif<ToStringKvWrapper<X, Y>, Void> xes) {
        return xes.getNotExistingInOther().stream().map($ -> sk.utils.tuples.X.x($.getVal(), $.getAdd())).collect(Cc.toMX2());
    }

}

