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

import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.parser.MgcParsedData;
import sk.utils.ifaces.IdentifiableString;
import sk.utils.statics.Cc;
import sk.utils.statics.Re;

import java.util.List;
import java.util.stream.Collectors;

public class MgcEnumEdge extends MgcNormalEdge {
    final List<String> items;

    public MgcEnumEdge(MgcParsedData parsedData) {
        super(parsedData);

        if (parsedData.getParams().size() < 1) {
            throw new RuntimeException(parsedData + " need 1 parameter");
        }

        Class<?> cls = Re.cls4Name(parsedData.getParams().get(0));
        Object[] enumConstants = cls.getEnumConstants();

        if (enumConstants == null) {
            throw new RuntimeException(parsedData + " must have enum parameter");
        }

        items = Cc.stream(enumConstants)
                .filter(this::filterEnum)
                .map($ -> $ instanceof IdentifiableString ? ((IdentifiableString) $).getId() : $.toString())
                .collect(Collectors.toList());
    }

    protected boolean filterEnum(Object enumValue) {
        return true;
    }

    @Override
    public List<String> getPossibleEdges(String template, MgcGraphExecutionContext context) {
        return items;
    }
}
