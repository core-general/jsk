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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArTest {

    @Test
    public void mapAllTest() {
        Assertions.assertArrayEquals(
                Ar.mapAll(doubles -> doubles[0] + doubles[1], true, new double[]{1, 2, 3}, new double[]{2, 3, 4}),
                new double[]{3, 5, 7}, 0.001);


        Assertions.assertArrayEquals(
                Ar.mapAll(doubles -> doubles[0] + doubles[1], false, new double[]{1, 2, 3}, new double[]{2, 3}),
                new double[]{3, 5, 3}, 0.001);

        Assertions.assertThrows(RuntimeException.class,
                () -> Ar.mapAll(doubles -> doubles[0] + doubles[1], true, new double[]{1, 2, 3}, new double[]{2, 3}));
    }

    @Test
    public void copy() {
        byte[] b = new byte[]{1, 2, 3, 4, 5, 6};
        Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6}, Ar.copy(b, 0, 6));
        Assertions.assertArrayEquals(new byte[]{4, 5}, Ar.copy(b, 3, 2));
        Assertions.assertArrayEquals(new byte[]{4, 5, 6}, Ar.copy(b, 3, 3));
        Assertions.assertArrayEquals(new byte[]{4, 5, 6}, Ar.copy(b, 3, 4));
        Assertions.assertArrayEquals(new byte[]{6}, Ar.copy(b, 5, 4));
    }

    @Test
    public void intToByteArray() {
        final int[] arr = {1, 3, 5, 7, 8};
        final byte[] bytes = Ar.intToByteArray(arr);
        final int[] arr2 = Ar.byteToIntArray(bytes);

        Assertions.assertArrayEquals(arr, arr2);
    }
}
