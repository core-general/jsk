package sk.services.json.marcono1234.gson.recordadapter;

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

// See https://github.com/google/gson/issues/1794#issuecomment-919890214

import com.google.gson.annotations.JsonAdapter;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Optional;
/*
https://github.com/Marcono1234/gson-record-type-adapter-factory
MIT License

Copyright (c) 2021 Marcono1234

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/**
 * Creator for type adapter instances of classes referenced by {@link JsonAdapter @JsonAdapter}.
 *
 * <p>Creator implementations must be thread-safe.
 *
 * @see marcono1234.gson.recordadapter.RecordTypeAdapterFactory.Builder#registerJsonAdapterCreator(JsonAdapterCreator)
 */
// Deprecate once Gson has API for using its InstanceCreators, see https://github.com/google/gson/pull/1968
public interface JsonAdapterCreator {
    /**
     * Exception thrown when creation of a type adapter fails.
     *
     * @see JsonAdapterCreator#create(Class)
     */
    class AdapterCreationException extends Exception {
        @Serial
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unused")
        public AdapterCreationException(String message) {
            super(message);
        }

        public AdapterCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * A creator which invokes the public no-args constructor of a given class.
     *
     * <p>If a class does not have a public no-args constructor or if it is
     * abstract or non-static no instance will be created and an empty {@code Optional}
     * is returned.
     */
    JsonAdapterCreator DEFAULT_CONSTRUCTOR_INVOKER = new JsonAdapterCreator() {
        @Override
        public Optional<Object> create(Class<?> c) throws AdapterCreationException {
            int modifiers = c.getModifiers();
            // Cannot create instances of abstract and inner classes
            if (Modifier.isAbstract(modifiers) || !Modifier.isStatic(modifiers)) {
                return Optional.empty();
            }

            Constructor<?> constructor;
            try {
                constructor = c.getConstructor();
            } catch (NoSuchMethodException e) {
                return Optional.empty();
            }
            try {
                constructor.setAccessible(true);
                return Optional.of(constructor.newInstance());
            } catch (InaccessibleObjectException | IllegalAccessException e) {
                throw new AdapterCreationException("Default constructor of " + c +
                        " is not accessible; open it to this library or register a custom JsonAdapterCreator", e);
            } catch (InstantiationException e) {
                throw new AdapterCreationException("Failed invoking default constructor for " + c, e);
            } catch (InvocationTargetException e) {
                throw new AdapterCreationException("Failed invoking default constructor for " + c, e.getCause());
            }
        }

        @Override
        public String toString() {
            return "DEFAULT_CONSTRUCTOR_INVOKER";
        }
    };

    /**
     * Creates an instance for the given class specified by a {@link JsonAdapter @JsonAdapter} annotation.
     * The instance has to be a subtype of one of the following types:
     * <ol>
     *     <li>{@link com.google.gson.TypeAdapter}</li>
     *     <li>{@link com.google.gson.TypeAdapterFactory}</li>
     *     <li>{@link com.google.gson.JsonSerializer} or {@link com.google.gson.JsonDeserializer} (or both)</li>
     * </ol>
     *
     * <p>The returned instance does not have to be a new instance; it may also be a singleton, but it must
     * be thread-safe.
     *
     * <p>If this creator does not support the given class, it can return {@code Optional.empty()} to
     * let the next creator (if any) try to create an instance. If this creator supports the class but
     * creation fails an {@link AdapterCreationException} should be thrown.
     *
     * <p>A creator is allowed to return an instance whose type is unrelated to the given class, for example
     * if the given class has a constant field representing the type adapter to use.
     *
     * @param c class to create an instance for
     * @return {@code Optional} containing the created instance; or empty {@code Optional} if instance creation
     * for the given class is not supported
     * @throws AdapterCreationException If instance creation is supported for the given class, but fails
     */
    Optional<Object> create(Class<?> c) throws AdapterCreationException;
}
