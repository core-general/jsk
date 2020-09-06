package sk.utils.files;

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

import lombok.*;
import sk.utils.functional.O;
import sk.utils.statics.St;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static sk.utils.statics.St.*;

@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class PathWithBase {
    @Getter String base;
    O<String> path;

    public PathWithBase(String base) {
        this(base, O.empty());
    }

    public String getPathWithSlash() {
        return getInnerPathWithSlash().orElse("");
    }

    public String getPathNoSlash() {
        return getInnerPathNoSlash().orElse("");
    }

    @SneakyThrows
    public String getEncodedUrl() {
        return URLEncoder.encode(St.endWith(base, "/") + path.orElse(""), StandardCharsets.UTF_8.toString());
    }

    public PathWithBase addToPath(String pathSuffix) {
        return new PathWithBase(
                base,
                getInnerPathNoSlash().map(currentPath -> currentPath + startWith(pathSuffix, "/"))
                        .or(() -> O.of(notStartWith(pathSuffix, "/")))
        );
    }

    public PathWithBase replacePath(String pathSuffix) {
        return new PathWithBase(
                base,
                O.of(pathSuffix)
        );
    }

    private O<String> getInnerPathNoSlash() {
        return path.map($ -> notEndWith($, "/"));
    }

    private O<String> getInnerPathWithSlash() {
        return path.map($ -> endWith($, "/"));
    }
}
