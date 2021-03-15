package sk.services.kv.keys;

import org.junit.Test;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;
import sk.utils.tuples.X1;

import static org.junit.Assert.assertEquals;

public class KvKeyEnumTest {

    @Test
    public void name() {
        X1<String> trickster = X.x(null);
        KvKeyEnum key = new KvKeyEnum() {
            @Override
            public String name() {
                return trickster.getI1();
            }

            @Override
            public String getDefaultValue() {
                return null;
            }
        };

        trickster.set("abc_def_gcs_gkt");
        assertEquals(Cc.join(key.categories()), "abc,def,gcs_gkt");

        trickster.set("abc_def_gcs");
        assertEquals(Cc.join(key.categories()), "abc,def,gcs");

        trickster.set("abc_def");
        assertEquals(Cc.join(key.categories()), "abc,def");

        trickster.set("abc");
        assertEquals(Cc.join(key.categories()), "abc");

        trickster.set("");
        assertEquals(Cc.join(key.categories()), "");
    }
}