package jsk.gcl.srv.jpa;

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

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import sk.services.bytes.BytesImpl;
import sk.services.ids.IIds;
import sk.services.ids.IdsImpl;
import sk.services.rand.IRand;
import sk.test.MockitoTest;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class GclStorageFacadeImplTest extends MockitoTest {
    @Spy
    IIds ids = new IdsImpl(new IRand() {
        Random r = new Random(0);

        @Override
        public Random getRandom() {
            return r;
        }
    }, new BytesImpl());

    @InjectMocks
    GclStorageFacadeImpl impl = new GclStorageFacadeImpl(ids);

    @Test
    public void prepareTagFromId() {
        assertEquals(impl.prepareTagFromId(new GclNodeId("a-b-c-d")), "a-b");
        assertEquals(impl.prepareTagFromId(new GclNodeId("a-b-c")), "a-b");
        assertEquals(impl.prepareTagFromId(new GclNodeId("a-b")), "a-b");
        assertEquals(impl.prepareTagFromId(new GclNodeId("a")), "horror-mission");
    }
}
