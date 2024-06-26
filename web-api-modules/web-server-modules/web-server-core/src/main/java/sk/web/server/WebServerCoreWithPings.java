package sk.web.server;

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

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sk.exceptions.JskProblem;
import sk.services.bytes.IBytes;
import sk.services.free.IFree;
import sk.services.nodeinfo.INodeInfo;
import sk.services.nodeinfo.model.IServerInfo;
import sk.services.rescache.IResCache;
import sk.utils.async.locks.JLock;
import sk.utils.async.locks.JLockDecorator;
import sk.utils.functional.C1;
import sk.utils.functional.F3;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.web.WebMethodType;
import sk.web.exceptions.IWebExcept;
import sk.web.infogatherer.WebClassInfo;
import sk.web.renders.WebFilterOutput;
import sk.web.renders.WebRender;
import sk.web.renders.inst.WebJsonPrettyRender;
import sk.web.renders.inst.WebRawStringRender;
import sk.web.server.context.WebContextHolder;
import sk.web.server.context.WebRequestInnerContext;
import sk.web.server.context.WebRequestOuterFullContext;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterNext;
import sk.web.utils.WebApiMethod;
import sk.web.utils.WebUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static sk.web.WebMethodType.GET;
import static sk.web.WebMethodType.POST_FORM;

@Slf4j
public class WebServerCoreWithPings<T> extends WebServerCore<T> {
    @Inject WebJsonPrettyRender jsonRender;
    @Inject WebRawStringRender rawRender;
    @Inject IWebExcept except;
    @Inject INodeInfo nodeInfo;
    @Inject IFree free;
    @Inject IBytes bytes;
    @Inject IResCache resources;
    @Inject WebContextHolder ctxHolder;

    public WebServerCoreWithPings(Class<T> tClass, T impl) {
        super(tClass, impl);
    }

    public WebServerCoreWithPings(Class<T> tClass, T impl, String basePath) {
        super(tClass, impl, basePath);
    }

    @Override
    protected void addAuxiliaryMethods(Class<T> apiClass, WebServerContextWithInfo env) {
        super.addAuxiliaryMethods(apiClass, env);

        final String base = St.endWith(WebUtils.getBaseApiPath(getBasePath(), apiClass), "/");

        final TreeSet<WebServerFilter> filters = validateFilters(getDefaultFilters());
        final WebApiMethod<T> api = new WebApiMethod<>(apiClass, O.empty(), true);

        final F3<WebMethodType, WebServerFilterNext, WebRender, C1<WebRequestOuterFullContext>> processor =
                (wmt, out, render) -> context -> {
                    try {
                        final WebRequestInnerContext innerCtx =
                                prepareInnerContext(context, api, wmt, getExceptionProcessors(O.empty()), O.empty(), () -> {},
                                        O.empty(), render);
                        final WebServerFilterNext filterChain = createFilterChain(api, out, innerCtx, filters);

                        final WebFilterOutput apply = filterChain.invokeNext();

                        context.setResponse(render.getResult(apply, except.getDefaultExceptionRender(), api), O.empty());
                    } catch (Exception e) {
                        log.error("", e);
                        context.setError(500, except.getDefaultExceptionRender(), JskProblem.code(INTERNAL_ERROR), api);
                    }
                };

        {//todo move to WebJettyContextConsumer4Spark or in some other way
            final String pingPath = base + "ping";
            env.addGet(pingPath, processor.apply(GET, () -> WebFilterOutput.empty(), jsonRender), O.empty());
            env.addPost(pingPath, processor.apply(POST_FORM, () -> WebFilterOutput.empty(), jsonRender), false, O.empty());
        }


        {//todo move to WebJettyContextConsumer4Spark or in some other way
            final String infoPath = base + "jskinfo";
            final WebServerFilterNext infoProcessor =
                    () -> {
                        List<String> categories = getCategoriesFromRequest();
                        final IServerInfo info = nodeInfo.getCurrentServerInfo();
                        if (categories.size() == 0) {
                            return WebFilterOutput.rawValue(200, Cc.m(
                                    "parameterNameAndType", "json - List<String> needInfo",
                                    "value", info.getCategories()
                            ));
                        } else {
                            return WebFilterOutput.rawValue(200, info.getInfoGetterByFilter().apply(categories));
                        }
                    };
            env.addGet(infoPath, processor.apply(GET, infoProcessor, jsonRender), O.empty());
            env.addPost(infoPath, processor.apply(POST_FORM, infoProcessor, jsonRender), false, O.empty());
        }

        {
            final String apiInfo = base + "api-info";
            final WebServerFilterNext apiInfoProcessor = () -> {
                checkBasic();
                return WebFilterOutput.rawValue(200, htmlInfoCache.getValue());
            };
            env.addGet(apiInfo, processor.apply(GET, apiInfoProcessor, rawRender), O.empty());
            env.addPost(apiInfo, processor.apply(POST_FORM, apiInfoProcessor, rawRender), false, O.empty());
        }
        {
            final String postmanApiInfo = base + "api-info-postman";
            final WebServerFilterNext apiInfoProcessor = () -> {
                checkBasic();
                return WebFilterOutput.rawValue(200, postmanInfoCache.getValue());
            };
            env.addGet(postmanApiInfo, processor.apply(GET, apiInfoProcessor, rawRender), O.empty());
            env.addPost(postmanApiInfo, processor.apply(POST_FORM, apiInfoProcessor, rawRender), false, O.empty());
        }
        {
            final String postmanApiInfo = base + "api-info-swagger";
            final WebServerFilterNext apiInfoProcessor =
                    () -> {
                        checkBasic();
                        return WebFilterOutput.rawValue(200, swaggerInfoCache.getValue());
                    };
            env.addGet(postmanApiInfo, processor.apply(GET, apiInfoProcessor, rawRender), O.empty());
            env.addPost(postmanApiInfo, processor.apply(POST_FORM, apiInfoProcessor, rawRender), false, O.empty());
        }
    }

