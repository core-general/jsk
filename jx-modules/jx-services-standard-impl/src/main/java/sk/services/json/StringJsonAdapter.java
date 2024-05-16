package sk.services.json;

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

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.AllArgsConstructor;

import java.io.IOException;

public class StringJsonAdapter implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        return new __StringJsonAdapter<>(gson, type);
    }

    @AllArgsConstructor
    private static class __StringJsonAdapter<T> extends TypeAdapter<T> {
        private final Gson gson;
        private final TypeToken<T> type;

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            final String s = gson.toJson(value).replace("\n", "");
            gson.toJson(s, String.class, out);
        }

        @Override
        public T read(JsonReader in) throws IOException {
            final String s = in.nextString();
            return gson.fromJson(s, type);
        }
    }
    //public static void main(String[] args) {
    //    IJson json = CoreServicesRaw.services().json();
    //
    //    String json = """
    //            {
    //            "b":"{\\"x\\":\\"abc\\"}"
    //            }
    //            """;
    //
    //    final A from = js.from(json, A.class);
    //    final String to = js.to(from, true);
    //    final A from2 = js.from(json, A.class);
    //    System.out.println(to);
    //}
    //
    //private static class A {
    //    @JsonAdapter(sk.services.json.StringJsonAdapter.class)
    //    B b;
    //}
    //
    //private static class B {
    //    String x;
    //}
}

