package jsk.gcl.cli.model;

import com.google.gson.annotations.JsonAdapter;
import sk.services.ids.IIds;
import sk.utils.ids.IdString;

@JsonAdapter(GclJobIdAdapter.class)
public class GclJobGroupId extends IdString {
    public final static String type = "jsk.gcl.cli.model.GclJobGroupId";

    public GclJobGroupId(IIds ids) {
        super(ids.timedHaiku());
    }

    //should be used for deserialization
    GclJobGroupId(String uuid) {
        super(uuid);
    }
}
