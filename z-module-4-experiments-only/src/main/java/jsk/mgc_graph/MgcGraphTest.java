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

import sk.outer.graph.MgcHistoryProvider;
import sk.outer.graph.MgcModelUtils;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.execution.MgcGraphExecutionResult;
import sk.outer.graph.execution.MgcGraphHistoryItem;
import sk.outer.graph.execution.MgcObjectType;
import sk.outer.graph.nodes.MgcGraphExecutor;
import sk.outer.graph.nodes.MgcNode;
import sk.outer.graph.nodes.MgcNodeBase;
import sk.outer.graph.parser.MgcCtxProvider;
import sk.outer.graph.parser.MgcObjectGenerator;
import sk.outer.graph.parser.MgcParser;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.services.free.Freemarker;
import sk.services.rand.RandImpl;
import sk.utils.functional.O;
import sk.utils.paging.SimplePage;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Io;
import sk.utils.statics.Ma;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static sk.outer.graph.parser.MgcObjectGenerator.edge;
import static sk.outer.graph.parser.MgcObjectGenerator.node;

public class MgcGraphTest {

    static volatile Deque<MgcGraphHistoryItem> history = new LinkedList<>();

    public static void main(String[] args) {
        var ge = new MgcGraphExecutor<>(new MgcParser<Ctx, GType>() {}
                .parse("test.graph", "1", Io.getResource("graphs/test.graph").get(), type -> getCtxGTypeMgcObjectGenerator(type))
                .collect($ -> $, $ -> Ex.thRow($)));


        System.out.println(MgcModelUtils.convertToGraphVizFormat(ge.getGraph(), new Freemarker()));


        Io.endlessReadFromKeyboard("XXX", s -> onUserInput(ge, s));
    }

    private static void onUserInput(MgcGraphExecutor<Ctx, GType> ge, String s) {
        MgcGraphExecutionResult<Ctx, GType> execution = ge.executeByHistory(O.of(s), MgcCtxProvider.generator(Ctx::new));

        if (execution.isError()) {
            System.out.println("PROBLEM: " + execution.getError().get().getMessage());
        } else {
            var text = execution.getResults().getNewNodeInfo().getNewNodeText();
            var edges = execution.getResults().getPossibleEdges();

            System.out.println("""
                    Text: %s
                    Possible Edges: %s""".formatted(text, Cc.join(", ", edges.getAllEdges())));
        }
    }

    private static MgcObjectGenerator<Ctx, GType> getCtxGTypeMgcObjectGenerator(GType type) {
        return switch (type) {
            case N_GRAPH -> node(parsedData -> new MgcNodeBase<>(parsedData));
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


    private static class Ctx extends MgcGraphExecutionContext<Ctx, GType> {
        public Ctx(MgcGraphExecutor<Ctx, GType> executedGraph, O<MgcNode<Ctx, GType>> fromNode, O<String> selectedEdge) {
            super(executedGraph, fromNode, selectedEdge);
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


    private static class ThisHistoryProvider implements MgcHistoryProvider {
        @Override
        public synchronized void addGraphHistoryItem(MgcGraphHistoryItem item) {
            history.addFirst(item);
        }

        @Override
        public synchronized SimplePage<MgcGraphHistoryItem, String> getGraphHistory(int count, O<String> npa, boolean ascending,
                MgcObjectType type) {
            int skip = npa.map($ -> Ma.pi($)).orElse(0);
            return new SimplePage<>(history.stream().skip(skip).limit(count).collect(Collectors.toList()), (skip + count) + "");
        }

        @Override
        public boolean userHasHistory() {
            return history.size() > 0;
        }
    }
}
