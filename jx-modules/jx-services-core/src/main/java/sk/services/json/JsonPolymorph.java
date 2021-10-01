package sk.services.json;

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

public abstract class JsonPolymorph implements IJsonPolymorph {
    public static final String fieldName = "_jcl_";
    public String _jcl_;

    @Override
    public void clearMyType() {
        _jcl_ = null;
    }

    @Override
    public void setMyType() {
        _jcl_ = getClass().getName();
    }

    @Override
    public boolean equals(Object o) {
        throw new RuntimeException("PLEASE OVERRIDE EQUALS!");
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("PLEASE OVERRIDE HASHCODE!");
    }
}
