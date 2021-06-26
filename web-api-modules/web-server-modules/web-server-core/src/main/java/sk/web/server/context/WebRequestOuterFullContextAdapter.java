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

import lombok.AllArgsConstructor;
import sk.utils.functional.O;
import sk.web.redirect.WebRedirectResult;
import sk.web.renders.WebRenderResult;

import javax.servlet.http.Part;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

@AllArgsConstructor
public abstract class WebRequestOuterFullContextAdapter extends WebRequestOuterFullContext {
    WebRequestOuterFullContext delegate;

    @Override
    public String getRequestType() {return delegate.getRequestType();}

    @Override
    public String getUrlPathPart() {return delegate.getUrlPathPart();}

    @Override
    public String getIp() {return delegate.getIp();}

    @Override
    public SortedSet<String> getRequestHeaderNames() {return delegate.getRequestHeaderNames();}

    @Override
    public O<String> getRequestHeader(String name) {return delegate.getRequestHeader(name);}

    @Override
    public SortedSet<String> getResponseHeaderNames() {return delegate.getResponseHeaderNames();}

    @Override
    public O<String> getResponseHeader(String name) {return delegate.getResponseHeader(name);}

    @Override
    public Map<String, String> getAllParamsAsStrings() {return delegate.getAllParamsAsStrings();}

    @Override
    public boolean isMultipart() {return delegate.isMultipart();}

    @Override
    public O<String> getParamAsString(String param) {return delegate.getParamAsString(param);}

    @Override
    public O<byte[]> getParamAsBytes(String param) {return delegate.getParamAsBytes(param);}

    @Override
    public O<byte[]> getBody() {return delegate.getBody();}

    @Override
    public SortedMap<String, String> getNonMultipartParamInfo() {return delegate.getNonMultipartParamInfo();}

    @Override
    public O<List<Part>> getMultipartParamInfo() {return delegate.getMultipartParamInfo();}

    @Override
    public String getRequestHash() {return delegate.getRequestHash();}

    @Override
    public void redirect(String url) {delegate.redirect(url);}

    @Override
    public void setCookie(String key, String value, int seconds) {delegate.setCookie(key, value, seconds);}

    @Override
    public void setResponseHeader(String key, String value) {delegate.setResponseHeader(key, value);}

    @Override
    public void innerSetResponse(WebRenderResult result,
            O<WebRedirectResult> redirect) {delegate.innerSetResponse(result, redirect);}
}
