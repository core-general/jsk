package sk.services.json;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import sk.utils.statics.St;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;

public class JGsonLoggingInputStreamReader extends InputStreamReader {
    private final StringBuilder forLog;
    private final int maxLimit;

    public JGsonLoggingInputStreamReader(InputStream in, int maxLimit) {
        super(in);
        this.maxLimit = maxLimit;
        this.forLog = new StringBuilder();
    }

    @Override
    public int read() throws IOException {
        int c = super.read();
        if (c != -1 && forLog.length() < maxLimit) {
            forLog.append((char) c);
        }
        return c;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int bytesRead = super.read(cbuf, off, len);
        if (bytesRead != -1) {
            if (forLog.length() < maxLimit) {
                forLog.append(cbuf, off, bytesRead);
            }
        }
        return bytesRead;
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        int initialPosition = target.position();
        int bytesRead = super.read(target);
        if (bytesRead != -1) {
            target.position(initialPosition);
            char[] cbuf = new char[bytesRead];
            target.get(cbuf);
            forLog.append(cbuf);
        }
        return bytesRead;
    }

    public String getForLog() {
        return St.raze3dots(forLog.toString(), maxLimit - 1);
    }
}
