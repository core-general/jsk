package sk.outer.graph.parser;

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

import lombok.Data;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.edges.MgcNormalEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.nodes.MgcGraph;
import sk.outer.graph.nodes.MgcGraphImpl;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.statics.Re;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.of;
import static sk.utils.statics.Cc.*;
import static sk.utils.statics.Fu.equal;

public class MgcParser<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>> {
    Class<T> cls;

    public MgcParser(Class<T> cls) {
        this.cls = cls;
    }

    public OneOf<MgcGraph<CTX, T>, String> parse(
            String id,
            String graphTxt,
            MgcParseEnv<CTX, T> graphSource
    ) {
        MgcParseContext<CTX, T> parseContext = new MgcParseContext<>(id, graphSource);
        Cc.eachWithIndex(Cc.l(graphTxt.split("\n")), (line, i) -> processLine(line.trim(), parseContext));
        finishLastItemIfNeeded(parseContext);
        O<String> error = validateContext(parseContext);
        return error.map(OneOf::<MgcGraph<CTX, T>, String>right).orElseGet(() -> OneOf.left(prepareGraph(parseContext)));
    }

    private void processLine(String line, MgcParseContext<CTX, T> parseContext) {
        try {
            if (line.startsWith("//")) {
                return;
            }
            switch (parseContext.stage) {
                case START_END_META -> parseStartEndMetaStage(line, parseContext);
                case GRAPH -> parseGraphStage(line, parseContext);
                case PROPERTIES_HEADER -> parsePropertiesHeaderStage(line, parseContext);
                case PROPERTIES_BODY_WAIT -> parsePropertiesBodyWaitStage(line, parseContext);
                case PROPERTIES_BODY_OK -> parsePropertiesBodyOkStage(line, parseContext);
                case FINISHED -> {}
            }
        } finally {
            parseContext.currentLineNumber++;
        }
    }

    private void parseStartEndMetaStage(String line, MgcParseContext<CTX, T> context) {
        if (St.isNullOrEmpty(line)) {
            return;
        }
        if (line.startsWith("---")) {
            if (context.initialState == null || context.version == null) {
                throw new RuntimeException("Graph:'" + context.id + "' metainfo should have both VERSION and START sections");
            }
            context.stage = MgcParseStage.GRAPH;
        }

        if (line.startsWith("VERSION:") || line.startsWith("VER:")) {
            context.version = St.subLF(line, ":").trim();
        } else if (line.startsWith("START:")) {
            context.initialState = St.subLF(line, ":").trim();
        } else if (line.startsWith("META:")) {
            String[] split = St.subLF(line, ":").trim().split("\\s+");
            context.cyclingMetaEdge = split[2];
        }
    }

