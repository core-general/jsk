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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sk.outer.graph.MgcHistoryProvider;
import sk.outer.graph.MgcModelUtils;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.edges.MgcNormalEdge;
import sk.outer.graph.execution.*;
import sk.outer.graph.parser.*;
import sk.services.free.Freemarker;
import sk.services.rand.RandImpl;
import sk.utils.functional.O;
import sk.utils.paging.SimplePage;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Io;
import sk.utils.statics.Ma;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static sk.outer.graph.parser.MgcObjectGenerator.edge;
import static sk.outer.graph.parser.MgcObjectGenerator.node;

public class MgcGraphTest {

    private Deque<MgcGraphHistoryItem> hist = new ArrayDeque<>();
    private Map<String, MgcGraphExecutor<?, ?>> executors = new ConcurrentHashMap<>();

    @Before
    public void init() {
        final String rootId = "test.graph";
        var ge = new MgcGraphExecutor<>(
                new MgcParser<Ctx, GType>(GType.class).parse(rootId, Io.getResource("graphs/" + rootId).get(),
                        new MultiGraphParseEnv()).collect($ -> $, $ -> Ex.thRow($)));

        executors.put(rootId, ge);
    }

    @Test
    public void fullTestWithInnerGraphs() {
        final String rootId = "test.graph";
        var ge = new MgcGraphExecutor<>(
                new MgcParser<Ctx, GType>(GType.class).parse(rootId, Io.getResource("graphs/" + rootId).get(),
                        new MultiGraphParseEnv()).collect($ -> $, $ -> Ex.thRow($)));

        executors.put(rootId, ge);

        System.out.println(rootId + "\n" + MgcModelUtils.convertToGraphVizFormat(ge.getGraph(), new Freemarker()));

        //Io.endlessReadFromKeyboard("XXX", s -> onUserInput(ge, s));

        onUserInput(ge, "?");//init graph execution
        LinkedHashMap<String, String> inAndOut = Cc.lhm(
                "g1e1", "g1n2",
                "g1eY", "g1n0",
                "g1e0", "g1n1",

                "g1e2", "g1G1->g2n1",
                "g2e2", "g1G1->g2n3",
                "g2e5", "g1G1->g2G1->g3n1",
                "g3e1", "g1G1->g2G1->g3n2",
                "g1e4", "g1G2->g3n1",
                "g3e2", "g1G2->g3n3",
                "g1e5", "g1G1->g2n1",
                "g2e1", "g1G1->g2n2",
                "g1e3", "g1n1"
        );
        inAndOut.forEach((in, out) -> {
            final MgcListenerResults result = onUserInput(ge, in);
            Assert.assertEquals("Problem on input: " + in, out,
                    O.ofNull(result).map($ -> $.getNewNodeInfoMustExist().getNewNodeText()).orElse(""));
        });
    }

    private MgcListenerResults onUserInput(MgcGraphExecutor<Ctx, GType> ge, String input) {
        MgcGraphExecutionResult execution = ge.executeByHistory(O.of(input), MgcCtxProvider.generator(Ctx::new));

        if (execution.isError()) {
            execution.getError().get().printStackTrace();
            hist.clear();
            ge.executeByHistory(O.of(input), MgcCtxProvider.generator(Ctx::new));
            return null;
        } else {
            var text = execution.getResults().getNewNodeInfoMustExist().getNewNodeText();
            var edges = execution.getResults().getPossibleEdgesMustExist();

            //System.out.println("""
            //        Text: %s
            //        Possible Edges: %s""".formatted(text, Cc.join(", ", edges.getAllEdges())));
            return execution.getResults();
        }
    }

