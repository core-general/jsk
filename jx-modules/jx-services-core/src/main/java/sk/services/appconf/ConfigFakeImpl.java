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

import lombok.extern.log4j.Log4j2;
import sk.exceptions.NotImplementedException;
import sk.services.appconf.model.ConfigUnit;
import sk.utils.javafixes.TypeWrap;

@SuppressWarnings({"unused", "unchecked"})
@Log4j2
public abstract class ConfigFakeImpl implements IConfig {
    @Override
    public <T> T getAsObject(ConfigUnit item, Class<T> cls) {
        return (T) item.getDefaultValue();
    }

    @Override
    public <T> T getAsObject(ConfigUnit item, TypeWrap<T> cls) {
        return (T) item.getDefaultValue();
    }

    @Override
    public String getAsString(ConfigUnit item) {
        return (String) item.getDefaultValue();
    }

    @Override
    public boolean getAsBool(ConfigUnit item) {
        return (Boolean) item.getDefaultValue();
    }

    @Override
    public long getAsLong(ConfigUnit item) throws NumberFormatException {
        return (Long) item.getDefaultValue();
    }

    @Override
    public int getAsInt(ConfigUnit item) throws NumberFormatException {
        return (Integer) item.getDefaultValue();
    }

    @Override
    public double getAsDouble(ConfigUnit item) throws NumberFormatException {
        return (Double) item.getDefaultValue();
    }

    @Override
    public float getAsFloat(ConfigUnit item) throws NumberFormatException {
        return (Float) item.getDefaultValue();
    }

    @Override
    public void setString(ConfigUnit key, String value) {
        throw new NotImplementedException();
    }

    @Override
    public void setBool(ConfigUnit key, boolean value) {
        throw new NotImplementedException();
    }

    @Override
    public void setLong(ConfigUnit key, long value) {
        throw new NotImplementedException();
    }

    @Override
    public void setInt(ConfigUnit key, int value) {
        throw new NotImplementedException();
    }

    @Override
    public void setDouble(ConfigUnit key, double value) {
        throw new NotImplementedException();
    }

    @Override
    public void setFloat(ConfigUnit key, float value) {
        throw new NotImplementedException();
    }

    @Override
    public <T> void setObject(ConfigUnit key, T object) {
        throw new NotImplementedException();
    }

    @Override
    public void removeAll() {

    }
}
