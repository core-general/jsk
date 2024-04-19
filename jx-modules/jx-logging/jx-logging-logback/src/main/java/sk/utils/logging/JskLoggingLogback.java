package sk.utils.logging;

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

import sk.utils.functional.O;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;
import sk.utils.statics.Re;
import sk.utils.statics.St;

public class JskLoggingLogback implements JskLogging {
    private static String LOGBACK_FILE_PATTERN = "logback%s.xml";
    public static final String DEFAULT_LOGBACK = LOGBACK_FILE_PATTERN.formatted("");

    private final String folder;

    public JskLoggingLogback() {
        this("");
    }

    public JskLoggingLogback(String folder) {
        this.folder = folder == null ? "" : folder;
    }

    @Override
    public void prepare(String fileSuffix) {
        final String loggerClass = "ch.qos.logback.classic.Logger";
        O<Class<?>> classIfExist = Re.getClassIfExist(loggerClass);
        classIfExist.orElseThrow(() -> new IllegalArgumentException("Can't find logback classes in classpath:" + loggerClass));

        String actualLoggingFile = LOGBACK_FILE_PATTERN.formatted(fileSuffix);
        String fullFilePath = folder.isEmpty() ? "" : St.endWith(folder, "/") + LOGBACK_FILE_PATTERN.formatted(fileSuffix);

        if (Io.isResourceExists(fullFilePath)) {
            System.setProperty("logback.configurationFile", fullFilePath);
        } else {
            throw new IllegalArgumentException(
                    "Can't find logback configuration in file:" + fullFilePath +
                    "  with file suffix:'" + fileSuffix + "'");
        }

        //check that default logger does not exist in classpath
        if (!Fu.equal(DEFAULT_LOGBACK, actualLoggingFile)) {
            if (Io.isResourceExists(DEFAULT_LOGBACK)) {
                throw new IllegalArgumentException(
                        "Default logback.xml is in the root of classpath, it could be confused with actual log file:" +
                        fullFilePath);
            }
        }
    }
}
