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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import sk.utils.functional.O;
import sk.utils.functional.OneBothOrNone;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.St;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static sk.utils.statics.St.*;

@EqualsAndHashCode
public class PathWithBase {
    @Getter String base;
    O<String> path;

    public static PathWithBase pwb(String base) {
        return new PathWithBase(base);
    }

    public static PathWithBase pwb(String base, String... path) {
        return new PathWithBase(base, path);
    }

    public PathWithBase(String base) {
        this(base, O.empty());
    }

    public PathWithBase(String base, String... path) {
        this(base, path.length == 0 ? O.empty() : O.of(Cc.join("/", Arrays.asList(path))));
    }

    private PathWithBase(String base, O<String> path) {
        this.base = St.subRF(base, "/");
        O<String> pathPartOfBase = base.contains("/") ? O.of(St.subLF(base, "/")) : O.empty();
        OneBothOrNone<String, String> leftAndPath = OneBothOrNone.any(pathPartOfBase, path);
        this.path = O.ofNull(leftAndPath.collect(
                one -> one.collect($ -> $, $ -> $),
                both -> both.left() + "/" + both.right(),
                () -> null));
    }

    public String getPathWithSlash() {
        return getInnerPathWithSlash().orElse("");
    }

    public String getPathNoSlash() {
        return getInnerPathNoSlash().orElse("");
    }

    public String getEncodedUrl() {
        try {
            return URLEncoder.encode(St.endWith(base, "/") + path.orElse(""), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return Ex.thRow(e);
        }
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

    public String toString() {return this.base + O.ofNull(this.path).flatMap($ -> $).map($ -> St.startWith($, "/")).orElse("");}
}
