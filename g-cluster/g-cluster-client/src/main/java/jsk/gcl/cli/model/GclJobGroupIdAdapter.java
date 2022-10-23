package jsk.gcl.cli.model;

import sk.services.json.typeadapterfactories.IdBaseAdapter;

public class GclJobGroupIdAdapter extends IdBaseAdapter<String, GclJobGroupId> {
    @Override
    protected String construct(String value) {
        return value;
    }
}
