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
import lombok.SneakyThrows;
import sk.services.bytes.IBytes;
import sk.services.time.ITime;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.semver.Semver200;
import sk.utils.statics.Fu;
import sk.utils.statics.Ma;
import sk.utils.statics.Ti;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.InvalidParameterException;
import java.time.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static java.time.temporal.ChronoField.*;

public class GsonDefaultSerDes extends GsonSerDesList {
    public GsonDefaultSerDes(ITime times, IBytes bytes) {
        add(new GsonSerDes<ObjectAndItsJson>(ObjectAndItsJson.class) {
            @Override
            public ObjectAndItsJson deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                final Object deserialize = context.deserialize(json, ((ParameterizedType) typeOfT).getActualTypeArguments()[0]);
                return new ObjectAndItsJson(deserialize, json.toString());
            }

            @Override
            public JsonElement serialize(ObjectAndItsJson src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(src.getObject());
            }
        });
        add(new GsonSerDes<OneOf>(OneOf.class) {
            @Override
            public OneOf deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                JsonObject o = json.getAsJsonObject();
                JsonElement left = o.get("left");
                JsonElement right = o.get("right");
                if (left != null && !Fu.equal(left, JsonNull.INSTANCE)) {
                    return OneOf.left(context.deserialize(left, ((ParameterizedType) typeOfT).getActualTypeArguments()[0]));
                } else {
                    return OneOf.right(context.deserialize(right, ((ParameterizedType) typeOfT).getActualTypeArguments()[1]));
                }
            }

            @Override
            public JsonElement serialize(OneOf src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject jo = new JsonObject();
                if (src == null) {
                    return JsonNull.INSTANCE;
                }
                if (src.isLeft()) {
                    jo.add("left", context.serialize(src.left()));
                } else if (src.isRight()) {
                    jo.add("right", context.serialize(src.right()));
                }
                return jo;
            }
        });
        add(new GsonSerDes<Optional>(Optional.class) {
            @Override
            public Optional deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                return Optional
                        .ofNullable(context.deserialize(json, ((ParameterizedType) typeOfT).getActualTypeArguments()[0]));
            }

            @Override
            public JsonElement serialize(Optional src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(src.orElse(null));
            }
        });
        add(new GsonSerDes<O>(O.class) {
            @Override
            public O deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                return O.ofNullable(context.deserialize(json, ((ParameterizedType) typeOfT).getActualTypeArguments()[0]));
            }

            @Override
            public JsonElement serialize(O src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(src.orElse(null));
            }
        });
        add(new GsonSerDes<UUID>(UUID.class) {
            @Override
            public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                return UUID.fromString(json.getAsString());
            }

            @Override
            public JsonElement serialize(UUID src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(src.toString());
            }
        });
        add(new GsonSerDes<ZonedDateTime>(ZonedDateTime.class) {
            @Override
            public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                String val = json.getAsString();
                if (val.contains("-")) {
                    return Ti.yyyyMMddHHmmssSSS.parse(val, t -> ZonedDateTime.of(
                            t.get(YEAR), t.get(MONTH_OF_YEAR), t.get(DAY_OF_MONTH),
                            t.get(HOUR_OF_DAY), t.get(MINUTE_OF_HOUR), t.get(SECOND_OF_MINUTE),
                            t.get(MILLI_OF_SECOND) * 1_000_000, times.getZone()
                    ));
                } else {
                    return times.toZDT(Long.parseLong(val));
                }
            }

            @Override
            public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(Ti.yyyyMMddHHmmssSSS.format(src));
            }
        });
        add(new GsonSerDes<LocalDate>(LocalDate.class) {
            @Override
            public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                String val = json.getAsString();
                return LocalDate.parse(val, Ti.yyyyMMdd);
            }

            @Override
            public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(Ti.yyyyMMdd.format(src));
            }
        });
        add(new GsonSerDes<Instant>(Instant.class) {
            @Override
            public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                long val = json.getAsLong();
                return Instant.ofEpochMilli(val);
            }

            @Override
            public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(src.toEpochMilli());
            }
        });
        add(new GsonSerDes<LocalDateTime>(LocalDateTime.class) {
            @Override
            public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                String val = json.getAsString();
                return LocalDateTime.parse(val, Ti.yyyyMMddHHmmssSSS);
            }

            @Override
            public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(Ti.yyyyMMddHHmmssSSS.format(src));
            }
        });
        add(new GsonSerDes<Duration>(Duration.class) {
            @Override
            public Duration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                long val = json.getAsLong();
                return Duration.ofMillis(val);
            }

            @Override
            public JsonElement serialize(Duration src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(src.toMillis());
            }
        });
        add(new GsonSerDes<byte[]>(byte[].class) {
            @Override
            public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                if (json.isJsonArray()) {
                    final JsonArray arr = json.getAsJsonArray();
                    byte[] toRet = new byte[arr.size()];
                    for (int i = 0; i < arr.size(); i++) {
                        toRet[i] = arr.get(i).getAsByte();
                    }
                    return toRet;
                } else {
                    final String asString = json.getAsString();
                    return bytes.dec64(asString);
                }
            }

            @Override
            public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(bytes.enc64(src));
            }
        });
        add(new GsonSerDes<URL>(URL.class) {
            @Override
            @SneakyThrows
            public URL deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                return new URL(json.getAsString());
            }

            @Override
            public JsonElement serialize(URL src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(src.toString());
            }
        });
        add(new GsonSerDes<Semver200>(Semver200.class) {
            @Override
            @SneakyThrows
            public Semver200 deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                String asString = json.getAsString();
                int[] split = Arrays.stream(asString.split("\\.")).mapToInt(value -> Ma.pi(value)).toArray();
                if (split.length < 3) {
                    throw new InvalidParameterException(
                            "Semver200 should have strictly 3 numbers delimited by two '.':" + asString);
                }
                return Semver200.create(split[0], split[1], split[2]);
            }

            @Override
            public JsonElement serialize(Semver200 src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(src.toString());
            }
        });
    }
}