    private MgcObjectGenerator<Ctx, GType> getCtxGTypeMgcObjectGenerator(GType type) {
        return switch (type) {
            case N_GRAPH -> node(parsedData -> {
                final String resourceName = parsedData.getParams().get(0);
                //noinspection unchecked
                MgcGraphExecutor<Ctx, GType> graph = (MgcGraphExecutor<Ctx, GType>) executors.computeIfAbsent(resourceName,
                        (s) -> new MgcGraphExecutor<Ctx, GType>(new MgcParser<Ctx, GType>(GType.class).parse(resourceName,
                                        Io.getResource("graphs/" + resourceName).get(), new MultiGraphParseEnv())
                                .collect($ -> $, $ -> Ex.thRow($))));
                System.out.println(
                        resourceName + "\n" + MgcModelUtils.convertToGraphVizFormat(graph.getGraph(), new Freemarker()));

                return new MgcNestedGraphNode<Ctx, GType, Ctx, GType>(parsedData, graph, Ctx::new);
            });
            case E_RANDOM_NODE -> edge(pd -> new MgcMetaEdge<>(pd, (ctx) -> {
                return new RandImpl().rndFromList(ctx.getExecutedGraph().getGraph().getAllNodes()).map($ -> $.getId());
            }));
            case E_NULLIFIER -> edge((pd) -> new MgcMetaEdge<>(pd) {
                @Override
                public List<String> getPossibleEdges(String template, Ctx context) {
                    return Cc.l("a", "b");
                }
            });
            case FICTIVE_FICTIVE -> throw new RuntimeException("nope");
        };
    }


    private class Ctx extends MgcGraphExecutionContext<Ctx, GType> {
        public Ctx(MgcGraphExecutor<Ctx, GType> executedGraph, O<MgcNode<Ctx, GType>> fromNode, O<String> selectedEdge,
                int nestingLevel) {
            super(executedGraph, fromNode, selectedEdge, nestingLevel);
        }

        @Override
        public MgcHistoryProvider initHistoryProvider() {
            return new ThisHistoryProvider();
        }
    }

    private enum GType implements MgcTypeUtil<GType> {
        N_GRAPH, E_RANDOM_NODE, E_NULLIFIER, FICTIVE_FICTIVE;

        @Override
        public GType getFictiveType() {
            return FICTIVE_FICTIVE;
        }
    }


    private class ThisHistoryProvider implements MgcHistoryProvider {
        Deque<MgcGraphHistoryItem> h = hist;

        public ThisHistoryProvider() {
        }

        @Override
        public synchronized void addGraphHistoryItem(MgcGraphHistoryItem item) {
            hist.addFirst(item);
            if (hist.size() > 1_000) {
                hist.removeLast();
            }
        }

        @Override
        public synchronized SimplePage<MgcGraphHistoryItem, String> getHistory(int count, O<String> npa, boolean ascending,
                MgcObjectType type) {
            int skip = npa.map($ -> Ma.pi($)).orElse(0);
            return new SimplePage<>(hist.stream()
                    .filter($ -> switch (type) {
                        case NODE -> $.isNode();
                        case EDGE -> !$.isNode();
                        case BOTH -> true;
                    })
                    .skip(skip).limit(count)
                    .collect(Collectors.toList()), (skip + count) + "");
        }

        @Override
        public void replaceLastItemWith(MgcGraphHistoryItem mgcGraphHistoryItem) {
            hist.removeFirst();
            hist.addFirst(mgcGraphHistoryItem);
        }
    }

    private class MultiGraphParseEnv implements MgcParseEnv<Ctx, GType> {
        @Override
        public MgcObjectGenerator<Ctx, GType> generatorByType(GType type) {
            return getCtxGTypeMgcObjectGenerator(type);
        }

        @Override
        public MgcObjectGenerator<Ctx, GType> getDefaultGenerator() {
            return new MgcDefaultObjectGenerator<>(
                    MgcMetaEdge::new, MgcNormalEdge::new,
                    pd -> new MgcNodeBase<>(pd) {
                        @Override
                        public String getText(String template, Ctx context) {
                            return context.getHistory().getCurrentNodeIdWithNesting(context.getNestingLevel(), this);
                        }
                    }
            );
        }
    }
}
