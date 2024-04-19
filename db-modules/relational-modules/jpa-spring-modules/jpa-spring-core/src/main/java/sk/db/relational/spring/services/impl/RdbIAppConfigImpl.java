package sk.db.relational.spring.services.impl;

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


import jakarta.inject.Inject;
import sk.db.relational.model.JpaAppProperties;
import sk.db.relational.spring.services.dao.repo.JpaAppPropertiesRepo;
import sk.services.appconf.ConfigCoreImpl;
import sk.services.appconf.model.ConfigUnit;
import sk.services.appconf.model.ConfigUnitKey;
import sk.services.time.ITime;
import sk.utils.functional.O;

@SuppressWarnings("unused")
public class RdbIAppConfigImpl extends ConfigCoreImpl {
    @Inject JpaAppPropertiesRepo db;
    @Inject ITime times;

    @Override
    public long getTimeSec() {
        return 0;
    }

    @Override
    public O<String> getValue(ConfigUnitKey key) {
        try {
            return O.of(db.findById(new JpaAppProperties.Id(key.getCat(), key.getId()))
                    .map(JpaAppProperties::getValue));
        } catch (Exception ignored) {

        }
        return O.empty();
    }

    @Override
    public String saveValue(ConfigUnit config) {
        try {
            String defaultValue = String.valueOf(config.getDefaultValue());
            JpaAppProperties dp = JpaAppProperties.builder()
                    .id(new JpaAppProperties.Id(config.getKey().getCat(), config.getKey().getId()))
                    .value(defaultValue)
                    .propertyDate(times.nowZ())
                    .description(config.getDescription())
                    .build();
            db.save(dp);
            return defaultValue;
        } catch (Exception ignored) {

        }
        return null;
    }

    @Override
    public void setStringCacheOk(ConfigUnit key, String value) {
        final JpaAppProperties val =
                O.of(db.findById(new JpaAppProperties.Id(key.getKey().getCat(), key.getKey().getId()))).or(() -> {
                    saveValue(key);
                    return db.findById(new JpaAppProperties.Id(key.getKey().getCat(), key.getKey().getId()));
                }).get();

        val.setValue(value);
        db.save(val);
    }

    @Override
    public void removeAll() {
        db.deleteAll();
        super.removeAll();
    }
}
