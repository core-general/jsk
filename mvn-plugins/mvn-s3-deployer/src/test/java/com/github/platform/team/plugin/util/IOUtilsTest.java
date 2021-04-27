package com.github.platform.team.plugin.util;

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

import java.io.*;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class IOUtilsTest {

    @Test
    public void copy() throws Exception {
        // GIVEN
        String expected = "expected";
        InputStream in = new ByteArrayInputStream(expected.getBytes());
        OutputStream out = new ByteArrayOutputStream();

        // WHEN
        IOUtils.copy(in, out);

        // THEN
        assertThat(expected, equalTo(out.toString()));
    }

    @Test
    public void closeQuietlyIfNoCloseables() {
        // GIVEN

        // WHEN
        IOUtils.closeQuietly();

        // THEN
        // no exceptions
    }

    @Test
    public void closeQuietlyIfCloseablesIsEmpty() {
        // GIVEN
        Closeable[] closeables = new Closeable[1];

        // WHEN
        IOUtils.closeQuietly(closeables);

        // THEN
        // no exceptions
    }

    @Test
    public void closeQuietlyIfCloseablesThrowsException() throws Exception {
        // GIVEN
        Closeable closeables = mock(Closeable.class);
        doThrow(IOException.class).when(closeables).close();

        // WHEN
        IOUtils.closeQuietly(closeables);

        // THEN
        // no exceptions
    }

    @Test
    public void closeQuietly() throws Exception {
        // GIVEN
        Closeable closeables = mock(Closeable.class);

        // WHEN
        IOUtils.closeQuietly(closeables);

        // THEN
        // no exceptions
    }
}
