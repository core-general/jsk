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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import sk.utils.functional.F1;
import sk.utils.functional.F2;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.web.WebMethodType;
import sk.web.annotations.WebAuth;
import sk.web.annotations.WebIdempotence;
import sk.web.annotations.WebUserToken;
import sk.web.renders.WebFilterOutput;
import sk.web.renders.WebRender;
import sk.web.utils.WebApiMethod;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

@Getter
@RequiredArgsConstructor
public class WebRequestInnerContextImpl<API> implements WebRequestInnerContext {
    final boolean shouldStop;
    final String serverRequestId;
    final ZonedDateTime startTime;
    final long reqStartNano4Dif;
    final WebRequestIp ipInfo;
    final WebApiMethod<API> methodInfo;
    final WebMethodType apiMethodType;
    final O<F1<Class<? extends Exception>, O<F2<Exception, WebRequestInnerContext, WebFilterOutput>>>> exceptionProcessors;
    final O<WebAuth> webAuth;
    final O<WebIdempotence> webIdempotence;
    final WebRender webRender;

    @Getter(AccessLevel.NONE) final WebRequestOuterFullContext outerFull;
    @Getter(AccessLevel.NONE) final Map<WebRequestVariable, Object> innerRequestVariables = Cc.m();

    private volatile String changedToken;

    @Override
    public String getUrlPathPart() {
        return outerFull.getUrlPathPart();
    }

    @Override
    public String getRequestType() {
        return outerFull.getRequestType();
    }

    @Override
    public O<String> getUserToken() {
        return O.ofNull(changedToken)
                .or(() -> methodInfo.getAnnotation(WebUserToken.class)
                        .flatMap($ -> $.isParamOrHeader() ? getParamAsString($.paramName()) : getRequestHeader($.paramName())));
    }

    @Override
    public void setUserToken(String token) {
        changedToken = token;
    }

    @Override
    public SortedSet<String> getRequestHeaderNames() {
        return outerFull.getRequestHeaderNames();
    }

    @Override
    public O<String> getRequestHeader(String name) {
        return outerFull.getRequestHeader(name);
    }

    @Override
    public boolean isMultipart() {
        return outerFull.isMultipart();
    }

    @Override
    public SortedMap<String, String> getNonMultipartParamInfo() {
        return outerFull.getNonMultipartParamInfo();
    }

    @Override
    public O<List<String>> getMultipartParamInfo() {
        return outerFull.getMultipartParamInfo()
                .map($ -> $.stream().map(x -> x.getName() + ":" + x.getSize()).collect(Cc.toL()));
    }

    @Override
    public O<String> getParamAsString(String param) {
        return outerFull.getParamAsString(param);
    }

    @Override
    public O<byte[]> getParamAsBytes(String param) {
        return outerFull.getParamAsBytes(param);
    }

    @Override
    public O<byte[]> getBody() {
        return outerFull.getBody();
    }

    @Override
    public void setResponseHeader(String key, String value) {
        outerFull.setResponseHeader(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> O<T> getVariableValue(WebRequestVariable variable) {
        return O.ofNull((T) innerRequestVariables.get(variable));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> O<T> setVariableValue(WebRequestVariable variable, T newValue) {
        return O.ofNull((T) innerRequestVariables.put(variable, newValue));
    }

    @Override
    public void addProblemHeader() {
        outerFull.addProblemHeader();
    }

    @Override
    public Map<String, String> getAllParamValues() {
        return outerFull.getAllParamsAsStrings();
    }
}
