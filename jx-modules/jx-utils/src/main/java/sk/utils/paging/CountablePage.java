package sk.utils.paging;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import sk.utils.functional.F1;
import sk.utils.statics.Cc;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CountablePage<A> extends SimplePage<A, Integer> {
    Integer prevPageId;
    Integer numOfPages;
    Integer curPage;

    public CountablePage(List<A> data, Integer nextPageAccessor, Integer prevPageId, Integer numOfPages,
            Integer curPage) {
        super(data, nextPageAccessor);
        this.prevPageId = prevPageId;
        this.numOfPages = numOfPages;
        this.curPage = curPage;
    }

    @Override
    public <D> CountablePage<D> map(F1<A, D> f) {
        return new CountablePage<>(Cc.map(data, f), nextPageId, prevPageId, numOfPages, curPage);
    }
}
