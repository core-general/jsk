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

import sk.services.json.IJson;
import sk.utils.functional.O;
import sk.utils.statics.St;

import java.nio.charset.StandardCharsets;

public interface INodeRestartStorage {
    public IJson getJson();

    public O<byte[]> getBinaryAfterRestart(String fileName);

    default public O<String> getStringAfterRestart(String fileName) {
        return getBinaryAfterRestart(fileName).map($ -> new String($, St.UTF8));
    }

    default public <T> O<T> getDataAfterRestart(String fileName, Class<T> cls) {
        return getStringAfterRestart(fileName).map($ -> getJson().from($, cls));
    }

    public void setDataForRestart(String fileName, byte[] binary);

    default public void setDataForRestart(String fileName, String data) {
        setDataForRestart(fileName, data.getBytes(StandardCharsets.UTF_8));
    }

    default public <T> void setDataForRestart(String fileName, T object) {
        setDataForRestart(fileName, getJson().to(object));
    }
}
