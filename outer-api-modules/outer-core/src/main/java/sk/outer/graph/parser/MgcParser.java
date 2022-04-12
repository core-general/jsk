package sk.outer.graph.parser;

import lombok.Data;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.edges.MgcNormalEdge;
import sk.outer.graph.nodes.MgcGraph;
import sk.outer.graph.nodes.MgcGraphImpl;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.util.*;
import java.util.stream.Collectors;

import static sk.utils.functional.O.empty;
import static sk.utils.statics.Cc.*;
import static sk.utils.statics.Fu.equal;

public class MgcParser {
    public OneOf<MgcGraph, String> parse(String id, String version, String graphTxt, O<MgcGraph> parent,
            MgcParseEnv graphSource) {
        MgcParseContext parseContext = new MgcParseContext(id, version, graphSource, parent);
        Cc.eachWithIndex(Cc.l(graphTxt.split("\n")), (line, i) -> processLine(line.trim(), parseContext));
        finishLastItemIfNeeded(parseContext);
        O<String> error = validateContext(parseContext);
        return error.map(OneOf::<MgcGraph, String>right).orElseGet(() -> OneOf.left(prepareGraph(parseContext)));
    }

    protected MgcObjectGenerator getInnerGraphGenerator(MgcParseEnv graphSource) {
        return (MgcNodeGenerator) parsedData -> {
            String graphId = parsedData.getParams().get(0);
            return graphSource.getOrCreateInnerGraph(graphId).map(innerGraph -> innerGraph);
        };

    }

    private boolean processLine(String line, MgcParseContext parseContext) {
        try {
            if (line.startsWith("//")) {
                return false;
            }
            switch (parseContext.stage) {
                case START_END_META:
                    return parseStartEndMetaStage(line, parseContext);
                case GRAPH:
                    return parseGraphStage(line, parseContext);
                case PROPERTIES_HEADER:
                    return parsePropertiesHeaderStage(line, parseContext);
                case PROPERTIES_BODY_WAIT:
                    return parsePropertiesBodyWaitStage(line, parseContext);
                case PROPERTIES_BODY_OK:
                    return parsePropertiesBodyOkStage(line, parseContext);
                case FINISHED:
                    return false;
            }
        } finally {
            parseContext.curentLineNumber++;
        }
        throw new RuntimeException();
    }

    private boolean parseStartEndMetaStage(String line, MgcParseContext context) {
        if (St.isNullOrEmpty(line)) {
            return true;
        }
        if (line.startsWith("---")) {
            context.stage = MgcParseStage.GRAPH;
            return true;
        }

        if (line.startsWith("START:")) {
            context.initialState = St.subLF(line, ":").trim();
        } else if (line.startsWith("META:")) {
            String[] split = St.subLF(line, ":").trim().split("\\s+");
            context.cyclingMetaEdge = split[2];
        }
        return true;
    }

    private boolean parseGraphStage(String line, MgcParseContext context) {
        if (St.isNullOrEmpty(line)) {
            return true;
        }
        if (line.startsWith("---")) {
            context.stage = MgcParseStage.PROPERTIES_HEADER;
        } else {
            String[] split = line.split("\\s+");
            if (split.length != 3 || equal(split[2], "?") || stream(split).filter($ -> equal($, "?")).count() > 1) {
                return except(context.curentLineNumber, "(v1 v2 e1) OR (? v2 e1) OR (v1 ? e1) expected");
            }

            if (equal(split[0], "?")) {
                context.s1Nodes.add(split[1]);
                context.s1MetaEdges.put(split[2], X.x(split[1], split[2]));
            } else if (equal(split[1], "?")) {
                context.s1Nodes.add(split[0]);
                context.s1MetaBackEdges.put(split[2], split[0]);
            } else {
                context.s1Nodes.add(split[0]);
                context.s1Nodes.add(split[1]);
                computeAndApply(context.s1Edges, split[2], (v, l) -> l, Cc::l).add(new X2<>(split[0], split[1]));
            }
        }
        return true;
    }

