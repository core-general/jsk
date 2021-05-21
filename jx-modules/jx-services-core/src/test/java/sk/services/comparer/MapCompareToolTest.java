package sk.services.comparer;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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
import sk.services.comparer.model.MapCompareResult;
import sk.utils.statics.Cc;

import static org.junit.Assert.assertEquals;

public class MapCompareToolTest {

    @Test
    public void compare() {
        {
            MapCompareResult<String, String> res = MapCompareTool.compare(
                    Cc.m("a", "1", "b", "2", "c", "3"), Cc.m("a", "1", "b", "2", "c", "3")
            );
            assertEquals(Cc.joinMap(res.getNotExistingIn2()), "");
            assertEquals(Cc.joinMap(res.getNotExistingIn1()), "");
            assertEquals(Cc.joinMap(res.getExistButDifferent()), "");
        }

        {
            MapCompareResult<String, String> res = MapCompareTool.compare(
                    Cc.m("a", "1", "b", "2", "c", "3"), Cc.m("a", "1")
            );
            assertEquals(Cc.joinMap(res.getNotExistingIn2()), "b:2, c:3");
            assertEquals(Cc.joinMap(res.getNotExistingIn1()), "");
            assertEquals(Cc.joinMap(res.getExistButDifferent()), "");
        }

        {
            MapCompareResult<String, String> res = MapCompareTool.compare(
                    Cc.m("c", "3"), Cc.m("a", "1", "b", "2", "c", "3")
            );
            assertEquals(Cc.joinMap(res.getNotExistingIn2()), "");
            assertEquals(Cc.joinMap(res.getNotExistingIn1()), "a:1, b:2");
            assertEquals(Cc.joinMap(res.getExistButDifferent()), "");
        }

        {
            MapCompareResult<String, String> res = MapCompareTool.compare(
                    Cc.m("a", "1", "c", "3"), Cc.m("b", "2", "c", "3")
            );
            assertEquals(Cc.joinMap(res.getNotExistingIn2()), "a:1");
            assertEquals(Cc.joinMap(res.getNotExistingIn1()), "b:2");
            assertEquals(Cc.joinMap(res.getExistButDifferent()), "");
        }

        {
            MapCompareResult<String, String> res = MapCompareTool.compare(
                    Cc.m("a", "1", "c", "5"), Cc.m("b", "2", "c", "3")
            );
            assertEquals(Cc.joinMap(res.getNotExistingIn2()), "a:1");
            assertEquals(Cc.joinMap(res.getNotExistingIn1()), "b:2");
            assertEquals(Cc.joinMap(res.getExistButDifferent()), "c:{1=5,2=3}");
        }
    }
}
