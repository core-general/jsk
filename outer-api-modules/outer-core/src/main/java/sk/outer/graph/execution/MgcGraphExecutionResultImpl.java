package sk.outer.graph.execution;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MgcGraphExecutionResultImpl implements MgcGraphExecutionResult {
    MgcGraphExecutionContext context;
    boolean cantFindEdge;

}
