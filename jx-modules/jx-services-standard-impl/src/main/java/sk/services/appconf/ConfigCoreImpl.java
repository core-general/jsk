package sk.services.appconf;

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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import sk.services.appconf.model.ConfigUnit;
import sk.services.appconf.model.ConfigUnitKey;
import sk.services.appconf.model.NeedsConfig;
import sk.services.json.IJson;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings({"unused"})
@Log4j2
public abstract class ConfigCoreImpl implements IConfig {
    @Inject IJson json;
    @Inject Optional<List<NeedsConfig>> configurables;

    private ConcurrentMap<ConfigUnitKey, ConfigUnit> d;
    private LoadingCache<ConfigUnitKey, Object> val;

    @PostConstruct
    void initDynamoAppConfigServiceImpl() {
        d = O.of(configurables).stream()
                .flatMap(Collection::stream)
                .flatMap(dc -> dc.getConfigUnits().stream())
                .collect(Collectors.toConcurrentMap(ConfigUnit::getKey, a -> a));

        val = Caffeine.newBuilder()
                .expireAfterWrite(getTimeSec() >= 0
                                ? getTimeSec()
                                : Long.MAX_VALUE,
                        TimeUnit.SECONDS)
                .build(key -> getValue(key).orElseGet(() -> {
                    ConfigUnit config = d.get(key);
                    String s = String.valueOf(config.getDefaultValue());
                    try {
                        s = saveValue(config);
                    } catch (Exception e) {
                        log.error("", e);
                    }
                    return s;
                }));

        StringBuilder sb = new StringBuilder("\n\nCONFIG\n");
        d.forEach((k, v) -> sb.append(String.format("     %s : %s\n", k.toString(), getAsString(v))));
        log.info(sb.toString());
    }

    public abstract long getTimeSec();

    public abstract O<String> getValue(ConfigUnitKey key);

    public abstract String saveValue(ConfigUnit item);

    @Override
    public <T> T getAsObject(ConfigUnit item, Class<T> cls) {
        return json.from(getAsString(item), cls);
    }

    @Override
    public <T> T getAsObject(ConfigUnit item, TypeWrap<T> cls) {
        return json.from(getAsString(item), cls);
    }


    @Override
    @SneakyThrows
    public String getAsString(ConfigUnit item) {
        try {
            return Objects.requireNonNull(val.get(item.getKey())).toString();
        } catch (Throwable ignored) {

        }
        return null;
    }

    @Override
    @SneakyThrows
    public long getAsLong(ConfigUnit item) throws NumberFormatException {
        return Long.parseLong(getAsString(item));
    }

    @Override
    @SneakyThrows
    public boolean getAsBool(ConfigUnit item) {
        return Boolean.parseBoolean(getAsString(item));
    }

    @Override
    @SneakyThrows
    public int getAsInt(ConfigUnit item) throws NumberFormatException {
        return Integer.parseInt(getAsString(item));
    }

    @Override
    public double getAsDouble(ConfigUnit item) throws NumberFormatException {
        return Double.parseDouble(getAsString(item));
    }

    @Override
    public float getAsFloat(ConfigUnit item) throws NumberFormatException {
        return Float.parseFloat(getAsString(item));
    }

    @Override
    public void setBool(ConfigUnit key, boolean value) {
        setString(key, String.valueOf(value));
    }


    @Override
    public final void setString(ConfigUnit key, String value) {
        setStringCacheOk(key, value);
        val.invalidateAll();
    }

    public abstract void setStringCacheOk(ConfigUnit key, String value);


    @Override
    public void setLong(ConfigUnit key, long value) {
        setString(key, String.valueOf(value));
    }

    @Override
    public void setInt(ConfigUnit key, int value) {
        setString(key, String.valueOf(value));
    }

    @Override
    public void setDouble(ConfigUnit key, double value) {
        setString(key, String.valueOf(value));
    }

    @Override
    public void setFloat(ConfigUnit key, float value) {
        setString(key, String.valueOf(value));
    }

    @Override
    public <T> void setObject(ConfigUnit key, T object) {
        setString(key, json.to(object));
    }

    @Override
    public void removeAll() {
        val.invalidateAll();
        d.forEach((key, value) -> saveValue(d.get(key)));
    }
}
