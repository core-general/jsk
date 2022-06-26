package sk.services.json.marcono1234.gson.recordadapter;

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
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

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
// Does not match com.google.gson.internal.bind.TypeAdapterRuntimeTypeWrapper behavior
// but instead always uses runtime type to be more deterministic
class RuntimeTypeTypeAdapter<T> extends TypeAdapter<T> {
    private final Gson gson;
    private final TypeAdapter<T> delegate;

    RuntimeTypeTypeAdapter(Gson gson, TypeAdapter<T> delegate) {
        this.gson = gson;
        this.delegate = delegate;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
            // Let compile time type adapter handle it; might write custom value
            delegate.write(out, null);
        } else {
            @SuppressWarnings("unchecked")
            TypeAdapter<T> adapter = (TypeAdapter<T>) gson.getAdapter(TypeToken.get(value.getClass()));
            adapter.write(out, value);
        }
    }

    @Override
    public T read(JsonReader in) throws IOException {
        return delegate.read(in);
    }
}
