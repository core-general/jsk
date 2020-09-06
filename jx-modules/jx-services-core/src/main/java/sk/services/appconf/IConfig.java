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


import sk.services.appconf.model.ConfigUnit;
import sk.utils.javafixes.TypeWrap;

@SuppressWarnings("unused")
public interface IConfig {
    void setString(ConfigUnit key, String value);

    void setBool(ConfigUnit key, boolean value);

    void setLong(ConfigUnit key, long value);

    void setInt(ConfigUnit key, int value);

    void setDouble(ConfigUnit key, double value);

    void setFloat(ConfigUnit key, float value);

    <T> void setObject(ConfigUnit key, T object);

    <T> T getAsObject(ConfigUnit item, Class<T> cls);

    <T> T getAsObject(ConfigUnit item, TypeWrap<T> cls);

    String getAsString(ConfigUnit item);

    boolean getAsBool(ConfigUnit item);

    long getAsLong(ConfigUnit item) throws NumberFormatException;

    int getAsInt(ConfigUnit item) throws NumberFormatException;

    double getAsDouble(ConfigUnit item) throws NumberFormatException;

    float getAsFloat(ConfigUnit item) throws NumberFormatException;

    void removeAll();
}
