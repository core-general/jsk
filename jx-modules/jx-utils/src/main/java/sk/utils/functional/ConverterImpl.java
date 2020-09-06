package sk.utils.functional;

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

public class ConverterImpl<A, B> implements Converter<A, B> {
    F1<A, B> there;
    F1<B, A> back;

    public ConverterImpl(F1<A, B> there, F1<B, A> back) {
        this.there = there;
        this.back = back;
    }

    @Override
    public B convertThere(A a) {
        return there.apply(a);
    }

    @Override
    public A convertBack(B b) {
        return back.apply(b);
    }
}