    private boolean parsePropertiesHeaderStage(String line, MgcParseContext context) {
        if (St.isNullOrEmpty(line)) {
            return true;
        }

        context.tempType = parseObjAndType(line, context.curentLineNumber, true);
        context.stage = MgcParseStage.PROPERTIES_BODY_WAIT;
        return true;
    }

    private boolean parsePropertiesBodyWaitStage(String line, MgcParseContext context) {
        if (St.isNullOrEmpty(line)) {
            return except(context.curentLineNumber, "Body must be right below header");
        }
        context.tempBody = line;
        context.stage = MgcParseStage.PROPERTIES_BODY_OK;
        return true;
    }


    private boolean parsePropertiesBodyOkStage(String line, MgcParseContext context) {
        if (St.isNullOrEmpty(line)) {
            finishLastItemIfNeeded(context);
            context.stage = MgcParseStage.PROPERTIES_HEADER;
            return true;
        }

        context.tempBody += "\n" + line;
        return true;
    }

    private O<String> validateContext(MgcParseContext context) {
        StringBuilder sb = new StringBuilder();
        List<String> badNodes =
                context.s1Nodes.stream().filter($ -> !context.s2Objects.containsKey($)).collect(Collectors.toList());
        if (badNodes.size() > 0) {
            sb.append("Bad nodes:" + Cc.join(badNodes));
        }
        List<String> badEdges =
                context.s1Edges.keySet().stream().filter($ -> !context.s2Objects.containsKey($)).collect(Collectors.toList());
        if (badEdges.size() > 0) {
            sb.append("Bad edges:" + Cc.join(badEdges));
        }

        List<String> tooLongEdges = context.s1Edges.keySet().stream()
                .filter($ -> {
                    try {
                        return context.s2Objects.get($).getText().length() > context.getParseEnv().maxSizeOfEdgeText()
                                && !context.parseEnv.isEdgeSizeOk(context.s2Objects.get($));
                    } catch (Exception e) {
                        throw new RuntimeException("Prolem with edge:" + $);
                    }
                })
                .collect(Collectors.toList());
        if (tooLongEdges.size() > 0) {
            sb.append("Too long edges:" + Cc.join(tooLongEdges));
        }

        String s = sb.toString().trim();
        return St.isNullOrEmpty(s) ? empty() : O.of(s);
    }

    private MgcGraph prepareGraph(MgcParseContext context) {
        MgcGraph mgcGraph = new MgcGraphImpl(new MgcParsedData(context.id, "GRAPH", Cc.lEmpty(), ""),
                context.version, context.parent);
        F1<String, MgcParsedData> s2Object =
                (f) -> O.ofNullable(context.getS2Objects().get(f))
                        .orElseGet(() -> except(-1, "Unknown id:" + f));
        F1<String, MgcObjectGenerator> getGenerator = (String a) -> context.getParseEnv()
                .getGenerator(O.ofNullable(s2Object.apply(a)).map($ -> $.getType()))
                .orElseGet(() -> except(-1, "Unknown type:" + a));
        Map<String, MgcNode> nodes = context.s1Nodes.stream()
                .map($ -> {
                    MgcNode node = getGenerator.apply($)
                            .getNodeGenerator(s2Object.apply($))
                            .orElseGet(() -> except(-1, "Node generator fail:" + s2Object.apply($)));
                    mgcGraph.addNode(node);
                    return X.x($, node);
                })
                .collect(Cc.toMX2());

        context.s1Edges.forEach((k, v) -> v
                .forEach($ -> mgcGraph.addNormalEdge(nodes.get($.i1), nodes.get($.i2),
                        (MgcNormalEdge) getGenerator.apply(k).getEdgeGenerator(s2Object.apply(k), false)
                                .orElseGet(() -> except(-1, "Edge generator fail:" + s2Object.apply(k)))
                )));

        context.s1MetaEdges.forEach((k, $) -> mgcGraph.addMetaEdge(nodes.get($.i1),
                (MgcMetaEdge) getGenerator.apply($.i2).getEdgeGenerator(s2Object.apply($.i2), true)
                        .orElseGet(() -> except(-1, "Edge generator fail:" + s2Object.apply($.i2)))));

        context.s1MetaBackEdges.forEach((edgeId, nodeFromId) -> mgcGraph.addMetaEdgeBack(nodes.get(nodeFromId),
                (MgcMetaEdge) getGenerator.apply(edgeId).getEdgeGenerator(s2Object.apply(edgeId), true)
                        .orElseGet(() -> except(-1, "Edge generator fail:" + s2Object.apply(edgeId)))));


        Set<String> endings = mgcGraph.getAllNodes().stream()
                .filter($ -> mgcGraph.getDirectEdgesFrom($).stream().count() == 0)
                .map($ -> $.getId())
                .collect(Collectors.toSet());

        mgcGraph.setMetaInfo(context.getInitialState(), endings, O.of(mgcGraph.getMetaEdges().stream()
                .filter($ -> context.getCyclingMetaEdge() != null && Fu.equal($.getId(), context.getCyclingMetaEdge()))
                .findAny()));

        //validate starting state
        mgcGraph.getStartingStateId();

        return mgcGraph;
    }

