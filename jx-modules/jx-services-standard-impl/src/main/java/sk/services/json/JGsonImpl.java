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

import com.google.gson.*;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import sk.services.bean.IServiceLocator;
import sk.services.bytes.IBytes;
import sk.services.json.typeadapterfactories.GsonIdBaseTypeAdapterFactory;
import sk.services.json.typeadapterfactories.GsonOptionalTypeAdapterFactory;
import sk.services.json.typeadapterfactories.GsonPostProcessTypeAdapterFactory;
import sk.services.json.typeadapterfactories.recordadapter.RecordTypeAdapterFactory;
import sk.services.time.ITime;
import sk.utils.functional.*;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.statics.Re;
import sk.utils.statics.St;
import sk.utils.tuples.X;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType", "rawtypes"})
public class JGsonImpl implements IJson {

    protected @Inject Optional<List<GsonSerDesList>> converters = Optional.empty();
    protected @Inject ITime times;
    protected @Inject IBytes bytes;
    protected @Inject Optional<IServiceLocator> serviceLocator = Optional.empty();

    protected Gson jsonConcrete;
    protected Gson jsonPolymorphic;

    protected Gson jsonSerializeNullPolymorphic;

    protected Gson jsonPrettyPolymorphic;

    protected Gson jsonPrettySerializeNullsPolymorphic;

    protected final O<ThreadLocal<IJsonInstanceProps>> tlRunStorage;

    public JGsonImpl(boolean useThreadLocalForRunStorage) {
        tlRunStorage = useThreadLocalForRunStorage ? O.of(new ThreadLocal<>()) : O.empty();
    }

    public JGsonImpl(O<List<GsonSerDesList>> converters, ITime times, IBytes bytes, boolean useThreadLocalForRunStorage) {
        this(useThreadLocalForRunStorage);
        this.converters = converters.toOpt();
        this.times = times;
        this.bytes = bytes;
    }

