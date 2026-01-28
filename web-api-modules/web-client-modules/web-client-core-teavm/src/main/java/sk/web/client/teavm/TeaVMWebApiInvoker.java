package sk.web.client.teavm;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import sk.web.client.WebApiInvoker;
import sk.web.client.WebMethodInvokeHandler;
import sk.web.infogatherer.WebClassInfo;

/**
 * TeaVM implementation of WebApiInvoker.
 * <p>
 * TeaVM cannot use java.lang.reflect.Proxy, so this implementation requires
 * compile-time generated client classes. The actual implementation would use
 * TeaVM Metaprogramming or annotation processors to generate concrete client
 * implementations at compile time.
 * <p>
 * To use this in a TeaVM project:
 * 1. Implement a TeaVM Metaprogramming plugin or annotation processor
 * 2. For each API interface, generate a concrete class that implements it
 * 3. The generated class should delegate to WebMethodInvokeHandler for each method
 * 4. Register generated classes in a factory/registry
 * <p>
 * Example generated code pattern:
 * <pre>
 * public class MyApiClient implements MyApi {
 *     private final WebMethodInvokeHandler handler;
 *
 *     public String myMethod(String param) {
 *         return (String) handler.invoke("myMethod", new Object[]{param});
 *     }
 * }
 * </pre>
 */
public class TeaVMWebApiInvoker implements WebApiInvoker {

    @Override
    public <API> API createClient(Class<API> apiCls, WebClassInfo classInfo, WebMethodInvokeHandler methodInvoker) {
        // TeaVM requires compile-time generation - this is a placeholder
        // Actual implementation requires:
        // 1. Compile-time annotation processor or TeaVM plugin
        // 2. Generated concrete classes for each API interface
        // 3. A registry to look up generated implementations
        throw new UnsupportedOperationException(
                "TeaVM requires compile-time generated clients. " +
                "Use @GenerateClient annotation or TeaVM Metaprogramming to generate " +
                "client implementations for: " + apiCls.getName());
    }
}
