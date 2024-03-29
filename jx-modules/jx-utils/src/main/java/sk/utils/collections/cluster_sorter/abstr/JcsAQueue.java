package sk.utils.collections.cluster_sorter.abstr;

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

import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JcsPollResult;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class JcsAQueue<ITEM, EXPAND_DIRECTION, SOURCE extends JcsISource<ITEM>>
        implements JcsIQueue<ITEM, EXPAND_DIRECTION, SOURCE> {
    protected long modCount = 0;

    @Override
    public void addAllRespectItem(List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> items, O<ITEM> _itemToRespect,
            EXPAND_DIRECTION expandDirection) {
        O<ITEM> toRespect = _itemToRespect.or(() -> getLastConsumedItem(expandDirection).map($ -> $.getItem()));

        Map<Boolean, List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>>> split =
                items.stream().collect(Collectors.groupingBy(
                        item -> toRespect.map(
                                        itemToRespect -> item.getComparator().compare(item.getItem(), itemToRespect) >= 0)
                                .orElse(true)
                ));
        List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> addToBeginning = split.getOrDefault(true, Cc.lEmpty());
        List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> didNotGetToMainQueue = split.getOrDefault(false, Cc.lEmpty());
        if (addToBeginning.size() > 0) {
            uniAddAll(addToBeginning, getQueuePartToAddElements(), (item) -> item.getComparator().reversed());
        }
        if (didNotGetToMainQueue.size() > 0) {
            onDidNotGetToMainQueueWhenAddRespectOrder(didNotGetToMainQueue);
        }
    }

    protected abstract List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> getQueuePartToAddElements();

    protected JcsPollResult<ITEM, EXPAND_DIRECTION, SOURCE> uniPoll(
            List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> uniItems, EXPAND_DIRECTION direction) {
        modCount++;
        return new JcsPollResult<>(uniItems.size() > 0 ? O.of(uniItems.remove(uniItems.size() - 1)) : O.empty(), direction);
    }

    protected void uniAddAll(List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> data,
            List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> target,
            F1<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>, Comparator<ITEM>> comparatorSupplier) {
        if (data.size() > 0) {
            modCount++;
            target.addAll(data);
            final Comparator<ITEM> reversed = comparatorSupplier.apply(target.get(0));
            Cc.sort(target, (o1, o2) -> reversed.compare(o1.getItem(), o2.getItem()));
        }
    }
}
