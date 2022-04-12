package jsk.mgc_graph;

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
