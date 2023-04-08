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
import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import sk.services.ICoreServices;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.collections.cluster_sorter.backward.model.JcsIBackSource;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.List;

import static sk.utils.collections.cluster_sorter.backward.model.JcsEBackType.BACKWARD;
import static sk.utils.collections.cluster_sorter.backward.model.JcsEBackType.FORWARD;

@RequiredArgsConstructor
public class JcsSqlSource<ITEM> implements JcsIBackSource<ITEM> {
    @Getter
    private final JcsSourceId sourceId;

    private final Class<ITEM> cls;
    private final String tableNameWithSchema;
    private final O<String> defaultWhere; //"generator_id = 'c3014468-9253-4919-b617-ae2e0e723090'"
    private final String itemSelectorField; //"created_at"
    private final String itemSelectorFieldOperation; //">"
    private final F1<ITEM, String> selectedItem2SqlSelector; //{'a':'2022-10-01'}-> '2022-10-01'
    private final Order order;

    private final ICoreServices iCore;
    private final EntityManager entityManager;


    private O<ITEM> positionItem = O.empty();
    private int currentOffsetForward = 0;
    private int currentOffsetBackward = 0;


    @Override
    public JcsList<ITEM> getNextUnseenElements(int limit) {
        return getItemsPrivate(limit, FORWARD);
    }

    @Override
    public JcsList<ITEM> getPreviousUnseenElements(int limit) {
        return getItemsPrivate(limit, BACKWARD);
    }

    @Override
    public void setPositionToItem(ITEM item) {
        this.positionItem = O.of(item);
        currentOffsetForward = 0;
        currentOffsetBackward = 0;
    }

    @Override
    public boolean canSetPosition() {
        return true;
    }

    @NotNull
    private JcsList<ITEM> getItemsPrivate(int limit, JcsEBackType direction) {
        String request = createSql(limit, direction);
        List<ITEM> finalItems = entityManager.createNativeQuery(request, cls).getResultList();
        updateOffset(finalItems.size(), direction);
        return new JcsList<>(finalItems, finalItems.size() == limit);
    }

    String createSql(int limit, JcsEBackType direction) {
        String request = iCore.free().process("jsk/db/jcs/selectPage.sql.ftl", Cc.m(
                "table", tableNameWithSchema,
                "defaultWhere", defaultWhere.orElse("true"),
                "itemSelector", positionItem.isPresent() ? selectItem(direction) : "",
                "orderByField", itemSelectorField,
                "orderByDirection", (direction == BACKWARD ? switchOrder(order) : order).name(),
                "limit", limit + "",
                "offset", (direction == BACKWARD ? currentOffsetBackward : currentOffsetForward) + ""
        ));
        return request;
    }

    private Order switchOrder(Order order) {
        return switch (order) {
            case ASC -> Order.DESC;
            case DESC -> Order.ASC;
        };
    }

    void updateOffset(int addToOffset, JcsEBackType direction) {
        if (direction == BACKWARD) {
            currentOffsetBackward += addToOffset;
        } else {
            currentOffsetForward += addToOffset;
        }
    }

    private String selectItem(JcsEBackType direction) {
        String template = """
                %s %s %s""".formatted(itemSelectorField, itemSelectorFieldOperation,
                selectedItem2SqlSelector.apply(positionItem.get()));
        return direction == BACKWARD ? "and NOT(" + template + ")" : "and " + template;
    }

/*
select *
from eat.eat_img_pack_info
where track_gen_id = 'c3014468-9253-4919-b617-ae2e0e723090'
  and created_at > '2023-02-03 05:26:32'
order by created_at asc
limit 10 offset 10
*/
}
