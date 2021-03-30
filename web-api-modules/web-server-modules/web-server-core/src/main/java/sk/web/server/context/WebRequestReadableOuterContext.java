package sk.web.server.context;

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

import sk.utils.functional.O;

import javax.servlet.http.Part;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

public interface WebRequestReadableOuterContext {
    String getRequestType();

    String getUrlPathPart();

    String getIp();

    SortedSet<String> getRequestHeaderNames();

    O<String> getRequestHeader(String name);

    SortedSet<String> getResponseHeaderNames();

    O<String> getResponseHeader(String name);

    Map<String, String> getAllParamsAsStrings();

    boolean isMultipart();

    O<String> getParamAsString(String param);

    O<byte[]> getParamAsBytes(String param);

    O<byte[]> getBody();

    SortedMap<String, String> getNonMultipartParamInfo();

    O<List<Part>> getMultipartParamInfo();

    String getRequestHash();
}
