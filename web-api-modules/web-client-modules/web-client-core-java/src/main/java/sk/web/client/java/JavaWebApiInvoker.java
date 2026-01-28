package sk.web.client.java;

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

import sk.utils.statics.Re;
import sk.web.client.WebApiInvoker;
import sk.web.client.WebMethodInvokeHandler;
import sk.web.infogatherer.WebClassInfo;

/**
 * Java implementation of WebApiInvoker using dynamic proxies.
 * Uses Re.singleProxy (java.lang.reflect.Proxy) for runtime proxy generation.
 */
public class JavaWebApiInvoker implements WebApiInvoker {

    @Override
    public <API> API createClient(Class<API> apiCls, WebClassInfo classInfo, WebMethodInvokeHandler methodInvoker) {
        final Object proxier = new Object();

        return Re.singleProxy(apiCls, (proxy, method, args) -> {
            String methodName = method.getName();

            // Handle Object methods that are not part of the API
            if (classInfo.getMethod(methodName) == null) {
                switch (methodName) {
                    case "getClass":
                        return apiCls;
                    case "hashCode":
                        return proxier.hashCode();
                    case "equals":
                        return args != null && args.length > 0 && proxier.equals(args[0]);
                    case "toString":
                        return apiCls.getName() + " : " + proxier.hashCode();
                    default:
                        throw new UnsupportedOperationException("Method not supported: " + methodName);
                }
            }

            // Delegate to the method invoker for actual API calls
            return methodInvoker.invoke(methodName, args);
        });
    }
}
