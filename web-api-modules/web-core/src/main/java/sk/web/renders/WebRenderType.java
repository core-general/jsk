package sk.web.renders;

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
import sk.services.bean.IServiceLocator;
import sk.utils.functional.O;
import sk.web.renders.inst.*;

@AllArgsConstructor
public enum WebRenderType implements WebRenderProvider {
    JSON(WebJsonRender.class),
    JSON_PRETTY(WebJsonPrettyRender.class),
    BASE64_BYTES(WebB64Render.class),
    RAW_STRING(WebRawStringRender.class),
    RAW_BYTE_ZIPPED(WebRawByteRenderZipped.class),
    ;

    Class<? extends WebRender> render;

    @Override
    public O<? extends WebRender> getRender(IServiceLocator context) {
        return context.getService(render);
    }
}