    private void checkBasic() {
        checkBasicAuth((header) -> ctxHolder.get().getRequestHeader(header), (k, v) -> ctxHolder.get().setResponseHeader(k, v),
                "Api info", false);
    }

    private List<String> getCategoriesFromRequest() {
        final O<String> needInfo = ctxHolder.get().getParamAsString("needInfo");
        if (needInfo.isEmpty()) {
            return Cc.l();
        } else {
            return json.from(needInfo.get(), TypeWrap.getList(String.class));
        }
    }

    protected Map<String, String> getAdditionalParams4Postman() {
        return Cc.m();
    }

    private StringWithLock htmlInfoCache = new StringWithLock() {
        @Override
        protected String createNew() {
            final WebClassInfo apiModel = infoProvider.getClassModel(getApiClass(), getBasePath());

            final String s = free.processHtml("sk/web/server/templates/api_template.html.ftl", Cc.m(
                    "prefixPath", apiModel.getPrefix(),
                    "mainComment", apiModel.getCommentOrNull(),
                    "methodList", apiModel.getMethods(),
                    "classList", apiModel.getClasses()
            ));

            return s.replace("*\n⭐", "<br>");
        }
    };

    private StringWithLock postmanInfoCache = new StringWithLock() {
        @Override
        protected String createNew() {
            final WebClassInfo apiModel = infoProvider.getClassModel(getApiClass(), getBasePath());

            final String s = free.processHtml("sk/web/server/templates/postman_template.json.ftl", Cc.m(
                    "server_name", getApiClass().getSimpleName(),
                    "postman_id", ids.shortIdS(),
                    "url_var_id", ids.shortIdS(),
                    "methods", apiModel.getMethods(),
                    "additionalParams", getAdditionalParams4Postman()
            ));

            return s;
        }
    };

    private StringWithLock swaggerInfoCache = new StringWithLock() {
        @Override
        protected String createNew() {
            //this is what is generated via WebSwaggerMavenPlugin
            O<String> resource = resources.getResource("__jsk_util/swagger/api_specs/" + getApiClass().getSimpleName() + ".json");
            return resource.orElse("OPENAPI SCHEMA DOES NOT EXIST FOR " + getApiClass().getSimpleName());
        }
    };

    private static abstract class StringWithLock {
        private final static String EMPTY = "E";

        private volatile String target = EMPTY;
        private final JLock lock = new JLockDecorator();

        protected abstract String createNew();

        public String getValue() {
            String tmp = target;
            if (EMPTY.equals(tmp)) {
                return lock.getInLock(() -> {
                    String tmp2 = target;
                    if (EMPTY.equals(tmp2)) {
                        tmp2 = createNew();
                    }
                    target = tmp2;
                    return tmp2;
                });
            } else {
                return tmp;
            }
        }
    }
}
