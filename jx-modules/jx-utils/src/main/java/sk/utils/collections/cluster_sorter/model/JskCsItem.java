package sk.utils.collections.cluster_sorter.model;

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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import sk.utils.collections.cluster_sorter.JskCsSource;

import java.util.Comparator;

@Getter
@AllArgsConstructor
public class JskCsItem<SRC_ID, ITEM> implements Comparable<JskCsItem<SRC_ID, ITEM>> {
    private Comparator<ITEM> comparator;
    private JskCsSource<SRC_ID, ITEM> source;
    private ITEM item;
    @Setter private boolean expandableLastItem;

    @Override
    public int compareTo(@NotNull JskCsItem<SRC_ID, ITEM> o) {
        return comparator.compare(item, o.item);
    }

    public boolean isExpandableLastItem() {
        return expandableLastItem;
    }

    @Override
    public String toString() {
        return item + (expandableLastItem ? "💼" : "");
    }
}
