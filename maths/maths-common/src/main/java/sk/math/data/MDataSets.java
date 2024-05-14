package sk.math.data;

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
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;


@AllArgsConstructor
public class MDataSets {
    @Setter private String name;
    private String[] nameX;
    private String nameY;
    @Getter List<MDataSet> datasets;

    public MDataSets(List<MDataSet> datasets) {
        this.datasets = datasets;
    }

    public String getPlotName() {
        return Objects.requireNonNullElseGet(name, () -> datasets.stream().map(MDataSet::getName).findFirst().orElse("EMPTY"));
    }

    public String getYName() {
        return Objects.requireNonNullElseGet(nameY, () -> datasets.stream().map(MDataSet::getNameY).findFirst().orElse("EMPTY"));
    }

    public String[] getXName() {
        return Objects.requireNonNullElseGet(nameX,
                () -> datasets.stream().map(MDataSet::getNameX).findAny().orElse(new String[]{"EMPTY"}));
    }
}
