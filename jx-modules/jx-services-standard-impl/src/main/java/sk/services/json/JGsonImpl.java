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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import sk.services.bytes.IBytes;
import sk.services.time.ITime;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

@Log4j2
@NoArgsConstructor
@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType", "rawtypes"})
public class JGsonImpl implements IJson {

    @Inject Optional<List<GsonSerDesList>> converters = Optional.empty();
    @Inject ITime times;
    @Inject IBytes bytes;

    private Gson json;
    private Gson jsonPretty;

    public JGsonImpl(O<List<GsonSerDesList>> converters, ITime times) {
        this.converters = converters.toOpt();
        this.times = times;
    }

    @PostConstruct
    public JGsonImpl init() {
        List<GsonSerDes<?>> adaptClasses = converters
                .map($ -> $.stream().flatMap(x -> x.getSerDesInfoList().stream()).collect(Cc.toL()))
                .orElse(Cc.lEmpty());

        GsonBuilder gsonBuilder = new GsonBuilder();
        new GsonDefaultSerDes(times, bytes).getSerDesInfoList()
                .forEach($ -> gsonBuilder.registerTypeAdapter($.getCls(), $));

        adaptClasses.forEach((gsonAdapter) -> {
            gsonBuilder.registerTypeAdapter(gsonAdapter.getCls(), gsonAdapter);
        });

        json = gsonBuilder.disableHtmlEscaping().create();
        jsonPretty = gsonBuilder.disableHtmlEscaping().setPrettyPrinting().create();
        return this;
    }

    @Override
    @SneakyThrows
    public <T> String to(T object, boolean pretty) {
        return pretty ? jsonPretty.toJson(object) : json.toJson(object);
    }

    @Override
    public <T> T from(String objInJson, Class<T> cls) {
        try {
            return json.fromJson(objInJson, cls);
        } catch (Exception e) {
            throw new RuntimeException(St.raze3dots(objInJson, 500), e);
        }
    }

    @Override
    public <T> T from(InputStream objInJson, Class<T> cls) {
        try (InputStreamReader rdr = new InputStreamReader(objInJson)) {
            return json.fromJson(rdr, cls);
        } catch (Exception e) {
            throw new RuntimeException(St.raze3dots(St.streamToS(objInJson), 500), e);
        }
    }

    @Override
    public <T> T from(String objInJson, TypeWrap<T> type) {
        try {
            return json.fromJson(objInJson, type.getType());
        } catch (Exception e) {
            throw new RuntimeException(St.raze3dots(objInJson, 500), e);
        }
    }

    @Override
    public <T> T from(InputStream objInJson, TypeWrap<T> type) {
        try (InputStreamReader rdr = new InputStreamReader(objInJson)) {
            return json.fromJson(rdr, type.getType());
        } catch (Exception e) {
            throw new RuntimeException(St.raze3dots(St.streamToS(objInJson), 500), e);
        }
    }

    @Override
    public boolean validate(String possibleJson) {
        try {
            json.fromJson(possibleJson, Object.class);
            return true;
        } catch (com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }

    @Override
    @SneakyThrows
    public String beautify(String smallJson) {
        JsonElement jsonElement = JsonParser.parseString(smallJson);
        if (jsonElement.isJsonArray()) {
            return json.toJson(jsonElement.getAsJsonArray());
        } else if (jsonElement.isJsonObject()) {
            return json.toJson(jsonElement.getAsJsonObject());
        } else if (jsonElement.isJsonPrimitive()) {
            return json.toJson(jsonElement.getAsJsonPrimitive());
        }
        return smallJson;
    }

    @Override
    public <T> O<T> jsonPath(String jsonFull, String jsonPath, TypeWrap<T> tt) {
        try {
            T data = jsonPathParse(jsonFull).read(jsonPath, new TypeRefFromIJsonTypeWrap<>(tt));
            return O.ofNullable(data);
        } catch (Exception e) {
            return O.empty();
        }
    }

    @Override
    public <T> O<T> jsonPath(String jsonFull, F1<JsonPathContext, T> contextProvider) {
        try {
            T val = contextProvider.apply(new JsonPathContextImpl(jsonPathParse(jsonFull)));
            return O.ofNullable(val);
        } catch (Exception e) {
            return O.empty();
        }
    }

    @Override
    public O<String> jsonPathToJson(String jsonFull, String jsonPath, boolean pretty) {
        try {
            Object v = jsonPathParse(jsonFull).read(jsonPath);
            return O.ofNullable(to(v, pretty));
        } catch (Exception e) {
            return O.empty();
        }
    }

    private DocumentContext jsonPathParse(String jsonFull) {
        return JsonPath.parse(jsonFull, Configuration.builder().mappingProvider(new JacksonMappingProvider()).build());
    }


    private static class TypeRefFromIJsonTypeWrap<T> extends TypeRef<T> {
        TypeWrap<T> tw;

        public TypeRefFromIJsonTypeWrap(TypeWrap<T> tw) {
            this.tw = tw;
        }

        @Override
        public Type getType() {
            return tw.getType();
        }
    }

    private static class JsonPathContextImpl implements JsonPathContext {
        private final DocumentContext ctx;

        public JsonPathContextImpl(DocumentContext ctx) {this.ctx = ctx;}

        @Override
        public <T> T read(String path, TypeWrap<T> typeRef) {
            return ctx.read(path, new TypeRefFromIJsonTypeWrap<>(typeRef));
        }

        @Override
        public <T> T read(String path, Class<T> cls) {
            return ctx.read(path, cls);
        }

        @Override
        public String read(String path) {
            return ctx.read(path, new TypeRefFromIJsonTypeWrap<>(TypeWrap.simple(String.class)));
        }
    }
}
