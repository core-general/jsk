package sk.web.swagger;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.core.jackson.mixin.MediaTypeMixin;
import io.swagger.v3.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.ObjectMapperFactory;
import lombok.*;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.functional.OneOrBoth;
import sk.utils.ids.IdBase;
import sk.utils.ids.IdLong;
import sk.utils.ids.IdString;
import sk.utils.ids.IdUuid;
import sk.utils.statics.*;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;
import sk.web.WebMethodType;
import sk.web.infogatherer.WebClassInfo;
import sk.web.infogatherer.WebClassInfoProvider;
import sk.web.infogatherer.WebMethodInfo;
import sk.web.utils.WebApiMethod;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.of;

@AllArgsConstructor
public class WebSwaggerGenerator {
    private final WebClassInfoProvider infoProvider;

    @SneakyThrows
    public <A> String generateSwaggerSpec(Class<A> apiClass, O<String> basePath) {
        OpenAPI spec = new OpenAPI();

        spec.info(new Info().description("").version("1.0.0").title(apiClass.getName()));

        final Paths paths = new Paths();
        spec.setPaths(paths);


        Map<Class<?>, ClassInfoGenericInfoRaw> classesForComponents = Cc.m();
        final WebClassInfo classModel = infoProvider.getClassModel(apiClass, basePath);
        for (WebMethodInfo method : classModel.getMethods()) {
            final WebApiMethod<?> webApiMethod = new WebApiMethod<>(apiClass, of(method.getMethod()), false);
            fillOneMethod(paths, method, webApiMethod, classesForComponents);
        }


        //prepare components
        Map<Class<?>, X2<String, Schema>> finishedClasses = Cc.m();
        while (finishedClasses.size() != classesForComponents.size()) {
            Map<Class<?>, ClassInfoGenericInfoRaw> clone = new HashMap<>(classesForComponents);
            for (Class<?> toProcess : clone.keySet()) {
                if (!finishedClasses.containsKey(toProcess)) {
                    final X2<String, Schema> stringSchemaX2 = clone.get(toProcess).toSchema(classesForComponents);
                    finishedClasses.put(toProcess, stringSchemaX2);
                }
            }
        }
        final Components components = new Components();
        components.setSchemas(finishedClasses.values().stream().collect(Cc.toMX2()));
        spec.setComponents(components);


        final ObjectMapper json = ObjectMapperFactory.createJson()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .addMixIn(Schema.class, SchemaMixin.class)
                .addMixIn(MediaType.class, MediaTypeMixin.class)
                .addMixIn(Parameter.class, ParameterMixin.class)
                .addMixIn(Header.class, ParameterMixin.class)
                .registerModule(
                        new SimpleModule()
                                .addSerializer(SecurityScheme.Type.class, new ToStringSerializer())
                                .addSerializer(SecurityScheme.In.class, new ToStringSerializer())
                                .addSerializer(Parameter.StyleEnum.class, new ToStringSerializer())
                );

        return json.writerWithDefaultPrettyPrinter().writeValueAsString(spec);
    }

    private static void fillOneMethod(Paths paths, WebMethodInfo method,
            WebApiMethod<?> webApiMethod, Map<Class<?>, ClassInfoGenericInfoRaw> classesForComponents) {
        final PathItem item = new PathItem();
        Operation op = new Operation();

        switch (method.getType()) {
            case GET:
                item.setGet(op);
                break;
            case POST_MULTI_SURE:
            case POST_MULTI:
            case POST_FORM:
            case POST_BODY:
                item.setPost(op);
                break;
        }

        op.setOperationId(method.getPrecompiledModel().getMName());

        String responseMediaType = byte[].class.equals(method.getMethod().getReturnType())
                                   ? "application/octet-stream"
                                   : "application/json";

        op.setResponses(new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("")
                        .content(new Content().addMediaType(responseMediaType,
                                new MediaType()
                                        .schema(convertToSchema(method.getMethod().getReturnType(), classesForComponents))))));

        prepareParameters(method, classesForComponents, op);

