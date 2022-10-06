package jsk.gcl.srv.jpa;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import sk.services.bytes.BytesImpl;
import sk.services.ids.IIds;
import sk.services.ids.IdsImpl;
import sk.services.rand.RandImpl;
import sk.test.MockitoTest;

import static org.junit.Assert.assertEquals;

public class GclStorageFacadeImplTest extends MockitoTest {
    @Spy
    IIds ids = new IdsImpl(new RandImpl(), new BytesImpl());

    @InjectMocks
    GclStorageFacadeImpl impl = new GclStorageFacadeImpl();

    @Test
    public void prepareTagFromId() {
        assertEquals(impl.prepareTagFromId("a-b-c"), "a-b");
        assertEquals(impl.prepareTagFromId("a-b"), "a-b");
        assertEquals(impl.prepareTagFromId("a"), "haiku");
    }
}