package sk.utils.javafixes.argparser;

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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Test;
import sk.utils.functional.O;

import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static sk.utils.asserts.JskAssert.checkCatchOrFail;
import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.of;
import static sk.utils.javafixes.argparser.ArgParserTest.Conf.*;
import static sk.utils.statics.Cc.ts;

public class ArgParserTest {
    @Test
    public void testParser() {
        ArgParser<Conf> parsed = ArgParser.parse(new String[]{"-a", "hello", "menly", "--clean", "cool"}, PARAM_A);
        assertEquals(parsed.getRequiredArg(PARAM_A), "hello");
        assertEquals(parsed.getRequiredArg(PARAM_B), "menly");
        assertEquals(parsed.getRequiredArg(PARAM_C), "cool");

        parsed = ArgParser.parse(new String[]{"menly", "--clean", "cool"}, PARAM_A);
        assertEquals(parsed.getArg(PARAM_A), empty());
        assertEquals(parsed.getRequiredArg(PARAM_B), "menly");
        assertEquals(parsed.getRequiredArg(PARAM_C), "cool");

        checkCatchOrFail(() -> ArgParser.parse(new String[]{"--clean", "cool"}, PARAM_A));
        checkCatchOrFail(() -> ArgParser.parse(new String[]{"-a", "hello", "menly"}, PARAM_A));
        checkCatchOrFail(() -> ArgParser.parse(new String[]{"-a", "hello", "menly", "--clean"}, PARAM_A));
        checkCatchOrFail(() -> ArgParser.parse(new String[]{"-a", "hello", "menly", "menly2"}, PARAM_A));
        checkCatchOrFail(() -> ArgParser.parse(new String[]{"-a", "hello", "menly", "menly2", "--clean", "cool"}, Conf.PARAM_A));

        try {
            ArgParser.parse(new String[]{"-a", "hello", "menly", "menly2", "--clean", "cool"}, Conf.PARAM_A);
        } catch (Exception e) {
            assertEquals(e.getMessage().trim(), """
                    Argument: "--all", "-a" desc: Param A, not mandatory
                    Required argument(no-arg): desc: Param B, mandatory, no arg
                    Required argument: "--clean", "-c" desc: Param C, mandatory
                    """.trim());
        }
    }

    @Getter
    @AllArgsConstructor
    public static enum Conf implements ArgParserConfigProvider<Conf> {
        PARAM_A(of(ts("-a", "--all")), false, "Param A, not mandatory"),
        PARAM_B(empty(), true, "Param B, mandatory, no arg"),
        PARAM_C(of(ts("-c", "--clean")), true, "Param C, mandatory");

        final O<TreeSet<String>> commandPrefix;
        final boolean required;
        final String description;

        @Override
        public Conf[] getArrConfs() {return values();}
    }
}
