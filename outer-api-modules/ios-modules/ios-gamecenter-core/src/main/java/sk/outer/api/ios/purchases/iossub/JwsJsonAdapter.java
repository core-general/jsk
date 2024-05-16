package sk.outer.api.ios.purchases.iossub;

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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import lombok.AllArgsConstructor;
import sk.services.CoreServicesRaw;
import sk.services.bytes.IBytes;
import sk.services.json.IJson;
import sk.utils.javafixes.TypeWrap;

import java.lang.reflect.Type;

@AllArgsConstructor
public class JwsJsonAdapter implements JsonDeserializer {
    //todo replace with inject
    private static final IJson json = CoreServicesRaw.services().json();
    private static final IBytes bytes = CoreServicesRaw.services().bytes();

    @Override
    public Object deserialize(JsonElement jj, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final String d3 = jj.getAsString();
        final String payload = new String(bytes.dec64(d3.split("\\.")[1]));
        return json.from(payload, TypeWrap.raw(typeOfT));
    }
}
