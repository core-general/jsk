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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.exceptions.JskProblem;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.web.exceptions.IWebExcept;
import sk.web.utils.WebApiMethod;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WebFilterOutput {
    final OneOf<WebReply<?>, WebRenderResult> rawOrRendered;

    public WebRenderResult render(WebRender okRender, IWebExcept badRenderProvider, WebApiMethod<?> method) {
        return okRender.getResult(this, badRenderProvider.getDefaultExceptionRender(), method);
    }

    public <T> O<T> getValue() {
        return rawOrRendered.mapLeft(l -> l.getValOrProblem().collect($ -> (T) $, $ -> null)).oLeft();
    }

    public int getCode() {
        return rawOrRendered.collect(l -> l.getHttpCode(), r -> r.getMeta().getHttpCode());
    }

    public String getRawOrRenderedAsString() {
        return rawOrRendered.collect(l -> l.getValOrProblem().collect(ll -> ll, rr -> rr) + "",
                r -> r.getValue().collect(ll -> ll, rr -> new String(rr)));
    }

    private WebFilterOutput(WebReply<?> raw) {
        rawOrRendered = OneOf.left(raw);
    }

    private WebFilterOutput(WebRenderResult rendered) {
        rawOrRendered = OneOf.right(rendered);
    }

    public static <A> WebFilterOutput empty() {
        return new WebFilterOutput(WebReply.empty());
    }

    public static <A> WebFilterOutput rawValue(int httpCode, A val) {
        return new WebFilterOutput(WebReply.value(httpCode, val));
    }

    public static <A> WebFilterOutput rawProblem(int httpCode, JskProblem problem) {
        return new WebFilterOutput(WebReply.problem(httpCode, problem));
    }

    public static <A> WebFilterOutput rendered(WebRenderResult rendered) {
        return new WebFilterOutput(rendered);
    }

    public static <A> WebFilterOutput withModifiedValue(WebFilterOutput output, F1<? super Object, O<?>> converter) {
        return new WebFilterOutput(output.getRawOrRendered().map(
                l -> WebReply.withModifiedValue(l, converter),
                r -> r
        ));
    }
}
