package sk.services.bytes;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import sk.utils.statics.Ar;

import java.nio.charset.StandardCharsets;

public abstract class ICountSetExistence {
    public ICountSetExistence() {}

    public ICountSetExistence(byte[] serialized) {
        if (serialized != null) {deSerialize(serialized);}
    }

    public abstract boolean addElement(byte[] element);

    public abstract boolean isElementExist(byte[] element);

    public abstract byte[] serialize();

    protected abstract void deSerialize(byte[] data);

    public final boolean addElement(int element) {
        return addElement(Ar.intToByteArray(element));
    }

    public final boolean addElement(long element) {
        return addElement(Ar.longToByteArray(element));
    }

    public final <T> boolean addElement(T element) {
        return addElement(element.toString());
    }

    public final boolean addElement(String element) {
        return addElement(element.getBytes(StandardCharsets.UTF_8));
    }


    public final boolean isElementExist(int element) {
        return isElementExist(Ar.intToByteArray(element));
    }

    public final boolean isElementExist(long element) {
        return isElementExist(Ar.longToByteArray(element));
    }

    public final <T> boolean isElementExist(T element) {
        return isElementExist(element.toString());
    }

    public final boolean isElementExist(String element) {
        return isElementExist(element.getBytes(StandardCharsets.UTF_8));
    }

}
