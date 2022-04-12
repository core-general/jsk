package sk.outer.graph.nodes;

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

import sk.outer.graph.parser.MgcParsedData;
import sk.utils.statics.Cc;

public class MgcFictiveNode extends MgcNodeBase {

    public static final String FICTIVE_START = "_fictive_start";
    public static final String FICTIVE_END = "_fictive_end";

    public MgcFictiveNode(boolean start) {
        super(new MgcParsedData(start ? FICTIVE_START : FICTIVE_END, "$FICTIVE", Cc.l(), ""));
    }
}