    private boolean finishLastItemIfNeeded(MgcParseContext context) {
        MgcParsedData tempType = context.tempType;
        if (!context.s1Edges.containsKey(tempType.id)
                && !context.s1MetaEdges.containsKey(tempType.id)
                && !context.s1MetaBackEdges.containsKey(tempType.id)
                && !context.s1Nodes.contains(tempType.id)) {
            return except(context.curentLineNumber, "Wrong objectId:" + context.getTempType().getId());
        }
        if (context.getTempBody() == null) {
            return except(context.curentLineNumber,
                    "Body must not be null objectId:" + context.getTempType().getId());
        }

        tempType.setText(context.getTempBody());
        context.s2Objects.put(context.tempType.id, tempType);

        context.tempBody = null;
        context.tempType = null;
        return true;
    }

    private <T> T except(long lineNumber, String text) {
        throw new RuntimeException("On line " + (lineNumber + 1) + " : " + text);
    }

    private MgcParsedData parseObjAndType(String line, long lineNumber, boolean allowSingle) {
        String[] split = line.split("\\s+");
        if (split.length < 2 && !allowSingle) {
            return except(lineNumber, "Additional params must have type to create nodes and edges");
        }
        return new MgcParsedData(split[0], split.length > 1 ? split[1] : null, stream(split).skip(2).collect(toL()), null);
    }

    @Data
    public static class MgcParseContext {
        private final String id;
        private final String version;
        private final MgcParseEnv parseEnv;
        private O<MgcGraph> parent;

        long curentLineNumber = 0;

        MgcParseStage stage = MgcParseStage.START_END_META;
        Set<String> s1Nodes = new HashSet<>();
        Map<String, List<X2<String, String>>> s1Edges = new LinkedHashMap<>();
        Map<String, X2<String, String>> s1MetaEdges = new LinkedHashMap<>();
        Map<String, String> s1MetaBackEdges = new LinkedHashMap<>();
        String cyclingMetaEdge;

        Map<String, MgcParsedData> s2Objects = new LinkedHashMap<>();

        String tempBody;
        MgcParsedData tempType;
        String initialState;

        public MgcParseContext(String id, String version, MgcParseEnv parseEnv,
                O<MgcGraph> parent) {
            this.id = id;
            this.version = version;
            this.parseEnv = parseEnv;
            this.parent = parent;
        }

    }

    enum MgcParseStage {
        START_END_META,
        GRAPH,
        PROPERTIES_HEADER,
        PROPERTIES_BODY_WAIT,
        PROPERTIES_BODY_OK,
        FINISHED
    }
}
