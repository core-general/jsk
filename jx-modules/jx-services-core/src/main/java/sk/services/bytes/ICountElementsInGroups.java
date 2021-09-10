package sk.services.bytes;

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

import sk.utils.statics.Ar;

import java.nio.charset.StandardCharsets;

public abstract class ICountElementsInGroups {
    public ICountElementsInGroups() {}

    public ICountElementsInGroups(byte[] serialized) {
        if (serialized != null) { deSerialize(serialized); }
    }

    public abstract void addElement(byte[] element);

    public abstract long getElementCount(byte[] element);

    public abstract byte[] serialize();

    protected abstract void deSerialize(byte[] data);

    public final void addElement(int element) {
        addElement(Ar.intToByteArray(element));
    }

    public final void addElement(long element) {
        addElement(Ar.longToByteArray(element));
    }

    public final <T> void addElement(T element) {
        addElement(element.toString());
    }

    public final void addElement(String element) {
        addElement(element.getBytes(StandardCharsets.UTF_8));
    }

    public final long getElementCount(int element) {
        return getElementCount(Ar.intToByteArray(element));
    }

    public final long getElementCount(long element) {
        return getElementCount(Ar.longToByteArray(element));
    }

    public final <T> long getElementCount(T element) {
        return getElementCount(element.toString());
    }

    public final long getElementCount(String element) {
        return getElementCount(element.getBytes(StandardCharsets.UTF_8));
    }
}
