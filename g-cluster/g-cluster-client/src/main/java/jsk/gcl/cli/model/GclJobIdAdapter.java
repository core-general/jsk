package jsk.gcl.cli.model;

import sk.services.json.typeadapterfactories.IdBaseAdapter;

public class GclJobIdAdapter extends IdBaseAdapter<String, GclJobId> {
    @Override
    protected String construct(String value) {
        return value;
    }
}
