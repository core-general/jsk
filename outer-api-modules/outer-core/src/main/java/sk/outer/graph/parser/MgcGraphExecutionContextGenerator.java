package sk.outer.graph.parser;

import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.nodes.MgcGraph;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.F3;

public interface MgcGraphExecutionContextGenerator extends F3<MgcGraph, MgcNode, String, MgcGraphExecutionContext> {}
