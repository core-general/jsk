package sk.services.json.typeadapterfactories.recordadapter;

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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

/*
https://github.com/Marcono1234/gson-record-type-adapter-factory
MIT License

Copyright (c) 2021 Marcono1234

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
// Similar to com.google.gson.internal.bind.TreeTypeAdapter
class TreeTypeAdapter<T> extends TypeAdapter<T> {
    private final JsonSerializer<T> serializer;
    private final JsonDeserializer<T> deserializer;
    private final Gson gson;
    private final TypeToken<T> type;
    private final TypeAdapter<JsonElement> jsonElementAdapter;
    private final GsonContext context;

    // Looked up lazily to avoid exceptions during lookup when delegate is not actually needed
    private volatile TypeAdapter<T> delegate;

    TreeTypeAdapter(JsonSerializer<T> serializer, JsonDeserializer<T> deserializer, Gson gson, TypeToken<T> type) {
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.gson = gson;
        this.type = type;
        this.jsonElementAdapter = gson.getAdapter(JsonElement.class);
        this.context = new GsonContext(gson);
    }

    private TypeAdapter<T> delegate() {
        TypeAdapter<T> delegate = this.delegate;
        // Allow racy initialization by multiple threads
        if (delegate == null) {
            this.delegate = delegate = gson.getAdapter(type);
        }
        return delegate;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (serializer == null) {
            delegate().write(out, value);
        } else {
            // Not exactly the same as Gson's TreeTypeAdapter implementation, but close enough
            // Can't use JsonParser.parseReader because it makes the reader lenient
            JsonElement jsonElement = serializer.serialize(value, type.getType(), context);
            jsonElementAdapter.write(out, jsonElement);
        }
    }

    @Override
    public T read(JsonReader in) throws IOException {
        if (deserializer == null) {
            return delegate().read(in);
        } else {
            // Not exactly the same as Gson's TreeTypeAdapter implementation, but close enough
            JsonElement jsonElement = jsonElementAdapter.read(in);
            return deserializer.deserialize(jsonElement, type.getType(), context);
        }
    }

    private static class GsonContext implements JsonSerializationContext, JsonDeserializationContext {
        private final Gson gson;

        private GsonContext(Gson gson) {
            this.gson = gson;
        }

        @Override
        public <T> T deserialize(JsonElement json, Type typeOfT) throws JsonParseException {
            return gson.fromJson(json, typeOfT);
        }

        @Override
        public JsonElement serialize(Object src) {
            return gson.toJsonTree(src);
        }

        @Override
        public JsonElement serialize(Object src, Type typeOfSrc) {
            return gson.toJsonTree(src, typeOfSrc);
        }
    }
}
