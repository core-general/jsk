package sk.db.relational.spring.services.impl;

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

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.db.relational.model.ImportantLogId;
import sk.db.relational.model.JpaImportantLog;
import sk.db.relational.spring.services.RdbTransactionWrapperRequiresNew;
import sk.db.relational.spring.services.dao.repo.JpaImportantLogRepo;
import sk.services.ids.IIds;
import sk.services.json.IJson;
import sk.services.log.ILog;
import sk.services.log.ILogCategory;
import sk.services.log.ILogSeverity;
import sk.services.log.ILogType;
import sk.spring.services.CoreServices;
import sk.utils.functional.O;

import javax.inject.Inject;
import java.util.Map;

@SuppressWarnings("unused")
@Log4j2
public class RdbILogImpl implements ILog {
    @Inject JpaImportantLogRepo impoLog;
    @Inject IIds ids;
    @Inject IJson json;
    @Inject CoreServices core;
    @Inject RdbTransactionWrapperRequiresNew trans;

    @Override
    public void uni(ILogSeverity severity, ILogCategory category, String type, Map<String, Object> info, ILogType logType) {
        if (logType == ILogType.AGG || logType == ILogType.BOTH) {
            try {
                JpaImportantLog toLogTemp = createImportantLog(false, category.name(), type, info);
                toLogTemp = getImportantLog(toLogTemp.getId()).orElse(toLogTemp);
                save(toLogTemp);
            } catch (Exception ignored) {
            }
        }
        if (logType == ILogType.LOG || logType == ILogType.BOTH) {
            try {
                JpaImportantLog toLogTemp = createImportantLog(true, category.name(), type, info);
                save(toLogTemp);
            } catch (Exception ignored) {
            }
        }
    }

    private O<JpaImportantLog> getImportantLog(ImportantLogId id) {
        return O.of(impoLog.findById(id));
    }

    private void save(JpaImportantLog importantLog) {
        try {
            //we have to save log independently from other transactions
            trans.transactionalRunForceNew(() -> impoLog.save(importantLog));
        } catch (Throwable e) {
            log.error("", e);
        }
    }

    private JpaImportantLog createImportantLog(boolean randomId, @NotNull String category, @Nullable String type,
            Map<String, Object> info) {
        return new JpaImportantLog(
                new ImportantLogId(randomId ? ids.shortId() : ids.text2Uuid(category + type + info)),
                category, type, json.to(info, true), null, null, null
        );
    }

    @Override
    public void clearAll() {
        impoLog.deleteAll();
    }
}

