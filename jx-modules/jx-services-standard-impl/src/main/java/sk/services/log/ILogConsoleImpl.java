package sk.services.log;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sk.services.json.IJson;

import java.util.Map;

@Slf4j
@NoArgsConstructor
public class ILogConsoleImpl implements ILog {
    @Inject private IJson json;

    public ILogConsoleImpl(IJson json) {
        this.json = json;
    }

    @Override
    public void uni(ILogSeverity severity, ILogCategory category, String type, Map<String, Object> info,
            ILogType logType) {
        switch (severity) {
            case ERROR:
                log.error(category + "__" + type + "_" + json.to(info));
                break;
            case INFO:
                log.info(category + "__" + type + "_" + json.to(info));
                break;
            case DEBUG:
                log.debug(category + "__" + type + "_" + json.to(info));
                break;
            case TRACE:
                log.trace(category + "__" + type + "_" + json.to(info));
                break;
        }
    }
}
