package sk.utils.javafixes;

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

import org.junit.jupiter.api.Test;

import java.io.CharConversionException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PartialClassTreeTest {
    PartialClassTree pct = new PartialClassTree();

    @Test
    public void add() {

        addAndCheck(RuntimeException.class, """
                ROOT
                |- java.lang.RuntimeException""", Exception.class, null);

        addAndCheck(Exception.class, """
                ROOT
                |- java.lang.Exception
                   |- java.lang.RuntimeException""", Exception.class, Exception.class);

        addAndCheck(NullPointerException.class, """
                ROOT
                |- java.lang.Exception
                   |- java.lang.RuntimeException
                      |- java.lang.NullPointerException""", IOException.class, Exception.class);

        addAndCheck(OutOfMemoryError.class, """
                ROOT
                |- java.lang.OutOfMemoryError
                |- java.lang.Exception
                   |- java.lang.RuntimeException
                      |- java.lang.NullPointerException""", Throwable.class, null);

        addAndCheck(CharConversionException.class, """
                ROOT
                |- java.lang.OutOfMemoryError
                |- java.lang.Exception
                   |- java.io.CharConversionException
                   |- java.lang.RuntimeException
                      |- java.lang.NullPointerException""", NullPointerException.class, NullPointerException.class);

        addAndCheck(IOException.class, """
                ROOT
                |- java.lang.OutOfMemoryError
                |- java.lang.Exception
                   |- java.io.IOException
                      |- java.io.CharConversionException
                   |- java.lang.RuntimeException
                      |- java.lang.NullPointerException""", ArithmeticException.class, RuntimeException.class);

        addAndCheck(Throwable.class, """
                ROOT
                |- java.lang.Throwable
                   |- java.lang.OutOfMemoryError
                   |- java.lang.Exception
                      |- java.io.IOException
                         |- java.io.CharConversionException
                      |- java.lang.RuntimeException
                         |- java.lang.NullPointerException""", Error.class, Throwable.class);
    }

    private void addAndCheck(Class<?> cls, String treeAfter) {
        pct.add(cls);
        assertEquals(treeAfter, pct.toString());
    }

    private void addAndCheck(Class<?> cls, String treeAfter, Class<?> clsToCheck, Class<?> nearestParent) {
        addAndCheck(cls, treeAfter);
        assertEquals(pct.getNearestParentTo(clsToCheck).orElse(null), nearestParent);
    }
}
