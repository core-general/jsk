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
import sk.services.bytes.IBytes;
import sk.services.time.ITime;
import sk.utils.functional.O;
import sk.utils.statics.Ti;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.time.temporal.ChronoField.*;

public class GsonDefaultSerDes extends GsonSerDesList {
    public GsonDefaultSerDes(ITime times, IBytes bytes) {
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
    }
}
