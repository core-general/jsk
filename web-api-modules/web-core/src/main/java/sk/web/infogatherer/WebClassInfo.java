package sk.web.infogatherer;

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

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.mvn.model.ApiDtoClassModel;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


@AllArgsConstructor
public class WebClassInfo {
    @Getter String prefix;
    String apiClass;
    O<String> comment = O.empty();
    Map<String, WebMethodInfo> endpoints = Cc.m();
    Map<String, ApiDtoClassModel> classes = Cc.m();

    public WebMethodInfo getMethod(String s) {
        return endpoints.get(s);
    }

    public ApiDtoClassModel getClass(String s) {
        return classes.get(s);
    }

    public Set<WebMethodInfo> getMethods() {
        final TreeSet<WebMethodInfo> objects = new TreeSet<>(Comparator.comparing($ -> $.getFullMethodPath()));
        objects.addAll(endpoints.values());
        return objects;
    }

    public Set<ApiDtoClassModel> getClasses() {
        final TreeSet<ApiDtoClassModel> objects = new TreeSet<>(Comparator.comparing($ -> $.getName()));
        objects.addAll(classes.values());
        return objects;
    }

    public String getCommentOrNull() {
        return comment.map($ -> $.replace("\n\n", "*\n⭐")).filter(St::isNotNullOrEmpty).orElse(null);
    }
}
