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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import sk.utils.ifaces.Identifiable;
import sk.utils.tuples.X2;

import java.util.List;

@Data
@AllArgsConstructor
public class CompareResult<T extends Identifiable<String>, I> {
    @Getter(AccessLevel.PRIVATE) CompareResultDif<T, I> firstDif;
    @Getter(AccessLevel.PRIVATE) CompareResultDif<T, I> secondDif;
    List<X2<T, T>> existButDifferent;


    public CompareResultDif<T, I> getIn1NotIn2() {
        return firstDif;
    }

    public CompareResultDif<T, I> getIn2NotIn1() {
        return secondDif;
    }

    public boolean hasDifferences() {
        return existButDifferent.size() > 0
                || firstDif.getNotExistingInOther().size() > 0
                || secondDif.getNotExistingInOther().size() > 0;
    }

    public String getShortInfo() {
        return String.format(
                "\nFirst has, second don't:%d\nSecond has, first don't:%d\nDiffer in both:%d",
                firstDif.getNotExistingInOther().size(),
                secondDif.getNotExistingInOther().size(),
                existButDifferent.size()
        );
    }
}
