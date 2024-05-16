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
import sk.services.json.IJson;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Io;

import java.util.List;

public class PropertyMetaService {

    public static synchronized void savePropertyNames(String pathPrefix, String propertyPathPrefix, List<String> properties) {
        IJson json = CoreServicesRaw.services().json();
        final String to = json.to(new PropertyMeta(propertyPathPrefix, properties), true);
        Io.reWrite(pathPrefix + "/__jsk_util/properties/props4spring.json", w -> w.append(to));
    }

    public static O<PropertyMeta> getPropertyNames() {
        final String properties = Io.getResource("__jsk_util/properties/props4spring.json")
                .orElseThrow(() -> new RuntimeException(
                        "Can't find property meta file in:__jsk_util/properties/props4spring.json"));
        IJson json = CoreServicesRaw.services().json();
        return O.of(json.from(properties, TypeWrap.simple(PropertyMeta.class)));
    }
}
