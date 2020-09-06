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

import lombok.Getter;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

@Getter
@SuppressWarnings("unused")
public class Semver200 implements Comparable<Semver200> {
    final MultiSemver ms;

    public static O<Semver200> parse(String semverString) {
        return MultiSemver.parse(semverString).map(Semver200::new);
    }

    public static Semver200 create(int major, int minor, int hotfix) {
        return new Semver200(major, minor, hotfix);
    }

    private Semver200(int major, int minor, int hotfix) {
        ms = new MultiSemver(Cc.l(major, minor, hotfix));
    }

    private Semver200(MultiSemver ms) {
        this.ms = ms;
    }

    public Integer getMajor() {
        return ms.getParts().get(0);
    }

    public Integer getMinor() {
        return ms.getParts().get(1);
    }

    public Integer getHotfix() {
        return ms.getParts().get(2);
    }

    @Override
    public int compareTo(Semver200 o) {
        return ms.compareTo(o.getMs());
    }

    public Boolean isGreaterThan(Semver200 other) {return ms.isGreaterThan(other.ms);}

    public Boolean isGreaterOrEqualThan(Semver200 other) {return ms.isGreaterOrEqualThan(other.ms);}

    public String toString() {
        return ms.toString();
    }
}