        paths.addPathItem(St.startWith(method.getFullMethodPath(), "/"), item);
    }

    private static void prepareParameters(WebMethodInfo method, Map<Class<?>, ClassInfoGenericInfoRaw> classesForComponents,
            Operation op) {
        final List<Parameter> parameters = Cc.mapEachWithIndex(method.getPrecompiledModel().getParams(), ($, i) -> {
            Parameter p = new Parameter();
            p.in("query");

            final java.lang.reflect.Parameter pp = method.getMethod().getParameters()[i];
            final TypeInfoGenericInfoRaw typeGenericInfoRaw = getTypeGenericInfoRaw(pp.getParameterizedType());

            p.setRequired(!typeGenericInfoRaw.getType().isOptional());
            p.setName($.getName());
            p.setSchema(convertToSchema(pp.getParameterizedType(), classesForComponents));
            return p;
        });

        if (method.getType() == WebMethodType.POST_MULTI || method.getType() == WebMethodType.POST_MULTI_SURE) {
            final Schema schema = new Schema();
            schema.setType("object");
            schema.setProperties(parameters.stream().map($ -> X.x($.getName(), $.getSchema())).collect(Cc.toMX2()));
            schema.setRequired(
                    parameters.stream().filter($ -> Fu.isTrue($.getRequired())).map($ -> $.getName()).collect(Cc.toL()));
            op.setRequestBody(new RequestBody()
                    .content(new Content().addMediaType("multipart/form-data", new MediaType()
                            .schema(schema))));

        } else if (method.getType() == WebMethodType.POST_BODY) {
            op.setRequestBody(new RequestBody()
                    .content(new Content().addMediaType("application/octet-stream", new MediaType()
                            .schema(new BinarySchema()))));
        } else {
            op.setParameters(parameters);
        }
    }

    private static Schema<?> convertToSchema(Type type, Map<Class<?>, ClassInfoGenericInfoRaw> classesForComponents) {
        return convertToSchema(getTypeGenericInfoRaw(type), classesForComponents);
    }


    private static Schema<?> convertToSchema(TypeInfoGenericInfoRaw typeGenericInfoRaw,
            Map<Class<?>, ClassInfoGenericInfoRaw> classesForComponents) {
        final SwaggerType tt = typeGenericInfoRaw.getType();

        final O<Class<?>> classForSchema = tt.getClassForSchema();
        if (classForSchema.isPresent()) {
            Schema<?> sc = new Schema<>();
            final Class<?> aClass = classForSchema.get();
            classesForComponents.computeIfAbsent(aClass, (k) -> getClassInfo(aClass));
            sc.set$ref("#/components/schemas/" + aClass.getSimpleName());
            return sc;
        }

        {
            final O<OneOrBoth<Class<?>, SwaggerType>> arrClass = tt.getArrayClassForSchema();
            if (arrClass.isPresent()) {
                ArraySchema sc = new ArraySchema();
                if (arrClass.get().isLeft()) {
                    sc.setItems(convertToSchema(arrClass.get().left(), classesForComponents));
                } else {
                    sc.setItems(new Schema()
                            .type(arrClass.get().right().getType())
                            .format(arrClass.get().right().getFormat().orElse(null)));
                }
                return sc;
            }
        }

        Schema<?> sc = new Schema<>();
        sc.setType(tt.getType());
        tt.getFormat().ifPresent($ -> sc.setFormat($));
        {
            final O<OneOrBoth<Class<?>, SwaggerType>> mapClass = tt.getMapClassForSchema();
            if (mapClass.isPresent()) {
                if (mapClass.get().isLeft()) {
                    sc.setAdditionalProperties(convertToSchema(mapClass.get().left(), classesForComponents));
                } else {
                    sc.setAdditionalProperties(new Schema()
                            .type(mapClass.get().right().getType())
                            .format(mapClass.get().right().getFormat().orElse(null)));
                }
            }
        }
        return sc;
    }

    private static ClassInfoGenericInfoRaw getClassInfo(Class<?> type) {
        if (type.isEnum()) {
            return new ClassInfoGenericInfoRaw(type.getSimpleName(),
                    OneOf.right(Cc.stream(type.getEnumConstants()).map($ -> ((Enum) $).name()).collect(Collectors.toList())));
        } else {
            final SortedSet<Field> fields = Re.getAllNonStaticFields(type);

            final List<FieldInfoGenericInfoRaw> collect = fields.stream()
                    .map($ -> new FieldInfoGenericInfoRaw($.getName(), getTypeGenericInfoRaw($.getGenericType())))
                    .collect(Cc.toL());

            return new ClassInfoGenericInfoRaw(type.getSimpleName(), OneOf.left(collect));
        }
    }

    private static TypeInfoGenericInfoRaw getTypeGenericInfoRaw(Type type) {
        TypeInfoGenericInfoRaw cir = new TypeInfoGenericInfoRaw();

        final String name = St.subRF(type.getTypeName(), "<");
        cir.setClassName(name);
        cir.setJavaType(type);

        if (type instanceof Class && IdBase.class.isAssignableFrom((Class<?>) type)) {
            if (IdLong.class.isAssignableFrom((Class) type)) {
                cir.setType(SwaggerType.simpleWithFormat("integer", "int64"));
            } else if (IdString.class.isAssignableFrom((Class) type)) {
                cir.setType(SwaggerType.simple("string"));
            } else if (IdUuid.class.isAssignableFrom((Class) type)) {
                cir.setType(SwaggerType.simpleWithFormat("string", "uuid"));
            }
        } else if (type instanceof ParameterizedType) {

            final List<TypeInfoGenericInfoRaw> collect = Cc.stream(((ParameterizedType) type).getActualTypeArguments())
                    .map($ -> getTypeGenericInfoRaw($))
                    .collect(Cc.toL());
            //cir.setTypeParameters(O.of(collect));

            if (name.startsWith("java.util.Optional") || name.startsWith("sk.utils.functional.O")) {
                final TypeInfoGenericInfoRaw typeInfoGenericInfoRaw = Cc.first(collect).get();
                typeInfoGenericInfoRaw.getType().setOptional(true);
                return typeInfoGenericInfoRaw;
            } else if (name.startsWith("java.util.List") || name.startsWith("java.util.Set")) {
                final TypeInfoGenericInfoRaw typeInfoGenericInfoRaw = Cc.first(collect).get();
                cir.setJavaType(typeInfoGenericInfoRaw.getJavaType());
                cir.setClassName(typeInfoGenericInfoRaw.getClassName());
                cir.setType(SwaggerType.array(typeInfoGenericInfoRaw.getType()));
            } else if (name.startsWith("java.util.Map")) {
                final TypeInfoGenericInfoRaw typeInfoGenericInfoRaw = collect.size() > 1 ? collect.get(1) : collect.get(0);
                cir.setJavaType(type);
                cir.setClassName(name);
                cir.setType(SwaggerType.map(typeInfoGenericInfoRaw.getType()));
            }
        } else if (type == String.class) {
            cir.setType(SwaggerType.simple("string"));
        } else if (type == boolean.class) {
            cir.setType(SwaggerType.simple("boolean"));
        } else if (type == int.class) {
            cir.setType(SwaggerType.simpleWithFormat("integer", "int32"));
        } else if (type == float.class) {
            cir.setType(SwaggerType.simpleWithFormat("number", "float"));
        } else if (type == double.class) {
            cir.setType(SwaggerType.simpleWithFormat("number", "double"));
        } else if (type == long.class) {
            cir.setType(SwaggerType.simpleWithFormat("integer", "int64"));
        } else if (type == Boolean.class) {
            cir.setType(SwaggerType.simple("boolean"));
        } else if (type == Integer.class) {
            cir.setType(SwaggerType.simpleWithFormat("integer", "int32"));
        } else if (type == Float.class) {
            cir.setType(SwaggerType.simpleWithFormat("number", "float"));
        } else if (type == Double.class) {
            cir.setType(SwaggerType.simpleWithFormat("number", "double"));
        } else if (type == Long.class) {
            cir.setType(SwaggerType.simpleWithFormat("integer", "int64"));
        } else if (type == UUID.class) {
            cir.setType(SwaggerType.simpleWithFormat("string", "uuid"));
        } else if (type instanceof Class && ((Class<?>) type).isEnum()) {
            cir.setType(SwaggerType.enumType((Class) type));
        } else if (type == byte[].class) {
            cir.setType(SwaggerType.binary());
        } else if (type == ZonedDateTime.class || type == LocalDateTime.class ||
                type == LocalDate.class || type == LocalTime.class) {
            cir.setType(SwaggerType.simple("string"));
        } else {
            cir.setType(SwaggerType.object(type));
        }
        return cir;
    }

    private static List<Class<?>> getClassesForSchema(List<TypeInfoGenericInfoRaw> types) {
        return types.stream()
                .filter($ -> $.getType().getItemsRefOrType().isPresent())
                .map($ -> $.getType().getClassForSchema())
                .distinct()
                .filter(O::isPresent)
                .map(O::get)
                .sorted(Comparator.comparing($ -> $.getName()))
                .collect(Collectors.toList());
    }

    public abstract static class ParameterMixin {
        @JsonIgnore
        abstract Parameter.StyleEnum getStyle();

        @JsonIgnore
        abstract Boolean getExplode();
    }

    @Data
    @AllArgsConstructor
    private static class ClassInfoGenericInfoRaw {
        String className;
        OneOf<List<FieldInfoGenericInfoRaw>, List<String>> fieldsOrEnumConstants;

        public X2<String, Schema> toSchema(
                Map<Class<?>, ClassInfoGenericInfoRaw> classesForComponents) {
            final Schema<?> schema = fieldsOrEnumConstants.collect(_object -> {
                ObjectSchema s = new ObjectSchema();
                final List<String> required = _object.stream()
                        .filter($ -> !$.getType().getType().isOptional())
                        .map($ -> $.getFieldName()).collect(Cc.toL());

                final Map<String, Schema> schemas = _object.stream()
                        .map($ -> X.x($.getFieldName(), (Schema) convertToSchema($.getType(), classesForComponents)))
                        .collect(Cc.toMX2());
                s.setProperties(schemas);
                s.required(required);
                return s;
            }, _enum -> {
                StringSchema ss = new StringSchema();
                ss.setEnum(_enum);
                return ss;
            });

            return X.x(className, schema);
        }
    }

    @Data
    @AllArgsConstructor
    private static class FieldInfoGenericInfoRaw {
        String fieldName;
        TypeInfoGenericInfoRaw type;
    }

    @Data
    @NoArgsConstructor
    private static class TypeInfoGenericInfoRaw {
        Type javaType;
        String className;
        SwaggerType type;
    }

    @Data
    @RequiredArgsConstructor
    private static class SwaggerType {
        final String type;
        final O<String> format;
        final O<OneOf<String, SwaggerType>> itemsRefOrType;
        final boolean isEnum;
        boolean isMap;
        boolean optional = false;

        public static SwaggerType simple(String type) {
            return new SwaggerType(type, O.empty(), O.empty(), false);
        }

        public static SwaggerType simpleWithFormat(String type, String format) {
            return new SwaggerType(type, O.of(format), O.empty(), false);
        }

        public static <T extends Enum<T>> SwaggerType enumType(Class<T> enumType) {
            return new SwaggerType("string", O.empty(), O.of(OneOf.left(enumType.getName())), true);
        }

        public static <T> SwaggerType object(Type objectType) {
            return new SwaggerType("object", O.empty(), O.of(OneOf.left(objectType.getTypeName())), true);
        }

        public static <T> SwaggerType array(SwaggerType type) {
            return new SwaggerType("array", O.empty(), O.of(OneOf.right(type)), false);
        }

        public static <T> SwaggerType map(SwaggerType valueType) {
            final SwaggerType object = new SwaggerType("object", O.empty(), O.of(OneOf.right(valueType)), false);
            object.setMap(true);
            return object;
        }

        public static SwaggerType binary() {
            return new SwaggerType("string", O.of("binary"), O.empty(), false);
        }

        public O<Class<?>> getClassForSchema() {
            return itemsRefOrType.flatMap($ -> $.oLeft().map($$ -> Ex.toRuntime(() -> Class.forName($$))));
        }

        public O<OneOrBoth<Class<?>, SwaggerType>> getArrayClassForSchema() {
            if ("array".equals(type)) {
                Class<?> classO = itemsRefOrType.flatMap($ -> $.oRight().flatMap($$ -> $$.getClassForSchema())).orElse(null);
                return of(OneOrBoth.maybeBoth(classO, itemsRefOrType.flatMap($ -> $.oRight()).orElse(null)));
            } else {
                return empty();
            }
        }

        public O<OneOrBoth<Class<?>, SwaggerType>> getMapClassForSchema() {
            if (isMap) {
                Class<?> classO = itemsRefOrType.flatMap($ -> $.oRight().flatMap($$ -> $$.getClassForSchema())).orElse(null);
                return of(OneOrBoth.maybeBoth(classO, itemsRefOrType.flatMap($ -> $.oRight()).orElse(null)));
            } else {
                return empty();
            }
        }
    }
}
