package sk.services.bytes;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BCryptRevisionsTest {
    private static final String PYTHON_2B_ASCII = "$2b$04$xEZGiD29XpXA8Sz7oaL97u30qloscTugy90vqSwaNQwHNmhEUQ8Fq";
    private static final String PYTHON_2B_UNICODE = "$2b$05$B31XNIuv162jkN13dH9dZ.su27t5fhiP8fcICLRkcQFoMPS59n/Je";

    @Test
    public void hashesOfOtherRevisionsVerify() {
        assertTrue(BCrypt.checkpw("DevAdmin1!", PYTHON_2B_ASCII));
        assertFalse(BCrypt.checkpw("DevAdmin1?", PYTHON_2B_ASCII));
        assertTrue(BCrypt.checkpw("Пароль-ünï1A", PYTHON_2B_UNICODE));
        assertTrue(BCrypt.checkpw("DevAdmin1!", "$2y" + PYTHON_2B_ASCII.substring(3)));
    }

    @Test
    public void explicitCostIsWrittenIntoTheHash() {
        final String hash = BCrypt.hashpw("secret", BCrypt.gensalt(5));
        assertTrue(hash.startsWith("$2a$05$"));
        assertTrue(BCrypt.checkpw("secret", hash));
    }
}
