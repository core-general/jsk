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

import sk.utils.statics.Cc;

import java.util.List;

record JcsDynamicPagingHelper(int pageIndex, int size, int firstElementIndex, int lastElementIndex) {
    public static JcsDynamicPagingHelper help(int offset, int limit) {
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException(offset + " " + limit);
        }
        int vlimit = limit;
        int page = -1;
        while (true) {
            int pageOfFirstElement = (offset) / vlimit;
            int pageOfSecondElement = (offset + limit - 1) / vlimit;
            if (pageOfFirstElement == pageOfSecondElement) {
                page = pageOfFirstElement;
                break;
            }
            vlimit++;
        }
        int firstElemIndex = offset % vlimit;
        return new JcsDynamicPagingHelper(page, vlimit, firstElemIndex, firstElemIndex + limit - 1);
    }

    public <T> List<T> applyToResult(List<T> result) {
        if (firstElementIndex >= result.size()) {
            return Cc.lEmpty();
        }
        int realLastIndex = Math.min(lastElementIndex, result.size() - 1);
        return result.subList(firstElementIndex, realLastIndex);
    }
}
