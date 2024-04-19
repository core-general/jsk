package sk.services.comparer;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sk.services.async.AsyncSingleThreadedImpl;
import sk.services.async.IAsync;
import sk.services.comparer.model.CompareResult;
import sk.services.comparer.model.CompareResultDif;
import sk.utils.functional.F0;
import sk.utils.functional.F1;
import sk.utils.functional.F3;
import sk.utils.ifaces.Identifiable;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Fu;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.util.*;
import java.util.concurrent.Future;

@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class CompareTool<T extends Identifiable<String>, I> {
    protected @Inject Optional<IAsync> async = Optional.of(new AsyncSingleThreadedImpl());

    private final F1<List<CompareItem<T>>, Map<String, T>> initialConverter =
            list -> list.parallelStream()
                    .distinct()
                    .collect(Cc.toM($ -> $.getId(), $ -> $.getItemInfo()));
    private final F3<Map<String, T>, HashSet<String>, F1<List<T>, I>, CompareResultDif<T, I>>
            finalConverter = (init, notExist, processor) -> new CompareResultDif<>(
            init.entrySet().parallelStream().filter($ -> notExist.contains($.getKey()))
                    .map($ -> $.getValue()).sorted(
                    Comparator.comparing($ -> $.getId())).collect(Cc.toL()),
            processor.apply(new ArrayList<>(init.values()))
    );

    protected final CompareResult<T, I> innerCompare(
            F0<List<CompareItem<T>>> firstSource,
            F0<List<CompareItem<T>>> secondSource,
            F1<List<T>, I> metaInfoProcessor
    ) {
        try {
            return async.get().coldTaskFJP().call(() -> {
                final List<List<CompareItem<T>>> results = async.get().supplyParallel(Cc.l(firstSource, secondSource));
                final Future<Map<String, T>> ftr1 = async.get().coldTaskFJP().call(() -> initialConverter.apply(results.get(0)));
                final Future<Map<String, T>> ftr2 = async.get().coldTaskFJP().call(() -> initialConverter.apply(results.get(1)));
                Map<String, T> first = ftr1.get();
                Map<String, T> second = ftr2.get();

                final HashSet<String> existInFirstNotInSecond = Cc.removeAll(new HashSet<>(first.keySet()), second.keySet());
                final HashSet<String> existInSecondNotInFirst = Cc.removeAll(new HashSet<>(second.keySet()), first.keySet());

                final HashSet<String> inBoth = Cc.retainAll(new HashSet<>(first.keySet()), second.keySet());
                final List<X2<T, T>> existButDifferent = inBoth.stream()
                        .filter($ -> Fu.notEqual(first.get($), second.get($)))
                        .map($ -> X.x(first.get($), second.get($)))
                        .collect(Cc.toL());

                return new CompareResult<T, I>(
                        finalConverter.apply(first, existInFirstNotInSecond, metaInfoProcessor),
                        finalConverter.apply(second, existInSecondNotInFirst, metaInfoProcessor),
                        existButDifferent
                );
            }).get();
        } catch (Exception e) {
            return Ex.thRow(e);
        }
    }
}
