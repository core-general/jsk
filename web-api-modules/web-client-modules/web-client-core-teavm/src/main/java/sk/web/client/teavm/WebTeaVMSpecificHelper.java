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

import sk.web.client.WebMethodInvokeHandler;
import sk.web.client.WebPlatformSpecificHelper;
import sk.web.infogatherer.WebClassInfo;

/**
 * TeaVM implementation of WebPlatformSpecificHelper.
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
public class WebTeaVMSpecificHelper implements WebPlatformSpecificHelper {

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

    @Override
    public String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        return encodeURIComponent(value);
    }

    /**
     * Pure Java implementation of URL encoding compatible with TeaVM.
     * Encodes a string as per RFC 3986 (similar to JavaScript's encodeURIComponent).
     */
    private String encodeURIComponent(String s) {
        StringBuilder result = new StringBuilder();
        byte[] bytes;
        try {
            bytes = s.getBytes("UTF-8");
        } catch (Exception e) {
            // Fallback for environments without UTF-8 (shouldn't happen in TeaVM)
            bytes = s.getBytes();
        }

        for (byte b : bytes) {
            int i = b & 0xFF;
            if (isUnreserved(i)) {
                result.append((char) i);
            } else {
                result.append('%');
                result.append(toHex(i >> 4));
                result.append(toHex(i & 0xF));
            }
        }
        return result.toString();
    }

    /**
     * Checks if a character is unreserved as per RFC 3986.
     * Unreserved characters: A-Z a-z 0-9 - _ . ~
     */
    private boolean isUnreserved(int c) {
        return (c >= 'A' && c <= 'Z') ||
               (c >= 'a' && c <= 'z') ||
               (c >= '0' && c <= '9') ||
               c == '-' || c == '_' || c == '.' || c == '~';
    }

    private char toHex(int digit) {
        if (digit < 10) {
            return (char) ('0' + digit);
        }
        return (char) ('A' + digit - 10);
    }
}
