package sk.services.json;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import sk.services.bytes.AesType;
import sk.services.bytes.IBytes;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.TypeWrap;

import java.io.InputStream;

@SuppressWarnings("unused")
public interface IJson extends IJsonPolymorphReader {
    default <T> String to(T object) {
        return to(object, false, false);
    }

    default <T> String toPretty(T object) {
        return to(object, true, false);
    }

    default <T> String toWithNulls(T object) {
        return to(object, false, true);
    }

    default <T> String to(T object, boolean pretty) {
        return to(object, pretty, false);
    }

    <T> String to(T object, boolean pretty, boolean serializeNulls);

    <T> T from(String json, Class<T> cls);

    <T> T fromWithNulls(String objInJson, Class<T> cls);

    <T> T from(String json, TypeWrap<T> type);

    <T> T fromWithNulls(String objInJson, TypeWrap<T> type);

    <T> T from(InputStream json, Class<T> cls);

    <T> T from(InputStream json, TypeWrap<T> type);

    boolean validate(String possibleJson);

    boolean validateWithNulls(String possibleJson);

    <T> String beautify(String smallJson);

    String beautifyWithNulls(String smallJson);

    O<IJsonInstanceProps> getCurrentInvocationProps();

    default <T> byte[] encryptAesObject(String password, T object, AesType type) {
        return getIBytes().encryptAes(password, to(object), type);
    }

    default <T> T decryptAesObject(String password, byte[] encrypted, Class<T> cls) {
        return from(new String(getIBytes().decryptAes(password, encrypted)), cls);
    }

    default <T> T decryptAesObject(String password, byte[] encrypted, TypeWrap<T> cls) {
        return from(new String(getIBytes().decryptAes(password, encrypted)), cls);
    }

    <T> OneOf<T, Exception> jsonPath(String jsonFull, String jsonPath, TypeWrap<T> tt);

    <T> OneOf<T, Exception> jsonPath(String jsonFull, F1<JsonPathContext, T> contextProvider);

    OneOf<String, Exception> jsonPathToJson(String jsonFull, String pathInJson, boolean pretty);

    IBytes getIBytes();

    interface JsonPathContext {
        <T> T read(String path, TypeWrap<T> typeRef);

        <T> T read(String path, Class<T> cls);

        String read(String path);
    }
}
