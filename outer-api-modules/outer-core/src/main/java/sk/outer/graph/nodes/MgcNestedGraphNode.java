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

import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.execution.MgcGraphExecutionResult;
import sk.outer.graph.execution.MgcGraphExecutionResultImpl;
import sk.outer.graph.execution.MgcListenerResults;
import sk.outer.graph.listeners.MgcListenerProcessorResult;
import sk.outer.graph.listeners.impl.MgcDefaultEdgeVariantsListener;
import sk.outer.graph.listeners.impl.MgcDefaultHistoryUpdaterListener;
import sk.outer.graph.listeners.impl.MgcEdgeVariantsListenerResult;
import sk.outer.graph.parser.MgcCtxProvider;
import sk.outer.graph.parser.MgcGraphExecutionContextGenerator;
import sk.outer.graph.parser.MgcParsedData;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.O;

import static sk.outer.graph.parser.MgcCtxProvider.generator;

public class MgcNestedGraphNode<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>,
        CTX1 extends MgcGraphExecutionContext<CTX1, T1>, T1 extends Enum<T1> & MgcTypeUtil<T1>>
        extends MgcNodeBase<CTX, T> {

    private MgcGraphExecutor<CTX1, T1> executor;
    private MgcGraphExecutionContextGenerator<CTX1, T1> contextGenerator;

    public MgcNestedGraphNode(MgcParsedData<T> parsedData,
            MgcGraphExecutor<CTX1, T1> executor,
            MgcGraphExecutionContextGenerator<CTX1, T1> contextGenerator
    ) {
        super(parsedData);
        this.executor = executor;
        this.contextGenerator = contextGenerator;
    }

    @Override
    protected void initDefaultListeners() {
        //not working here addListenerLast(new MgcDefaultEdgeVariantsListener<>(this));
        //addListenerLast(new MgcDefaultNodeTextListener<>(this));
        addListenerLast(MgcDefaultHistoryUpdaterListener.node(this));
    }

    /*
                ** - means which cases are covered
                 ______ Nested graph B _________    **
                |        **           **        | --bc-> C
       A --ab-> | B.A --babb-> B.B --bbbc-> B.C |   **
                |_______________________________| --bd-> D
     */
    public O<MgcGraphExecutionResult> executeNestedGraph(CTX context, String selectedEdge) {
        final MgcCtxProvider<CTX1, T1> ctx = generator(contextGenerator);
        var result = executor.executeByHistoryNested(O.of(selectedEdge), ctx, context.getNestingLevel() + 1);

        if (result.isReachedFinalNode() && result.isCantFindEdge()) {
            //B.C - we can't move from it in nested graph, we need to use upper level graph to proceed
            return O.empty();
        }

        if (result.isReachedFinalNode()) {
            //B.B--bbbc->B.C first time we need to replace it's edge variants with the variants of top graph in both:
            //history and listener
            final MgcEdgeVariantsListenerResult edges = new MgcDefaultEdgeVariantsListener<>(this).apply(context);
            if (edges.getNormalEdges().size() > 0) {
                result.unreachFinalNode();
            }
            ctx.getContextMustExist().getHistory().replaceLastItemWith(
                    ctx.getContextMustExist().getHistory().getLastNode().get()
                            .withReplacedEdges(edges.getNormalEdges(), edges.getMetaEdges())
            );
            result.getResults().getNodeListeners().replaceResult(MgcDefaultEdgeVariantsListener.id, edges);
        }

        return O.of(result);
    }

    /*
                ** - means which cases are covered
                 ______ Nested graph B _________
           **   |                               | --bc-> C
       A --ab-> | B.A --babb-> B.B --bbbc-> B.C |
                |_______________________________| --bd-> D
     */
    public MgcGraphExecutionResult executedNestedFirst(O<MgcEdge<CTX, T>> inputEdge, CTX context) {
        var edgeListeners = inputEdge.map($ -> $.executeListeners(context, context.getResults().getEdgeListeners()))
                .orElse(new MgcListenerProcessorResult());
        var nodeListeners = executeListeners(context, context.getResults().getNodeListeners());

        var result = executor.executeByHistoryNested(O.empty(), generator(contextGenerator), context.getNestingLevel() + 1);

        if (result.isInitialStep()) {
            //A--ab->B.A - we need to execute listeners of B and ab, because we are entering both B and B.A
            return new MgcGraphExecutionResultImpl(
                    new MgcListenerResults(
                            nodeListeners.rewriteNoErrorsBy(result.getResults().getNodeListeners()),
                            edgeListeners.rewriteNoErrorsBy(result.getResults().getEdgeListeners())), result.isCantFindEdge(),
                    result.isInitialStep(), result.isReachedFinalNode());
        } else {
            //should never occur
            throw new RuntimeException(
                    "Can't executedNestedFirst on: --" + inputEdge.map($ -> $.getId()).orElse("NONE") + "-> " + this.getId());
        }
    }
}
