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

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.backward.impl.strategies.JcsIBackBatch;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.tuples.X;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

@RequiredArgsConstructor
public class JcsSqlBatch<ITEM extends JcsBatchableItem, SOURCE extends JcsSqlSource<ITEM>>
        implements JcsIBackBatch<ITEM, SOURCE> {
    public static final String SOURCE_ID = "_source_id_";
    public static final String DIRECTION = "_direction_";
    public static final String UNION = "\nUNION\n";

    private final NamedParameterJdbcOperations namedJdbc;

    private final Class<ITEM> cls;

    @Override
    public Map<JcsSourceId, Map<JcsEBackType, JcsList<ITEM>>> getNextElements(
            Collection<SOURCE> sourcesToBatch,
            Map<JcsSourceId, Map<JcsEBackType, Integer>> neededCountsPerSourcePerDirection
    ) {
        String finalSql = sourcesToBatch.stream().map($ -> {
            Map<JcsEBackType, Integer> limits = neededCountsPerSourcePerDirection.get($.getSourceId());
            String directionsQuery = limits.entrySet().stream().map(dirLim -> {
                String selectForSource = $.createSql(dirLim.getValue(), dirLim.getKey());
                selectForSource = selectForSource.replace("select *", "select '%s' as %s, '%s' as %s, *".formatted(
                        $.getSourceId().toString(), SOURCE_ID,
                        dirLim.getKey().toString(), DIRECTION
                ));
                return selectForSource;
            }).collect(joining(UNION));
            return directionsQuery;
        }).collect(joining(UNION));

        List<ITEM> items = namedJdbc.queryForList(finalSql, Cc.m(), cls);
        Map<JcsSourceId, Map<JcsEBackType, JcsList<ITEM>>> collect =
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
        return collect;
    }
}
