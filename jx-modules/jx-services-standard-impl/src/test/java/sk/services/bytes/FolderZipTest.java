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
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class FolderZipTest {
    @TempDir Path temporary;
    private final BytesImpl bytes = new BytesImpl();

    @Test void archivesContentsAndReplacesThePreviousArchive() throws Exception {
        Path work = Files.createDirectory(temporary.resolve("work"));
        Path file = Files.writeString(work.resolve("artifact.txt"), "first");
        Path target = temporary.resolve("output.zip");
        bytes.zipFolderContentsTo(work.toFile(), target.toFile());
        Files.delete(file);
        Files.writeString(work.resolve("replacement.txt"), "second");
        bytes.zipFolderContentsTo(work.toFile(), target.toFile());
        try (ZipFile zip = new ZipFile(target.toFile())) {
            assertNull(zip.getEntry("artifact.txt"));
            assertNotNull(zip.getEntry("replacement.txt"));
            assertEquals("second", new String(zip.getInputStream(zip.getEntry("replacement.txt")).readAllBytes()));
        }
    }

    @Test void supportsEmptyDirectoriesAndRejectsEscapingLinks() throws Exception {
        Path work = Files.createDirectory(temporary.resolve("work"));
        Path target = temporary.resolve("output.zip");
        bytes.zipFolderContentsTo(work.toFile(), target.toFile());
        try (ZipFile zip = new ZipFile(target.toFile())) { assertFalse(zip.entries().hasMoreElements()); }
        Path outside = Files.writeString(temporary.resolve("outside.txt"), "private");
        Files.createSymbolicLink(work.resolve("escape"), outside);
        assertThrows(IllegalArgumentException.class, () -> bytes.zipFolderContentsTo(work.toFile(), target.toFile()));
    }
}
