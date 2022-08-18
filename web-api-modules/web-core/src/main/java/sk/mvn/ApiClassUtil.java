package sk.mvn;

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

import sk.mvn.model.ApiClassModel;
import sk.services.json.IJson;
import sk.services.nodeinfo.model.ApiBuildInfo;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Io;
import sk.utils.statics.St;

import java.util.List;
import java.util.stream.Collectors;

public class ApiClassUtil {
    private IJson json;
    O<String> pathForGet;
    F1<String, O<String>> getDataForApiPath;

    public ApiClassUtil(IJson json) {
        this(json, O.empty(), path -> Io.getResource(path));
    }

    public ApiClassUtil(IJson json, O<String> pathForGet, F1<String, O<String>> getDataForApiPath) {
        this.json = json;
        this.pathForGet = pathForGet;
        this.getDataForApiPath = getDataForApiPath;
    }

    public String calculateHashCode(String methodPath, String returnType, List<String> parameterTypes) {
        String toHash = methodPath +
                returnType +
                Cc.stream(parameterTypes).collect(Collectors.joining());
        return toHash;
    }

    public <T> O<ApiClassModel> getApiClassFromResources(Class<T> apiCls) throws RuntimeException {
        String apiPath =
                pathForGet.map($ -> St.endWith($, "/")).orElse("") + "__jsk_util/web_api/" + apiCls.getName().replace(".", "_") +
                        ".json";
        return getDataForApiPath.apply(apiPath).map($ -> json.from($, ApiClassModel.class));
    }

    public <T> void saveApiClassToResources(String outputPath, ApiClassModel model) {
        Io.reWrite(St.endWith(outputPath, "/") + "__jsk_util/web_api/" + model.getApiClass().replace(".", "_") + ".json",
                w -> w.append(json.to(model, true)));
    }

    public O<ApiBuildInfo> getVersionAndBuildTimeFromResources() {
        String apiPath = pathForGet.map($ -> St.endWith($, "/")).orElse("") + "__jsk_util/web_api/__buildInfo.json";
        return getDataForApiPath.apply(apiPath).map($ -> json.from($, ApiBuildInfo.class));
    }

    public void saveVersionAndBuildTime(String outPath, ApiBuildInfo info) {
        Io.reWrite(St.endWith(outPath, "/") + "/__jsk_util/web_api/__buildInfo.json",
                w -> w.append(json.to(info, true)));
    }
}
