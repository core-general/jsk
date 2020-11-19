package sk.utils.semver;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ma;
import sk.utils.statics.St;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "WeakerAccess"})
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MultiSemver implements Comparable<MultiSemver> {
    private List<Integer> parts;

    public static O<MultiSemver> parse(String semverString) {
        try {
            if (St.isNullOrEmpty(semverString)) {
                return O.empty();
            }
            return O.of(new MultiSemver(semverString));
        } catch (Exception e) {
            return O.empty();
        }
    }

    private MultiSemver(String version) {
        parts = Cc.stream(version.split("\\.")).map(Ma::pi).collect(Collectors.toList());
    }

    public int size() {
        return parts.size();
    }

    public int difInIndex(MultiSemver other) {
        for (int i = 0; i < Math.max(parts.size(), other.parts.size()); i++) {
            int v1 = Cc.getOrDefault(parts, i, 0);
            int v2 = Cc.getOrDefault(other.parts, i, 0);
            if (v1 != v2) {
                return i;
            }
        }
        return -1;
    }

    public String getStringValue() {
        return Cc.join(".", parts);
    }

    public Boolean isGreaterThan(MultiSemver other) {
        return compareOrEqual(other).orElse(false);
    }

    public Boolean isGreaterOrEqualThan(MultiSemver other) {
        return compareOrEqual(other).orElse(true);
    }

    @Override
    public String toString() {
        return getStringValue();
    }

    @Override
    public int compareTo(@NotNull MultiSemver o) {
        Boolean thisGOE = this.isGreaterOrEqualThan(o);
        Boolean otherGOE = o.isGreaterOrEqualThan(this);

        if (thisGOE && !otherGOE) {
            return 1;
        } else if (otherGOE & !thisGOE) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MultiSemver that = (MultiSemver) o;
        return this.compareTo(that) == 0;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (parts != null) {
            for (Integer part : parts) {
                hash += part;
            }
        }
        return hash;
    }

    private O<Boolean> compareOrEqual(MultiSemver other) {
        for (int i = 0; i < Math.max(parts.size(), other.parts.size()); i++) {
            int v1 = Cc.getOrDefault(parts, i, 0);
            int v2 = Cc.getOrDefault(other.parts, i, 0);
            if (v1 > v2) {
                return O.of(true);
            } else if (v1 < v2) {
                return O.of(false);
            }
        }
        return O.empty();
    }
}
