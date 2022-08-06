package sk.utils.math;

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

import static org.junit.Assert.assertEquals;

public class KahanSumTest {

    @Test
    public void getSum() {
        KahanSum kahanSum = new KahanSum();
        double sum = 0.0;
        for (long i = 0; i < 10L; i++) {
            double toSum = 0.1d;
            kahanSum.add(toSum);
            sum += toSum;
        }
        //System.out.println(kahanSum.getSum());
        //System.out.println(sum);
        assertEquals(kahanSum.getSum() + "", "1.0");
        assertEquals(sum + "", "0.9999999999999999");
    }
}
