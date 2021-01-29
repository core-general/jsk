package sk.aws.s3;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import sk.utils.ifaces.Identifiable;

import java.util.Comparator;

import static java.util.Comparator.comparing;

@Data
@AllArgsConstructor
public class S3ItemMeta implements Comparable<S3ItemMeta>, Identifiable<String> {
    private static final Comparator<S3ItemMeta> COMPARING = comparing($ -> $.getHash() + $.getSize());

    String path;
    long size;
    String hash;
    boolean failed;

    @Override
    public int compareTo(@NotNull S3ItemMeta o) {
        return COMPARING.compare(this, o);
    }

    @Override
    public String getId() {
        return path;
    }
}
