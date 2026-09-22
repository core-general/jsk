package sk.services.json;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
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

import org.junit.jupiter.api.Test;
import sk.services.CoreServicesRaw;
import sk.utils.javafixes.TypeWrap;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JGsonNumberTest {

    private final IJson json = CoreServicesRaw.services().json();

    @Test
    void existingReadMethodsPreserveUntypedAndNumberValues() {
        for (String value : List.of("9007199254740993", "1.0000000000000001", "0.123456789012345678901", "1e400")) {
            BigDecimal expected = new BigDecimal(value);
            assertEquals(expected, json.from(value, Object.class));
            assertEquals(expected, json.fromWithNulls(value, Object.class));
            assertEquals(expected, json.from(value, Number.class));
            assertEquals(expected, json.fromWithNulls(value, Number.class));
            assertEquals(expected, json.from(value, TypeWrap.simple(Object.class)));
            assertEquals(expected, json.fromWithNulls(value, TypeWrap.simple(Object.class)));
            assertEquals(expected, json.from(stream(value), Object.class));
            assertEquals(expected, json.from(stream(value), TypeWrap.simple(Object.class)));
            assertEquals(expected, json.from(json.to(expected), Object.class));
            assertEquals(expected, json.from(json.toPretty(expected), Object.class));
            assertEquals(expected, json.fromWithNulls(json.to(expected, true, true), Object.class));
        }
    }

    @Test
    void preservesPrecisionInsideMapsListsAndDtoFields() {
        String document = """
                {"integer":9007199254740993,"decimal":1.0000000000000001,"exponent":1e400,
                 "nil":null,"items":[true,"café",null,0.123456789012345678901]}
                """;
        Map<String, Object> data = json.fromWithNulls(document, TypeWrap.getMap(String.class, Object.class));
        assertEquals(new BigDecimal("9007199254740993"), data.get("integer"));
        assertEquals(new BigDecimal("1.0000000000000001"), data.get("decimal"));
        assertEquals(new BigDecimal("1e400"), data.get("exponent"));
        assertTrue(data.containsKey("nil"));
        assertNull(data.get("nil"));
        assertEquals(new BigDecimal("0.123456789012345678901"), ((List<?>) data.get("items")).get(3));
        assertEquals(data, json.fromWithNulls(json.toWithNulls(data), Object.class));

        NumericFields fields = json.fromWithNulls(
                "{\"object\":9007199254740993,\"number\":1.0000000000000001}", NumericFields.class);
        assertEquals(new BigDecimal("9007199254740993"), fields.object);
        assertEquals(new BigDecimal("1.0000000000000001"), fields.number);
    }

    @Test
    void explicitNumericTypesStillUseTheirDeclaredTypes() {
        assertEquals(1.5, json.fromWithNulls("1.5", Double.class));
        assertEquals(9007199254740993L, json.fromWithNulls("9007199254740993", Long.class));
        assertEquals(42, json.fromWithNulls("42", Integer.class));
        assertEquals(new BigDecimal("1.0000000000000001"), json.fromWithNulls("1.0000000000000001", BigDecimal.class));
    }

    @Test
    void jsonPathMappingUsesTheSameExactNumberDefaults() {
        BigDecimal expected = new BigDecimal("9007199254740993");
        assertEquals(expected, json.jsonPath("{\"value\":9007199254740993}", "$.value", TypeWrap.simple(Object.class)).left());
        assertEquals(expected, json.jsonPath("{\"value\":9007199254740993}", context -> context.read("$.value", Number.class)).left());
    }

    private ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static class NumericFields {
        Object object;
        Number number;
    }
}
