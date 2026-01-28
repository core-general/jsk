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

import sk.web.client.WebUrlEncoder;

/**
 * TeaVM-compatible URL encoder implementation.
 * <p>
 * TeaVM doesn't support java.net.URLEncoder directly, so this implementation
 * provides a pure Java URL encoding that is compatible with TeaVM compilation.
 * <p>
 * Alternative approaches for TeaVM:
 * 1. Bridge to JavaScript's encodeURIComponent via JSBody annotation
 * 2. Use a pure Java implementation (as done here)
 */
public class TeaVMUrlEncoder implements WebUrlEncoder {

    @Override
    public String encode(String value) {
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
