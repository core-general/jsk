package sk.spring.plugins;

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

import sk.services.CoreServicesRaw;
import sk.services.bytes.BytesImpl;
import sk.services.bytes.IBytes;
import sk.services.json.IJson;
import sk.utils.statics.Io;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PropertyMetaService {

    public static synchronized void savePropertyNames(String pathPrefix, String propertyPathPrefix, List<String> properties,
            String module) {
        IJson json = CoreServicesRaw.services().json();
        final String to = json.to(new PropertyMeta(propertyPathPrefix, properties), true);
        Io.reWrite(pathPrefix + "/__jsk_util/properties/%s/props4spring.json".formatted(module), w -> w.append(to));
    }

    public static List<PropertyMeta> getPropertyNames() {
        IBytes bytes = new BytesImpl();
        IJson json = CoreServicesRaw.services().json();
        Map<String, byte[]> files = bytes.getResourceFolderRecursively("__jsk_util/properties");
        return files.entrySet().stream().filter($ -> $.getKey().endsWith(".json"))
                .map($ -> json.from(new String($.getValue(), StandardCharsets.UTF_8), PropertyMeta.class))
                .collect(Collectors.toList());
    }
}
