package sk.db.relational.utils.cluster_sorter;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.backward.impl.strategies.JcsIBackBatch;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.tuples.X;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

@RequiredArgsConstructor
public class JcsSqlBatch<ITEM extends JcsBatchableItem, SOURCE extends JcsSqlSource<ITEM>>
        implements JcsIBackBatch<ITEM, SOURCE> {
    public static final String SOURCE_ID = "_source_id_";
    public static final String DIRECTION = "_direction_";
    public static final String UNION = "\nUNION\n";

    private final EntityManager entityManager;

    private final Class<ITEM> cls;

    @Override
    public Map<JcsSourceId, Map<JcsEBackType, JcsList<ITEM>>> getNextElements(
            Collection<SOURCE> sourcesToBatch,
            Map<JcsSourceId, Map<JcsEBackType, Integer>> neededCountsPerSourcePerDirection
    ) {
        int[] counter = new int[]{0};
        String firstPart = "with " + sourcesToBatch.stream().map($ -> {
            Map<JcsEBackType, Integer> limits = neededCountsPerSourcePerDirection.get($.getSourceId());
            String directionsQuery = limits.entrySet().stream().map(dirLim -> {
                String selectForSource = $.createSql(dirLim.getValue(), dirLim.getKey());
                selectForSource = selectForSource.replace("select *", "select '%s' as %s, '%s' as %s, *".formatted(
                        $.getSourceId().toString(), SOURCE_ID,
                        dirLim.getKey().toString(), DIRECTION
                ));
                return """
                        var_%d as (%s)""".formatted(counter[0]++, selectForSource);
            }).collect(joining(","));
            return directionsQuery;
        }).collect(joining(","));

        String secondPart = IntStream.range(0, counter[0]).mapToObj(i -> "select * from var_" + i).collect(joining(UNION));

        String finalSql = firstPart + " " + secondPart;

        List<ITEM> items = entityManager.createNativeQuery(finalSql, cls).getResultList();
        Map<JcsSourceId, Map<JcsEBackType, JcsList<ITEM>>> result =
                items.stream().collect(groupingBy($ -> $.getSourceId(),
                        collectingAndThen(groupingBy($1 -> $1.getDirection()),
                                XX -> XX.entrySet().stream().map($$ -> {
                                    if ($$.getValue().size() == 0) {
                                        return null;
                                    }
                                    return X.x($$.getKey(), new JcsList<>(
                                            $$.getValue(),
                                            neededCountsPerSourcePerDirection.get($$.getValue().get(0).getSourceId())
                                                    .get($$.getKey()) == $$.getValue().size()
                                    ));
                                }).filter(Fu.notNull()).collect(Cc.toMX2()))));
        //update offsets
        sourcesToBatch.forEach(source -> {
            O.ofNull(result.get(source.getSourceId())).ifPresent($ -> $.forEach((direction, list) -> {
                source.updateOffset(list.getItems().size(), direction);
            }));
        });

        return result;
    }
}