    @PostConstruct
    public JGsonImpl init() {
        class _Helper {
            public GsonBuilder cloneBuilder(GsonBuilder builder) {
                return builder.create().newBuilder();
            }

            public Gson addPolymorph(Gson nonPolymorph, GsonBuilder target) {
                return target
                        .registerTypeHierarchyAdapter(IJsonPolymorph.class, new PolySerDes(() -> nonPolymorph, JGsonImpl.this))
                        .create();
            }
        }
        _Helper $help = new _Helper();

        GsonBuilder concreteBuilder = new GsonBuilder();
        GsonBuilder polymorphBuilder = new GsonBuilder();

        GsonBuilder serializeNullsConcreteBuilder = new GsonBuilder();
        GsonBuilder serializeNullsPolymorphBuilder = new GsonBuilder();

        GsonBuilder prettyConcreteBuilder = new GsonBuilder();
        GsonBuilder prettyPolymorphBuilder = new GsonBuilder();

        GsonBuilder prettyAndNullConcreteBuilder = new GsonBuilder();
        GsonBuilder prettyAndNullPolymorphBuilder = new GsonBuilder();

        new GsonDefaultSerDes(times, bytes).getSerDesInfoList().forEach($ -> {
            concreteBuilder.registerTypeAdapter($.getCls(), $);
            polymorphBuilder.registerTypeAdapter($.getCls(), $);

            serializeNullsConcreteBuilder.registerTypeAdapter($.getCls(), $);
            serializeNullsPolymorphBuilder.registerTypeAdapter($.getCls(), $);

            prettyConcreteBuilder.registerTypeAdapter($.getCls(), $);
            prettyPolymorphBuilder.registerTypeAdapter($.getCls(), $);

            prettyAndNullConcreteBuilder.registerTypeAdapter($.getCls(), $);
            prettyAndNullPolymorphBuilder.registerTypeAdapter($.getCls(), $);
        });

        //region Optionals
        {
            final GsonOptionalTypeAdapterFactory factory = new GsonOptionalTypeAdapterFactory();
            concreteBuilder.registerTypeAdapterFactory(factory);
            polymorphBuilder.registerTypeAdapterFactory(factory);

            serializeNullsConcreteBuilder.registerTypeAdapterFactory(factory);
            serializeNullsPolymorphBuilder.registerTypeAdapterFactory(factory);

            prettyConcreteBuilder.registerTypeAdapterFactory(factory);
            prettyPolymorphBuilder.registerTypeAdapterFactory(factory);

            prettyAndNullConcreteBuilder.registerTypeAdapterFactory(factory);
            prettyAndNullPolymorphBuilder.registerTypeAdapterFactory(factory);
        }
        //endregion

        //region IdBase
        {
            final GsonIdBaseTypeAdapterFactory factory = new GsonIdBaseTypeAdapterFactory();
            concreteBuilder.registerTypeAdapterFactory(factory);
            polymorphBuilder.registerTypeAdapterFactory(factory);

            serializeNullsConcreteBuilder.registerTypeAdapterFactory(factory);
            serializeNullsPolymorphBuilder.registerTypeAdapterFactory(factory);

            prettyConcreteBuilder.registerTypeAdapterFactory(factory);
            prettyPolymorphBuilder.registerTypeAdapterFactory(factory);

            prettyAndNullConcreteBuilder.registerTypeAdapterFactory(factory);
            prettyAndNullPolymorphBuilder.registerTypeAdapterFactory(factory);
        }
        //endregion

        //region PostProcess
        {
            final GsonPostProcessTypeAdapterFactory factory = new GsonPostProcessTypeAdapterFactory(serviceLocator);
            concreteBuilder.registerTypeAdapterFactory(factory);
            polymorphBuilder.registerTypeAdapterFactory(factory);

            serializeNullsConcreteBuilder.registerTypeAdapterFactory(factory);
            serializeNullsPolymorphBuilder.registerTypeAdapterFactory(factory);

            prettyConcreteBuilder.registerTypeAdapterFactory(factory);
            prettyPolymorphBuilder.registerTypeAdapterFactory(factory);

            prettyAndNullConcreteBuilder.registerTypeAdapterFactory(factory);
            prettyAndNullPolymorphBuilder.registerTypeAdapterFactory(factory);
        }
        //endregion

        //region Records
        {
            final RecordTypeAdapterFactory factory = RecordTypeAdapterFactory.builder()
                    .allowMissingComponentValues()
                    .allowDuplicateComponentValues()
                    .create();
            concreteBuilder.registerTypeAdapterFactory(factory);
            polymorphBuilder.registerTypeAdapterFactory(factory);

            serializeNullsConcreteBuilder.registerTypeAdapterFactory(factory);
            serializeNullsPolymorphBuilder.registerTypeAdapterFactory(factory);

            prettyConcreteBuilder.registerTypeAdapterFactory(factory);
            prettyPolymorphBuilder.registerTypeAdapterFactory(factory);

            prettyAndNullConcreteBuilder.registerTypeAdapterFactory(factory);
            prettyAndNullPolymorphBuilder.registerTypeAdapterFactory(factory);
        }
        //endregion


        List<GsonSerDes<?>> adaptClasses = converters
                .map($ -> $.stream().flatMap(x -> x.getSerDesInfoList().stream()).collect(Cc.toL()))
                .orElse(Cc.lEmpty());
        adaptClasses.forEach((gsonAdapter) -> {
            gsonAdapter.setSerializerInstance(this);
            concreteBuilder.registerTypeAdapter(gsonAdapter.getCls(), gsonAdapter);
            polymorphBuilder.registerTypeAdapter(gsonAdapter.getCls(), gsonAdapter);

            serializeNullsConcreteBuilder.registerTypeAdapter(gsonAdapter.getCls(), gsonAdapter);
            serializeNullsPolymorphBuilder.registerTypeAdapter(gsonAdapter.getCls(), gsonAdapter);

            prettyConcreteBuilder.registerTypeAdapter(gsonAdapter.getCls(), gsonAdapter);
            prettyPolymorphBuilder.registerTypeAdapter(gsonAdapter.getCls(), gsonAdapter);

            prettyAndNullConcreteBuilder.registerTypeAdapter(gsonAdapter.getCls(), gsonAdapter);
            prettyAndNullPolymorphBuilder.registerTypeAdapter(gsonAdapter.getCls(), gsonAdapter);
        });

        concreteBuilder.disableHtmlEscaping();
        polymorphBuilder.disableHtmlEscaping();

        serializeNullsConcreteBuilder.disableHtmlEscaping().serializeNulls();
        serializeNullsPolymorphBuilder.disableHtmlEscaping().serializeNulls();

        prettyConcreteBuilder.disableHtmlEscaping().setPrettyPrinting();
        prettyPolymorphBuilder.disableHtmlEscaping().setPrettyPrinting();

        prettyAndNullConcreteBuilder.disableHtmlEscaping().serializeNulls().setPrettyPrinting();
        prettyAndNullPolymorphBuilder.disableHtmlEscaping().serializeNulls().setPrettyPrinting();


        jsonConcrete = $help.cloneBuilder(concreteBuilder).create();
        jsonPolymorphic = $help.addPolymorph(jsonConcrete, $help.cloneBuilder(polymorphBuilder));

        Gson jsonSerializeNullsConcrete = $help.cloneBuilder(serializeNullsConcreteBuilder).create();
        jsonSerializeNullPolymorphic =
                $help.addPolymorph(jsonSerializeNullsConcrete, $help.cloneBuilder(serializeNullsPolymorphBuilder));

        Gson jsonPrettyConcrete = $help.cloneBuilder(prettyConcreteBuilder).create();
        jsonPrettyPolymorphic = $help.addPolymorph(jsonPrettyConcrete, $help.cloneBuilder(prettyPolymorphBuilder));

        Gson jsonPrettySerializeNullsConcrete = $help.cloneBuilder(prettyAndNullConcreteBuilder).create();
        jsonPrettySerializeNullsPolymorphic =
                $help.addPolymorph(jsonPrettySerializeNullsConcrete, $help.cloneBuilder(prettyAndNullPolymorphBuilder));

        return this;
    }