    private void parseGraphStage(String line, MgcParseContext<CTX, T> context) {
        if (St.isNullOrEmpty(line)) {
            return;
        }
        if (line.startsWith("---")) {
            context.stage = MgcParseStage.PROPERTIES_HEADER;
        } else {
            String[] split = line.split("\\s+");
            if (split.length != 3 || equal(split[2], "?") || stream(split).filter($ -> equal($, "?")).count() > 1) {
                except(context.currentLineNumber, "(v1 v2 e1) OR (? v2 e1) OR (v1 ? e1) expected");
                return;
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
    }

    private void parsePropertiesHeaderStage(String line, MgcParseContext<CTX, T> context) {
        if (!St.isNullOrEmpty(line)) {
            context.tempType = parseObjAndType(line, context.currentLineNumber);
            context.stage = MgcParseStage.PROPERTIES_BODY_WAIT;
        }
    }

    private void parsePropertiesBodyWaitStage(String line, MgcParseContext<CTX, T> context) {
        if (St.isNullOrEmpty(line)) {
            except(context.currentLineNumber, "Body must be right below header");
        } else {
            context.tempBody = line;
            context.stage = MgcParseStage.PROPERTIES_BODY_OK;
        }
    }


    private void parsePropertiesBodyOkStage(String line, MgcParseContext<CTX, T> context) {
        if (St.isNullOrEmpty(line)) {
            finishLastItemIfNeeded(context);
            context.stage = MgcParseStage.PROPERTIES_HEADER;
        } else {
            context.tempBody += "\n" + line;
        }
    }

    private O<String> validateContext(MgcParseContext<CTX, T> context) {
        StringBuilder sb = new StringBuilder();
        List<String> badNodes =
                context.s1Nodes.stream().filter($ -> !context.s2Objects.containsKey($)).collect(Collectors.toList());
        if (badNodes.size() > 0) {
            sb.append("Bad nodes:").append(Cc.join(badNodes));
        }
        List<String> badEdges =
                context.s1Edges.keySet().stream().filter($ -> !context.s2Objects.containsKey($)).collect(Collectors.toList());
        if (badEdges.size() > 0) {
            sb.append("Bad edges:").append(Cc.join(badEdges));
        }

        List<String> tooLongEdges = context.s1Edges.keySet().stream()
                .filter($ -> {
                    try {
                        return context.s2Objects.get($).getText().length() > context.getParseEnv().maxSizeOfEdgeText()
                                && !context.parseEnv.isLongEdgeSizeOk(context.s2Objects.get($));
                    } catch (Exception e) {
                        throw new RuntimeException("Problem with edge:" + $);
                    }
                })
                .collect(Collectors.toList());
        if (tooLongEdges.size() > 0) {
            sb.append("Too long edges:").append(Cc.join(tooLongEdges));
        }

        String s = sb.toString().trim();
        return St.isNullOrEmpty(s) ? empty() : O.of(s);
    }

    private MgcGraph<CTX, T> prepareGraph(MgcParseContext<CTX, T> context) {
        MgcGraph<CTX, T> mgcGraph = new MgcGraphImpl<>(
                context.getId(), context.version, getTypeSelector().getEnumConstants()[0].getFictiveType());
        F1<String, MgcParsedData<T>> s2Object =
                (f) -> O.ofNullable(context.getS2Objects().get(f))
                        .orElseGet(() -> except(-1, "Unknown id:" + f));
        F1<String, MgcObjectGenerator<CTX, T>> getGenerator = (String a) -> context.getParseEnv()
                .getGenerator(O.ofNullable(s2Object.apply(a)).map($ -> $.getType()));
        Map<String, MgcNode<CTX, T>> nodes = context.s1Nodes.stream()
                .map($ -> {
                    MgcNode<CTX, T> node = requireNonNull(getGenerator.apply($)
                            .getNodeGenerator(s2Object.apply($)), "Node generator fail:" + s2Object.apply($));
                    mgcGraph.addNode(node);
                    return X.x($, node);
                })
                .collect(Cc.toMX2());

        context.s1Edges.forEach((k, v) -> v
                .forEach($ -> mgcGraph.addNormalEdge(nodes.get($.i1), nodes.get($.i2),
                        (MgcNormalEdge<CTX, T>) requireNonNull(
                                getGenerator.apply(k).getEdgeGenerator(s2Object.apply(k).withMeta(of(false))),
                                "Edge generator fail:" + s2Object.apply(k)))
                ));

        context.s1MetaEdges.forEach((k, $) -> mgcGraph.addMetaEdge(nodes.get($.i1),
                (MgcMetaEdge<CTX, T>) requireNonNull(
                        getGenerator.apply($.i2).getEdgeGenerator(s2Object.apply($.i2).withMeta(of(true))),
                        "Edge generator fail:" + s2Object.apply($.i2))));

        context.s1MetaBackEdges.forEach((edgeId, nodeFromId) -> mgcGraph.addMetaEdgeBack(nodes.get(nodeFromId),
                (MgcMetaEdge<CTX, T>) requireNonNull(
                        getGenerator.apply(edgeId).getEdgeGenerator(s2Object.apply(edgeId).withMeta(of(true))),
                        "Edge generator fail:" + s2Object.apply(edgeId))));


        Set<String> endings = mgcGraph.getAllNodes().stream()
                .filter($ -> mgcGraph.getNormalEdgesFrom($).size() == 0)
                .map($ -> $.getId())
                .collect(Collectors.toSet());

        mgcGraph.setMetaInfo(context.getInitialState(), endings, O.of(mgcGraph.getAllMetaEdges().stream()
                .filter($ -> context.getCyclingMetaEdge() != null && Fu.equal($.getId(), context.getCyclingMetaEdge()))
                .findAny()));

        //validate starting state
        mgcGraph.getStartingStateId();

        return mgcGraph;
    }

    private void finishLastItemIfNeeded(MgcParseContext<CTX, T> context) {
        MgcParsedData<T> tempType = context.tempType;
        if (!context.s1Edges.containsKey(tempType.id)
                && !context.s1MetaEdges.containsKey(tempType.id)
                && !context.s1MetaBackEdges.containsKey(tempType.id)
                && !context.s1Nodes.contains(tempType.id)) {
            except(context.currentLineNumber, "Wrong objectId:" + context.getTempType().getId());
            return;
        }
        if (context.getTempBody() == null) {
            except(context.currentLineNumber,
                    "Body must not be null objectId:" + context.getTempType().getId());
            return;
        }

        tempType.setText(context.getTempBody());
        context.s2Objects.put(context.tempType.id, tempType);

        context.tempBody = null;
        context.tempType = null;
    }

    private <XXX> XXX except(long lineNumber, String text) {
        throw new RuntimeException("On line " + (lineNumber + 1) + " : " + text);
    }

    private Class<T> getTypeSelector() {
        return cls;
    }

    private MgcParsedData<T> parseObjAndType(String line, long lineNumber) {
        String[] split = line.split("\\s+");

        return new MgcParsedData<>(split[0],
                split.length > 1 ? Re.findInEnum(getTypeSelector(), split[1])
                        .orElseThrow(() -> new RuntimeException("Unknown type: " + split[1] + " on line: " + lineNumber)) : null,
                stream(split).skip(2).collect(toL()), null, empty());
    }

    @Data
    public static class MgcParseContext<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>> {
        private final String id;
        private final MgcParseEnv<CTX, T> parseEnv;

        long currentLineNumber = 0;

        MgcParseStage stage = MgcParseStage.START_END_META;
        Set<String> s1Nodes = new HashSet<>();
        Map<String, List<X2<String, String>>> s1Edges = new LinkedHashMap<>();
        Map<String, X2<String, String>> s1MetaEdges = new LinkedHashMap<>();
        Map<String, String> s1MetaBackEdges = new LinkedHashMap<>();
        String cyclingMetaEdge;

        Map<String, MgcParsedData<T>> s2Objects = new LinkedHashMap<>();

        String tempBody;
        MgcParsedData<T> tempType;
        String initialState;
        String version;

        public MgcParseContext(String id, MgcParseEnv<CTX, T> parseEnv) {
            this.id = id;
            this.parseEnv = parseEnv;
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
