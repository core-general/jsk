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

import sk.utils.collections.cluster_sorter.abstr.JcsISource;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class JcsSources<ITEM, SOURCE extends JcsISource<ITEM>> {
    private final Map<JcsSourceId, SOURCE> sourcesById;

    public JcsSources(Collection<SOURCE> sources) {
        this.sourcesById = sources.stream().collect(Cc.toM($ -> $.getSourceId(), $ -> $));
    }

    public Map<JcsSourceId, SOURCE> getSourcesById() {
        return Collections.unmodifiableMap(sourcesById);
    }

    public SOURCE getById(JcsSourceId id) {
        return sourcesById.get(id);
    }

    public boolean removeSource(JcsSourceId id) {
        return O.ofNull(sourcesById.get(id)).map($ -> {
            sourcesById.remove($.getSourceId());
            return true;
        }).orElse(false);
    }

    public void addSource(SOURCE src) {
        sourcesById.put(src.getSourceId(), src);
    }
}
