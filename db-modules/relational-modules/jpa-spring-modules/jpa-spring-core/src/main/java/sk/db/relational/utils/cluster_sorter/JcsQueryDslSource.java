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

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpression;
import lombok.Getter;
import org.springframework.data.querydsl.QPageRequest;
import sk.db.relational.utils.ReadWriteRepo;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.backward.model.JcsIBackSource;
import sk.utils.functional.*;
import sk.utils.statics.Cc;

import java.io.Serializable;
import java.util.List;


public class JcsQueryDslSource<ITEM_ID extends Serializable, ITEM, ORDER_FIELD extends Comparable<ORDER_FIELD>>
        implements JcsIBackSource<ITEM> {
    @Getter
    private JcsSourceId sourceId;
    private ReadWriteRepo<ITEM, ITEM_ID> repo;
    private F0<BooleanExpression> defaultWhere;
    private F1<ITEM, BooleanExpression> itemSelector;
    private F0<ComparableExpression<ORDER_FIELD>> orderingSelector;
    private Order initialOrder;

    private O<ITEM> item = O.empty();
    private int currentOffsetForward = 0;
    private int currentOffsetBackward = 0;


    public JcsQueryDslSource(String sourceId) {
        this.sourceId = new JcsSourceId(sourceId);
    }

    @Override
    public JcsList<ITEM> getNextUnseenElements(int limit) {
        return getItemsPrivate(limit, false, GSetImpl.neu(() -> currentOffsetForward, i -> currentOffsetForward = i));
    }

    @Override
    public JcsList<ITEM> getPreviousUnseenElements(int limit) {
        return getItemsPrivate(limit, true, GSetImpl.neu(() -> currentOffsetBackward, i -> currentOffsetBackward = i));
    }

    @Override
    public void setPositionToItem(ITEM item) {
        this.item = O.of(item);
        currentOffsetForward = 0;
        currentOffsetBackward = 0;
    }

    @Override
    public boolean canSetPosition() {
        return true;
    }


    private JcsList<ITEM> getItemsPrivate(int limit, boolean reverse, GSet<Integer> offset) {
        JcsDynamicPagingHelper helper = JcsDynamicPagingHelper.help(offset.get(), limit);
        BooleanExpression where = this.defaultWhere.get();
        if (item.isPresent()) {
            BooleanExpression itemSelector = this.itemSelector.apply(item.get());
            if (reverse) {
                itemSelector = itemSelector.not();
            }
            where = where.and(itemSelector);
        }
        ComparableExpression<ORDER_FIELD> ordering = this.orderingSelector.apply();
        OrderSpecifier<ORDER_FIELD> orderSpec = switch (initialOrder) {
            case ASC -> reverse ? ordering.desc() : ordering.asc();
            case DESC -> reverse ? ordering.asc() : ordering.desc();
        };

        List<ITEM> items = Cc.list(repo.findAll(where, QPageRequest.of(helper.pageIndex(), helper.size(), orderSpec)));
        List<ITEM> finalItems = helper.applyToResult(items);
        offset.set(offset.get() + finalItems.size());
        return new JcsList<>(finalItems, finalItems.size() == limit);
    }
}
