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

import sk.utils.functional.OneOf;

public interface WebRender {
    String contentHeaderProvider(Object val, OneOf<String, byte[]> processed);

    boolean allowDeflation(Object val, OneOf<String, byte[]> processed);

    OneOf<String, byte[]> valueProvider(Object val);

    default public WebRenderResult getResult(WebReply<?> reply, WebRender problemRender) {
        final Object toRender = reply.getValOrProblem().collectSelf();
        final OneOf<String, byte[]> rendered = reply.getValOrProblem()
                .collect($ -> valueProvider($), $ -> problemRender.valueProvider($));

        return new WebRenderResult(new WebReplyMeta(reply.getHttpCode(),
                contentHeaderProvider(toRender, rendered), allowDeflation(toRender, rendered),
                reply.getValOrProblem().collect(__ -> false, __ -> true)), rendered);
    }

    default public WebRenderResult getResult(WebFilterOutput reply, WebRender problemRender) {
        return reply.getRawOrRendered().collect($ -> getResult($, problemRender), $ -> $);
    }
}
