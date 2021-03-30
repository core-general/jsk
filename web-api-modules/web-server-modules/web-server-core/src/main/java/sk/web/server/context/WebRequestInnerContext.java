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

import sk.utils.functional.F1;
import sk.utils.functional.F2;
import sk.utils.functional.O;
import sk.web.WebMethodType;
import sk.web.renders.WebFilterOutput;
import sk.web.renders.WebRender;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

public interface WebRequestInnerContext {
    String getServerRequestId();

    long getReqStartNano4Dif();

    WebRequestIp getIpInfo();

    WebMethodType getApiMethodType();


    WebRender getWebRender();


    String getUrlPathPart();

    String getRequestType();

    O<String> getUserToken();

    void setUserToken(String token);


    SortedSet<String> getRequestHeaderNames();

    O<String> getRequestHeader(String name);


    boolean isMultipart();

    SortedMap<String, String> getNonMultipartParamInfo();

    O<List<String>> getMultipartParamInfo();

    O<String> getParamAsString(String param);

    O<byte[]> getParamAsBytes(String param);

    O<byte[]> getBody();

    void setResponseHeader(String key, String value);

    SortedSet<String> getResponseHeaderNames();

    O<String> getResponseHeader(String name);


    <T> O<T> getVariableValue(WebRequestVariable variable);

    <T> O<T> setVariableValue(WebRequestVariable variable, T newValue);


    O<F1<Class<? extends Exception>, O<F2<Exception, WebRequestInnerContext, WebFilterOutput>>>> getExceptionProcessors();

    void addProblemHeader();

    boolean isShouldStop();

    O<sk.web.annotations.WebAuth> getWebAuth();

    O<sk.web.annotations.WebIdempotence> getWebIdempotence();

    java.time.ZonedDateTime getStartTime();

    Map<String, String> getAllParamValues();

    String getRequestHash();
}
