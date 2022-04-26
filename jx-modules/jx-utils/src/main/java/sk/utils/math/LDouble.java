package sk.utils.math;

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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.exceptions.NotImplementedException;
import sk.utils.statics.Ma;
import sk.utils.statics.St;

import java.util.stream.IntStream;

/**
 *
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LDouble {
    @Getter
    private long decValueRaw;
    private int precisionDigits;
    private int precisionTens;

    @Override
    public String toString() {
        return toStringAsDouble();
    }

    public static LDouble createMilli(String milliVal) {
        return create(milliVal, 2);
    }

    public static LDouble createMilliRaw(long multipliedVal) {
        return createRaw(multipliedVal, 2);
    }

    public static LDouble createMilliRaw(double raw) {
        return createRaw(raw, 2);
    }

    public static LDouble create(String milliVal, int precisionBits) {
        return new LDouble(milliVal, precisionBits);
    }

    public static LDouble createRaw(long multipliedVal, int precisionBits) {
        return new LDouble(multipliedVal, precisionBits);
    }

    public static LDouble createRaw(double val, int precisionBits) {
        return new LDouble(val, precisionBits);
    }

    private LDouble(String realValue, int precisionDigits) {
        this.precisionDigits = precisionDigits;
        this.precisionTens = IntStream.range(0, precisionDigits).map($ -> 10).reduce(1, (left, right) -> left * right);
        final String[] split = realValue.split("\\.");
        final long[] vv = new long[2];
        vv[0] = Ma.pl(split[0]) * precisionTens;
        if (split.length > 1) {
            String rightPart = St.ss(split[1], 0, precisionDigits);
            rightPart = St.minSymbolsOtherwiseSuffix(rightPart, precisionDigits, "0");
            vv[1] = Ma.pl(rightPart);
        }
        this.decValueRaw = vv[0] + vv[1];
    }

    private LDouble(long decValueRaw, int precisionDigits) {
        this.decValueRaw = decValueRaw;
        this.precisionDigits = precisionDigits;
        this.precisionTens = IntStream.range(0, precisionDigits).map($ -> 10).reduce(1, (left, right) -> left * right);
    }

    private LDouble(double decValueRaw, int precisionDigits) {
        this.precisionTens = IntStream.range(0, precisionDigits).map($ -> 10).reduce(1, (left, right) -> left * right);
        final double val = decValueRaw * precisionTens;
        this.decValueRaw = Math.round(val);
        this.precisionDigits = precisionDigits;
    }

    public String toStringAsDouble() {
        String l = decValueRaw % precisionTens + "";
        return decValueRaw / precisionTens + "." + (l.length() == 2 ? l : "0" + l);
    }

    public double toDouble() {
        return decValueRaw / (1d * precisionTens);
    }

    public LDouble plus(long val) {
        decValueRaw += val * precisionTens;
        return this;
    }

    public LDouble minus(long val) {
        decValueRaw -= val * precisionTens;
        return this;
    }

    public LDouble mult(long val) {
        decValueRaw *= val;
        return this;
    }

    public LDouble div(long val) {
        decValueRaw /= val;
        return this;
    }

    public LDouble pow2() {
        decValueRaw = decValueRaw * decValueRaw;
        return this;
    }

    public LDouble plus(LDouble val) {
        checkPrecisionDigits(val);

        decValueRaw += val.decValueRaw;
        return this;
    }

    public LDouble minus(LDouble val) {
        checkPrecisionDigits(val);

        decValueRaw -= val.decValueRaw;
        return this;
    }

    public LDouble mult(LDouble val) {
        checkPrecisionDigits(val);

        decValueRaw *= val.decValueRaw;
        return this;
    }

    public LDouble div(LDouble val) {
        checkPrecisionDigits(val);

        decValueRaw /= val.decValueRaw;
        return this;
    }

    public LDouble plusImmutable(LDouble val) {
        checkPrecisionDigits(val);

        return new LDouble(
                decValueRaw + val.decValueRaw,
                precisionDigits,
                precisionTens
        );
    }

    public LDouble minusImmutable(LDouble val) {
        checkPrecisionDigits(val);

        return new LDouble(
                decValueRaw - val.decValueRaw,
                precisionDigits,
                precisionTens
        );
    }

    public LDouble multImmutable(LDouble val) {
        checkPrecisionDigits(val);

        return new LDouble(
                decValueRaw * val.decValueRaw,
                precisionDigits,
                precisionTens
        );
    }

    public LDouble divImmutable(LDouble val) {
        checkPrecisionDigits(val);

        return new LDouble(
                decValueRaw / val.decValueRaw,
                precisionDigits,
                precisionTens
        );
    }


    private void checkPrecisionDigits(LDouble val) {
        if (val.precisionDigits != this.precisionDigits) {
            //todo possible, but not yet implemented
            throw new NotImplementedException();
        }
    }
}
