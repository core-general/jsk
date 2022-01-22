package sk.utils.statics;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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
import sk.utils.functional.O;

import static org.junit.Assert.*;

public class MaTest {
    @Test
    public void testMedian() {
        assertEquals(Ma.median(Cc.l(1L, 5L, 3L, 5L, 2L)).get().longValue(), 3L);
        assertEquals(Ma.median(Cc.l(1L, 5L, 3L, 5L)).get().longValue(), 4L);
        assertEquals(Ma.medianD(Cc.l(1D, 5D, 3D, 5D)).get(), 4D, 0.01D);
        assertEquals(Ma.medianD(Cc.l(1D, 5D, 3D, 5D, 2D)).get(), 3D, 0.01D);
    }

    @Test
    public void testClamp() {
        assertEquals((int) Ma.clamp(0D, 10D, 20D), 10);
        assertEquals((int) Ma.clamp(100D, 10D, 20D), 20);
        assertEquals((int) Ma.clamp(15, 10D, 20D), 15);
        assertEquals((int) Ma.clamp(0D / 0D, 10D, 20D), 10);
        assertEquals((int) Ma.clamp(5D / 0D, 10D, 20D), 20);
        assertEquals((int) Ma.clamp(-5D / 0D, 10D, 20D), 10);
    }

    @Test
    public void testMean() {
        assertEquals(Ma.median(Cc.l()), O.empty());
        assertTrue(Ma.median(Cc.l(1l)).get() == 1l);
        assertTrue(Ma.median(Cc.l(1l, 3l)).get() == 2l);
        assertTrue(Ma.median(Cc.l(1l, 3l, 5l)).get() == 3l);
        assertTrue(Ma.median(Cc.l(1l, 3l, 5l, 7l)).get() == 4l);
    }

    @Test
    public void optimalSampleSizeTest() {
        assertEquals(Ma.optimalSampleSize(Ma.SampleSizeAccuracy._95, 0.05, O.empty()), 384);
        assertEquals(Ma.optimalSampleSize(Ma.SampleSizeAccuracy._95, 0.05, O.of(1000l)), 278);
        assertEquals(Ma.optimalSampleSize(Ma.SampleSizeAccuracy._99, 0.05, O.empty()), 666);
        assertEquals(Ma.optimalSampleSize(Ma.SampleSizeAccuracy._99, 0.05, O.of(1000l)), 400);
    }

    @Test
    public void errorRateOfSampleTest() {
        assertEquals(Math.round(Ma.errorRateOfSample(Ma.SampleSizeAccuracy._95, 50000, O.empty()) * 10000), 44);//0.44%
        assertEquals(Math.round(Ma.errorRateOfSample(Ma.SampleSizeAccuracy._99, 50000, O.empty()) * 10000), 58);//0.58%
        assertEquals(Math.round(Ma.errorRateOfSample(Ma.SampleSizeAccuracy._99, 50000, O.of(100000l)) * 10000), 41);//0.41%
    }

    @Test
    public void isInt() {
        assertTrue(Ma.isInt("1234567890"));
        assertTrue(Ma.isInt("1"));
        assertTrue(Ma.isInt("9"));

        assertFalse(Ma.isInt("1.1"));
        assertFalse(Ma.isInt("1234567890a"));
        assertFalse(Ma.isInt("a1234567890"));
        assertFalse(Ma.isInt("a"));
        assertFalse(Ma.isInt(""));
    }

    @Test
    public void isFloat() {
        assertTrue(Ma.isFloat("1"));
        assertTrue(Ma.isFloat("1"));
        assertTrue(Ma.isFloat("9"));
        assertTrue(Ma.isFloat("1.1"));
        assertTrue(Ma.isFloat("22221.12222"));

        assertFalse(Ma.isFloat("1."));
        assertFalse(Ma.isFloat(".1"));
        assertFalse(Ma.isFloat("1234567890a"));
        assertFalse(Ma.isFloat("1234567.890a"));
        assertFalse(Ma.isFloat("a1234567890"));
        assertFalse(Ma.isFloat("a123456,7890"));
        assertFalse(Ma.isFloat("a"));
        assertFalse(Ma.isFloat(""));
    }
}
