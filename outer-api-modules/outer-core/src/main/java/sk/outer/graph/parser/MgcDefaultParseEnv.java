package sk.outer.graph.parser;

import sk.utils.functional.O;


public class MgcDefaultParseEnv implements MgcParseEnv {
    @Override
    public O<MgcObjectGenerator> getGenerator(O<String> type) {
        return O.of(new MgcDefaultObjectGenerator());
    }

    @Override
    public boolean isEdgeSizeOk(MgcParsedData mgcParsedData) {
        return false;
    }
}
