package sk.services.shutdown;

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
import lombok.NoArgsConstructor;
import sk.services.json.IJson;
import sk.utils.functional.O;
import sk.utils.statics.Io;

import javax.inject.Inject;


/**
 * Storage which persists through JVM restarts. Not VM restarts!
 */
@AllArgsConstructor
@NoArgsConstructor
public class NodeRestartStorageImpl implements INodeRestartStorage {
    private final static String path = "/tmp/jsk/shutdown/storage";

    @Getter
    @Inject
    IJson json;

    @Override
    public O<byte[]> getBinaryAfterRestart(String fileName) {
        return Io.bRead(filePath(fileName)).oBytes();
    }

    @Override
    public void setDataForRestart(String fileName, byte[] binary) {
        Io.reWriteBin(filePath(fileName), w -> w.append(binary));
    }

    private String filePath(String fileName) {
        return "%s/%s".formatted(path, fileName);
    }
}
