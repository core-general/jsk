package sk.db.relational.spring.services;

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

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.querydsl.QPageRequest;
import sk.db.relational.utils.ReadWriteRepo;
import sk.services.log.ILog;
import sk.services.log.ILogCategory;
import sk.services.profile.IAppProfile;
import sk.utils.statics.Ex;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static sk.utils.statics.Cc.m;

@SuppressWarnings({"ForLoopReplaceableByForEach", "UnnecessaryLabelOnBreakStatement", "unused"})
@AllArgsConstructor
@NoArgsConstructor
public class RdbIterator {
    @Inject ILog log;
    @Inject Optional<IAppProfile> profile = Optional.empty();

    public <T, ID extends Serializable> long iterate(
            Consumer<T> toApply,
            ILogCategory logCategory,
            String logSubCategorySingleFail,
            ReadWriteRepo<T, ID> repo,
            Predicate query,
            OrderSpecifier<?>... ordering) {
        int pageSize = profile.map($ -> $.getProfile().isForDefaultTesting()).orElse(false) ? 5 : 1000;
        int pageNumber = 0;
        long itemCount = 0;
        finish:
        while (pageNumber < Integer.MAX_VALUE) {
            Page<T> all = repo.findAll(query,
                    ordering.length > 0
                            ? new QPageRequest(pageNumber, pageSize, ordering)
                            : new QPageRequest(pageNumber, pageSize));

            List<T> content = all.getContent();
            for (int i = 0, contentSize = content.size(); i < contentSize; i++) {
                T row = content.get(i);
                try {
                    toApply.accept(row);
                    itemCount++;
                } catch (Exception e) {
                    log.logError(logCategory, logSubCategorySingleFail, m("row", row + "", "e", Ex.getInfo(e)));
                }
            }
            if (all.getContent().size() < pageSize) {
                break finish;
            }
            pageNumber++;
        }
        return itemCount;
    }
}