    @Override
    public <T> String to(T object, boolean pretty, boolean serializeNulls) {
        try {
            Gson selectedGson = pretty && serializeNulls
                                ? jsonPrettySerializeNullsPolymorphic
                                : pretty
                                  ? jsonPrettyPolymorphic
                                  : serializeNulls
                                    ? jsonSerializeNullPolymorphic
                                    : jsonPolymorphic;
            tlRunStorage.ifPresent($ -> $.set(new IJsonInstanceProps(selectedGson.serializeNulls())));

            return selectedGson.toJson(object);
        } finally {
            tlRunStorage.ifPresent($ -> $.remove());
        }
    }

    @Override
    public <T> T from(String objInJson, Class<T> cls) {
        return fromUni(jsonPolymorphic, new ByteArrayInputStream(objInJson.getBytes(St.UTF8)), TypeWrap.simple(cls));
    }

    @Override
    public <T> T fromWithNulls(String objInJson, Class<T> cls) {
        return fromUni(jsonSerializeNullPolymorphic, new ByteArrayInputStream(objInJson.getBytes(St.UTF8)), TypeWrap.simple(cls));
    }

    @Override
    public <T> T from(InputStream objInJson, Class<T> cls) {
        return fromUni(jsonPolymorphic, objInJson, TypeWrap.simple(cls));
    }

    @Override
    public <T> T from(String objInJson, TypeWrap<T> type) {
        return fromUni(jsonPolymorphic, new ByteArrayInputStream(objInJson.getBytes(St.UTF8)), type);
    }

    @Override
    public <T> T fromWithNulls(String objInJson, TypeWrap<T> type) {
        return fromUni(jsonSerializeNullPolymorphic, new ByteArrayInputStream(objInJson.getBytes(St.UTF8)), type);
    }

    @Override
    public <T> T from(InputStream objInJson, TypeWrap<T> type) {
        return fromUni(jsonPolymorphic, objInJson, type);
    }

