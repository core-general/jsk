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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import sk.exceptions.JskProblem;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class WebReply<A> {
    final int httpCode;
    final OneOf<A, JskProblem> valOrProblem;

    public static WebReply<String> empty() {
        return value(204, "");
    }

    public static <A> WebReply<A> value(int httpCode, A val) {
        return new WebReply<>(httpCode, OneOf.left(val));
    }

    public static <A> WebReply<A> problem(int httpCode, JskProblem problem) {
        return new WebReply<>(httpCode, OneOf.right(problem));
    }

    public static WebReply<?> withModifiedValue(WebReply<?> reply, F1<? super Object, O<?>> converter) {
        final OneOf<?, JskProblem> objectObjectOneOf = reply.getValOrProblem()
                .mapLeft(l -> converter.apply(l))
                .flatMap(
                        converted -> converted.isPresent() ? OneOf.left(converted.get()) : OneOf.left(converted),
                        r -> OneOf.right(r)
                );
        return new WebReply<>(reply.getHttpCode(), objectObjectOneOf);
    }
}
