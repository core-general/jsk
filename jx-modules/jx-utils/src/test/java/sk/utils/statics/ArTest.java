package sk.utils.statics;

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

import org.junit.Assert;
import org.junit.Test;

public class ArTest {

    @Test
    public void mapAllTest() {
        Assert.assertArrayEquals(
                Ar.mapAll(doubles -> doubles[0] + doubles[1], true, new double[]{1, 2, 3}, new double[]{2, 3, 4}),
                new double[]{3, 5, 7}, 0.001);


        Assert.assertArrayEquals(
                Ar.mapAll(doubles -> doubles[0] + doubles[1], false, new double[]{1, 2, 3}, new double[]{2, 3}),
                new double[]{3, 5, 3}, 0.001);

        Assert.assertThrows(RuntimeException.class,
                () -> Ar.mapAll(doubles -> doubles[0] + doubles[1], true, new double[]{1, 2, 3}, new double[]{2, 3}));
    }

    @Test
    public void intToByteArray() {
        final int[] arr = {1, 3, 5, 7, 8};
        final byte[] bytes = Ar.intToByteArray(arr);
        final int[] arr2 = Ar.byteToIntArray(bytes);

        Assert.assertArrayEquals(arr, arr2);
    }
}
