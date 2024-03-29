package sk.outer.graph.listeners;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Map;

public class MgcListenerProcessorResult {
    Map<String, MgcListenerResult> results = Cc.m();

    public void addListenerResult(String listenerId, MgcListenerResult result) {
        results.put(listenerId, result);
    }

    public boolean isError() {
        return results.values().stream().filter($ -> $.isError() && $.isStopper()).count() > 0;
    }

    public O<Throwable> getError() {
        return O.of(results.values().stream().filter($ -> $.isError() && $.isStopper()).findAny()
                .flatMap($ -> $.getException().toOpt()));
    }

    public <T extends MgcListenerResult> void replaceResult(String listenerId, T result) {
        results.put(listenerId, result);
    }

    public <T extends MgcListenerResult> O<T> getResultOf(String listenerId, Class<T> resultClass) {
        return O.ofNull((T) results.get(listenerId));
    }

    public MgcListenerProcessorResult rewriteNoErrorsBy(MgcListenerProcessorResult other) {
        results = results.entrySet().stream()
                //fixing errors so that they are not deleted
                .map($ -> X.x($.getValue().isError() ? $.getKey() + "__err" : $.getKey(), $.getValue()))
                .collect(Cc.toMX2());
        results.putAll(other.results);
        return this;
    }
}
