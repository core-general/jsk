package sk.services.comparer.model;

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
import lombok.Getter;
import sk.utils.tuples.X2;

import java.util.Map;

@AllArgsConstructor
@Getter
public class MapCompareResult<X, Y> {
    Map<X, Y> notExistingIn2;
    Map<X, Y> notExistingIn1;

    Map<X, X2<Y, Y>> existButDifferent;

    public boolean hasDifferences() {
        return existButDifferent.size() > 0
                || notExistingIn2.size() > 0
                || notExistingIn1.size() > 0;
    }

    public String getShortInfo() {
        return String.format(
                "\nFirst has, second don't:%d\nSecond has, first don't:%d\nDiffer in both:%d",
                notExistingIn2.size(),
                notExistingIn1.size(),
                existButDifferent.size()
        );
    }
}
