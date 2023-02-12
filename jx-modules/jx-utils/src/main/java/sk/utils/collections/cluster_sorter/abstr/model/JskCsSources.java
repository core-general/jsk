package sk.utils.collections.cluster_sorter.abstr.model;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

import lombok.Getter;
import sk.utils.collections.cluster_sorter.abstr.JskCsSource;
import sk.utils.statics.Cc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JskCsSources<SRC_ID, ITEM, SOURCE extends JskCsSource<SRC_ID, ITEM>> {
    @Getter private final List<SOURCE> sources;
    @Getter private final Map<SRC_ID, SOURCE> sourcesById;

    public JskCsSources(List<SOURCE> sources) {
        this.sources = Collections.unmodifiableList(sources);
        this.sourcesById = Collections.unmodifiableMap(sources.stream().collect(Cc.toM($ -> $.getId(), $ -> $)));
    }

    public SOURCE getById(SRC_ID id) {
        return sourcesById.get(id);
    }
}
