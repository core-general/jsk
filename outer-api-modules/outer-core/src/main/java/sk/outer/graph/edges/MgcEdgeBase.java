package sk.outer.graph.edges;

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
import sk.outer.graph.listeners.MgcListenerProcessorBase;
import sk.outer.graph.listeners.impl.MgcDefaultHistoryUpdaterListener;
import sk.outer.graph.parser.MgcParsedData;

@EqualsAndHashCode(of = {"parsedData"}, callSuper = false)
@Data
@AllArgsConstructor
public class MgcEdgeBase extends MgcListenerProcessorBase implements MgcEdge {
    MgcParsedData parsedData;

    {
        addListenerLast(MgcDefaultHistoryUpdaterListener.edge(this));
    }

    @Override
    public String getId() {
        return parsedData.getId();
    }
}