    @Override
    public boolean validate(String possibleJson) {
        try {
            jsonPolymorphic.fromJson(possibleJson, Object.class);
            return true;
        } catch (com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }

    @Override
    public boolean validateWithNulls(String possibleJson) {
        try {
            jsonSerializeNullPolymorphic.fromJson(possibleJson, Object.class);
            return true;
        } catch (com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }

    @Override
    public String beautify(String smallJson) {
        JsonElement jsonElement = JsonParser.parseString(smallJson);
        if (jsonElement.isJsonArray()) {
            return jsonPrettyPolymorphic.toJson(jsonElement.getAsJsonArray());
        } else if (jsonElement.isJsonObject()) {
            return jsonPrettyPolymorphic.toJson(jsonElement.getAsJsonObject());
        } else if (jsonElement.isJsonPrimitive()) {
            return jsonPrettyPolymorphic.toJson(jsonElement.getAsJsonPrimitive());
        }
        return smallJson;
    }

    @Override
    public String beautifyWithNulls(String smallJson) {
        JsonElement jsonElement = JsonParser.parseString(smallJson);
        if (jsonElement.isJsonArray()) {
            return jsonPrettySerializeNullsPolymorphic.toJson(jsonElement.getAsJsonArray());
        } else if (jsonElement.isJsonObject()) {
            return jsonPrettySerializeNullsPolymorphic.toJson(jsonElement.getAsJsonObject());
        } else if (jsonElement.isJsonPrimitive()) {
            return jsonPrettySerializeNullsPolymorphic.toJson(jsonElement.getAsJsonPrimitive());
        } else if (jsonElement.isJsonNull()) {
            return jsonPrettySerializeNullsPolymorphic.toJson(jsonElement.getAsJsonNull());
        }
        return smallJson;
    }

    @Override
    public O<IJsonInstanceProps> getCurrentInvocationProps() {
        return tlRunStorage.map($ -> O.ofNull($.get()))
                .orElseThrow(() -> new RuntimeException("Run storage is disabled for your IJson!"));
    }

    @Override
    public <T> OneOf<T, Exception> jsonPath(String jsonFull, String jsonPath, TypeWrap<T> tt) {
        try {
            T data = jsonPathParse(jsonFull).read(jsonPath, new TypeRefFromIJsonTypeWrap<>(tt));
            return OneOf.left(data);
        } catch (Exception e) {
            return OneOf.right(e);
        }
    }

    @Override
    public <T> OneOf<T, Exception> jsonPath(String jsonFull, F1<JsonPathContext, T> contextProvider) {
        try {
            T val = contextProvider.apply(new JsonPathContextImpl(jsonPathParse(jsonFull)));
            return OneOf.left(val);
        } catch (Exception e) {
            return OneOf.right(e);
        }
    }

    @Override
    public OneOf<String, Exception> jsonPathToJson(String jsonFull, String jsonPath, boolean pretty) {
        try {
            Object v = jsonPathParse(jsonFull).read(jsonPath);
            return OneOf.left(to(v, pretty));
        } catch (Exception e) {
            return OneOf.right(e);
        }
    }

    @Override
    public IBytes getIBytes() {
        return bytes;
    }

    private DocumentContext jsonPathParse(String jsonFull) {
        final Configuration build = Configuration.builder()
                .jsonProvider(new GsonJsonProvider(jsonPolymorphic))
                .mappingProvider(new GsonMappingProvider(jsonPolymorphic))
                .build();
        return JsonPath.parse(jsonFull, build);
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

    private ConcurrentHashMap<String, Reflections> typesToReflections = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Class<?>> classNamesToActualClasses = new ConcurrentHashMap<>();

    @Override
    public Class<?> getClassByClassName(F0<String> jsonOfType, String className, Type parentType) {
        try {
            return IJson.super.getClassByClassName(jsonOfType, className, parentType);
        } catch (ClassNotFoundException e) {
            final Class<?> aClass = classNamesToActualClasses.computeIfAbsent(className, (___) -> {
                final String typeName = parentType.getTypeName();
                final String targetPackage = St.subRF(typeName, ".");
                final Reflections searcher =
                        typesToReflections.computeIfAbsent(targetPackage,
                                (__) -> new Reflections(targetPackage, Scanners.SubTypes));

                final Class parentClass = (Class) parentType;
                Set<Class<?>> subTypesOf = searcher.getSubTypesOf(parentClass);
                if (!Modifier.isAbstract(parentClass.getModifiers())) {
                    subTypesOf = Cc.add(new HashSet<>(subTypesOf), parentClass);
                }
                O<Class<?>> sureClass = testClassByName100Percent(className, subTypesOf);
                if (sureClass.isPresent()) {
                    return sureClass.get();
                }

                sureClass = testClassByParameters100Percent(subTypesOf, jsonOfType);
                if (sureClass.isPresent()) {
                    return sureClass.get();
                }

                sureClass = testClassByNameAndParametersHeuristics(parentClass, jsonOfType);
                if (sureClass.isPresent()) {
                    return sureClass.get();
                }
                throw new RuntimeException("\nClass not found:\n%s\n%s\n%s".formatted(className, parentType, jsonOfType.get()));
            });
            return aClass;
        }
    }

    private O<Class<?>> testClassByNameAndParametersHeuristics(Class parentType, F0<String> jsonOfType) {
        return O.empty();//todo might be a good idea to implement
    }

    private O<Class<?>> testClassByParameters100Percent(Set<Class<?>> parentType, F0<String> jsonOfType) {
        //todo!! Issue here - Null jsonWithNull is not applied, needs to be implemented
        final Map<String, ?> map = jsonConcrete.fromJson(jsonOfType.get(), Map.class);
        final Set<String> parameterNames = map.keySet();

        var x2s = parentType.stream().map($ -> {
                    final Set<String> classParameterNames = Re.getAllNonStaticFields($).stream()
                            .map($$ -> $$.getName())
                            .collect(Collectors.toSet());
                    return X.x($, Fu.equal(parameterNames, classParameterNames));
                }).filter($ -> $.i2())
                .toList();
        if (x2s.size() == 1) {
            return O.of(x2s.get(0).i1());
        } else {
            return O.empty();
        }
    }

    private O<Class<?>> testClassByName100Percent(String className, Set<Class<?>> subTypesOf) {
        final List<Class<?>> classes = subTypesOf.stream()
                .filter($ -> Fu.equal(St.subLL($.getSimpleName(), "$")/*for inner classes*/,
                        St.subLL(St.subLL(className, "."/*package*/), "$"/*for inner classes*/)))
                .toList();
        if (classes.size() == 1) {
            return O.of(classes.get(0));
        } else {
            return O.empty();
        }
    }

    protected <T> T fromUni(Gson gson, InputStream in, TypeWrap<T> type) {
        JGsonLoggingInputStreamReader rdr = new JGsonLoggingInputStreamReader(in, 50_000);
        try (rdr) {
            tlRunStorage.ifPresent($ -> $.set(new IJsonInstanceProps(gson.serializeNulls())));
            return gson.fromJson(rdr, type.getType());
        } catch (Exception e) {
            throw new RuntimeException(rdr.getForLog(), e);
        } finally {
            tlRunStorage.ifPresent($ -> $.remove());
        }
    }

    private static class PolySerDes extends GsonSerDes<IJsonPolymorph> {
        F0<Gson> nonPolymorphicGson;
        IJsonPolymorphReader ijpr;

        public PolySerDes(F0<Gson> nonPolymorphicGson, IJsonPolymorphReader ijpr) {
            super(IJsonPolymorph.class);
            this.nonPolymorphicGson = nonPolymorphicGson;
            this.ijpr = ijpr;
        }

        @Override
        public JsonElement serialize(IJsonPolymorph src, Type typeOfSrc, JsonSerializationContext context) {
            src.setMyType();
            final JsonElement toRet = nonPolymorphicGson.get().toJsonTree(src);
            src.clearMyType();
            return toRet;
        }

        @Override
        public IJsonPolymorph deserialize(JsonElement json, Type parentType, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                final JsonObject obj = json.getAsJsonObject();
                final String clsName = obj.get(ijpr.getClassFieldName()).getAsString();
                final Class<?> classByClassName = ijpr.getClassByClassName(new Lazy<>(() -> obj.toString()), clsName, parentType);
                final IJsonPolymorph iJsonPolymorph = (IJsonPolymorph) nonPolymorphicGson.get().fromJson(json, classByClassName);
                iJsonPolymorph.clearMyType();
                return iJsonPolymorph;
            } catch (Exception e) {
                throw new RuntimeException("Can't process json element: " + json.toString() + " for type " + parentType, e);
            }
        }
    }
}
