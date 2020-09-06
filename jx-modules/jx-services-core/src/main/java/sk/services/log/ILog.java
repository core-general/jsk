package sk.services.log;

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

import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;

import java.util.Map;

import static sk.services.log.ILogSeverity.*;

@SuppressWarnings("unused")
public interface ILog {
    default void log(ILogCategory category, String type, Map<String, Object> info) {
        uni(INFO, category, type, info, ILogType.LOG);
    }

    default void log(ILogSeverity severity, ILogCategory category, String type, Map<String, Object> info) {
        uni(severity, category, type, info, ILogType.LOG);
    }

    default void logTrace(ILogCategory category, String type, Map<String, Object> info) {
        log(TRACE, category, type, info);
    }

    default void logDebug(ILogCategory category, String type, Map<String, Object> info) {
        log(DEBUG, category, type, info);
    }

    default void logInfo(ILogCategory category, String type, Map<String, Object> info) {
        log(INFO, category, type, info);
    }

    default void logError(ILogCategory category, String type, Map<String, Object> info) {
        log(ERROR, category, type, info);
    }


    public default void logExc(Throwable e) {
        logExc(ILogCatDefault.EXCEPT, e, O.empty());
    }

    public default void logExc(Throwable e, O<String> moreInfo) {
        logExc(ILogCatDefault.EXCEPT, e, moreInfo);
    }

    public default void logExc(ILogCategory cat, Throwable e, O<String> moreInfo) {
        Map<String, Object> info = Cc.m("stacktrace", Ex.getInfo(e));
        moreInfo.ifPresent($ -> info.put("moreInfo", $));
        uni(ERROR, cat, "exception", info, ILogType.LOG);
    }


    default void agg(ILogSeverity severity, ILogCategory category, String type, Map<String, Object> info) {
        uni(severity, category, type, info, ILogType.AGG);
    }

    default void aggTrace(ILogCategory category, String type, Map<String, Object> info) {
        agg(TRACE, category, type, info);
    }

    default void aggDebug(ILogCategory category, String type, Map<String, Object> info) {
        agg(DEBUG, category, type, info);
    }

    default void aggInfo(ILogCategory category, String type, Map<String, Object> info) {
        agg(INFO, category, type, info);
    }

    default void aggError(ILogCategory category, String type, Map<String, Object> info) {
        agg(ERROR, category, type, info);
    }

    default void both(ILogSeverity severity, ILogCategory category, String type, Map<String, Object> info) {
        uni(severity, category, type, info, ILogType.BOTH);
    }

    default void bothTrace(ILogCategory category, String type, Map<String, Object> info) {
        both(TRACE, category, type, info);
    }

    default void bothDebug(ILogCategory category, String type, Map<String, Object> info) {
        both(DEBUG, category, type, info);
    }

    default void bothInfo(ILogCategory category, String type, Map<String, Object> info) {
        both(INFO, category, type, info);
    }

    default void bothError(ILogCategory category, String type, Map<String, Object> info) {
        both(ERROR, category, type, info);
    }

    void uni(ILogSeverity severity, ILogCategory category, String type, Map<String, Object> info,
            ILogType logType);


    default void clearAll() {

    }
}


