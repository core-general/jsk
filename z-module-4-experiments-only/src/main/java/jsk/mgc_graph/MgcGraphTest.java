package jsk.mgc_graph;

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

import sk.outer.graph.MgcModelUtils;
import sk.outer.graph.edges.MgcNormalEdge;
import sk.outer.graph.execution.MgcGraphHistoryItem;
import sk.outer.graph.nodes.MgcGraph;
import sk.outer.graph.nodes.MgcNode;
import sk.outer.graph.parser.MgcDefaultParseEnv;
import sk.outer.graph.parser.MgcParser;
import sk.services.free.Freemarker;
import sk.utils.functional.O;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;

import java.util.ArrayList;
import java.util.List;

public class MgcGraphTest {

    public static void main(String[] args) {
        MgcParser parser = new MgcParser();
        List<MgcGraphHistoryItem> history = new ArrayList<>();
        MgcGraph x = parser.parse("X", "1", Io.getResource("test.graph").get(), O.empty(), new MgcDefaultParseEnv()).left();

        MgcNode p1 = x.getAllNodes().stream().filter($ -> Fu.equal($.getId(), "p1")).findAny().get();
        MgcNormalEdge mgcNormalEdge = x.getDirectEdgesFrom(p1).get(0);
        //MgcGraphExecutionResult execute = x.execute(p1, mgcNormalEdge.getId(), (g, n, e) -> new MgcGraphExecutionContext() {
        //
        //    @Override
        //    public SimplePage<MgcGraphHistoryItem, String> getGraphHistory(int count, O<String> npa, boolean ascending,
        //            O<Boolean> isNodeOrAll) {
        //        return null;
        //    }
        //
        //    @Override
        //    public MgcGraph getExecutedGraph() {
        //        return g;
        //    }
        //
        //    @Override
        //    public MgcNode getFromNode() {
        //        return n;
        //    }
        //
        //    @Override
        //    public MgcNode getToNode() {
        //        return null;
        //    }
        //
        //    @Override
        //    public void setToNode(MgcNode toNode) {
        //
        //    }
        //
        //    @Override
        //    public String getSelectedEdge() {
        //        return null;
        //    }
        //
        //
        //    @Override
        //    public void addGraphHistoryItem(MgcGraphHistoryItem item) {
        //        history.add(item);
        //    }
        //
        //    @Override
        //    public MgcListenerProcessorResultImpl getEdgeProcessor() {
        //        return null;
        //    }
        //
        //    @Override
        //    public MgcListenerProcessorResultImpl getNodeProcessor() {
        //        return null;
        //    }
        //
        //});

        System.out.println(MgcModelUtils.convertToGraphVizFormat(x, new Freemarker()));
        int i = 0;
    }
}
