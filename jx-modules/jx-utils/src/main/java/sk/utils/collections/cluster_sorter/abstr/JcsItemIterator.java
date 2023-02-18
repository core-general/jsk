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

import lombok.Getter;
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.functional.F0;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

public class JcsItemIterator<ITEM, EXPAND_DIRECTION, SOURCE extends JcsISource<ITEM>>
        implements Iterator<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> {
    private final List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> items;
    private int currentIndex;
    private final F0<Long> modCountProvider;
    private final long curModCount;
    @Getter private final EXPAND_DIRECTION expandDirection;

    public JcsItemIterator(EXPAND_DIRECTION expandDirection, List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> items,
            F0<Long> modCountProvider) {
        this.items = items;
        this.currentIndex = items.size();
        this.expandDirection = expandDirection;
        this.modCountProvider = modCountProvider;
        curModCount = modCountProvider.apply();
    }

    @Override
    public boolean hasNext() {
        checkModCount();
        return currentIndex > 0;
    }

    @Override
    public JcsItem<ITEM, EXPAND_DIRECTION, SOURCE> next() {
        checkModCount();
        return items.get(--currentIndex);
    }

    private void checkModCount() {
        if (modCountProvider.apply() != curModCount) {
            throw new ConcurrentModificationException();
        }
    }
}
