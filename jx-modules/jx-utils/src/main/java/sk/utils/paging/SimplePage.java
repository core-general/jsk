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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sk.utils.functional.F1;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class SimplePage<A, B> {
    List<A> data;
    B nextPageId;

    public <D> SimplePage<D, B> map(F1<A, D> f) {
        return new SimplePage<>(Cc.map(data, f), nextPageId);
    }

    public static <A, B, C> List<C> getAll(F1<Optional<B>, SimplePage<A, B>> getter, F1<A, C> conv) {
        Optional<B> curPage = empty();
        List<C> toRet = Cc.l();
        while (curPage != null) {
            SimplePage<A, B> page = getter.apply(curPage);
            toRet.addAll(page.map(conv).getData());
            if (page.getNextPageId() == null
                    || Fu.equal(page.getNextPageId(), curPage.orElse(null))) {
                break;
            }
            curPage = page.getNextPageId() == null ? null /*we stop*/ : of(page.getNextPageId()); /*we continue*/

        }
        return toRet;
    }

    public static <A, B> List<A> getAll(F1<Optional<B>, SimplePage<A, B>> getter) {
        return getAll(getter, a -> a);
    }
}
