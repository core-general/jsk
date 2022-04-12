package sk.outer.graph.edges;

import sk.outer.graph.MgcParsedDataHolder;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.listeners.MgcListenerProcessor;
import sk.utils.ifaces.IdentifiableString;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.List;
import java.util.function.BiPredicate;

public interface MgcEdge extends MgcListenerProcessor, IdentifiableString, MgcParsedDataHolder {
    default boolean acceptEdge(String edgeId, MgcGraphExecutionContext context) {
        return getPossibleEdges(getParsedData().getText(), context).stream().anyMatch($ -> getAcceptPredicate().test($, edgeId))
                || getAcceptPredicate().test("!any_string_which_will_never_be_met_in_production!", edgeId);
    }

    default BiPredicate<String, String> getAcceptPredicate() {
        return Fu::equal;
    }

    default List<String> getPossibleEdges(String template, MgcGraphExecutionContext context) {
        return Cc.l(template);
    }
}
