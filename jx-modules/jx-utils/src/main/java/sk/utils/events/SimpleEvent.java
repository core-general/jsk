package sk.utils.events;

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

import lombok.extern.log4j.Log4j2;
import sk.utils.functional.R;
import sk.utils.statics.Cc;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by kivan on 8/21/15
 */
@Log4j2
public class SimpleEvent {
    final Map<String, R> listeners = Cc.lhm();
    boolean removeOnException;

    private SimpleEvent(boolean removeOnException) {
        this.removeOnException = removeOnException;
    }

    public static SimpleEvent create() {
        return new SimpleEvent(false);
    }

    public static SimpleEvent createRemoveOnException() {
        return new SimpleEvent(true);
    }

    public SimpleEvent listen(String id, R r) {
        listeners.put(id, r);
        return this;
    }

    public void fire() {
        for (Iterator<Map.Entry<String, R>> iterator = listeners.entrySet().iterator(); iterator.hasNext(); ) {
            final Map.Entry<String, R> next = iterator.next();
            try {
                next.getValue().run();
            } catch (Throwable e) {
                log.error("Fail in listener: " + next.getKey(), e);
                if (removeOnException) {
                    iterator.remove();
                }
            }
        }
    }
}
