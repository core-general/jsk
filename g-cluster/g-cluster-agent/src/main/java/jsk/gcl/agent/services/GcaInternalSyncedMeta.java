package jsk.gcl.agent.services;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import sk.services.json.IJson;
import sk.utils.async.locks.JLock;
import sk.utils.async.locks.JLockDecorator;
import sk.utils.functional.C1;
import sk.utils.functional.O;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class GcaInternalSyncedMeta<T> {
    protected volatile T object;
    protected final IJson json;
    protected final String filePath;
    protected final Class<T> cls;
    protected final JLock lock = new JLockDecorator(new ReentrantLock());

    /**
     * If T will be serializable, it will work faster! Otherwise JSON approach is used
     */
    public GcaInternalSyncedMeta(IJson json, String filePath, Class<T> cls, T defaultValue) {
        this.json = json;
        this.filePath = filePath;
        this.cls = cls;
        object = loadFromDisc(filePath).map($ -> json.from($, cls)).orElse(defaultValue);
        flushCurrentToDisc();
    }

    public void readOrUpdateObject(C1<T> updater) throws Exception {
        Exception[] exc = new Exception[1];
        lock.runInLock(() -> {
            T oldValue = switch (object) {
                case Serializable s -> {
                    try {
                        yield (T) SerializationUtils.clone(s);
                    } catch (Exception e) {
                        log.error("Probably not serializable!", e);
                        yield json.from(json.to(object), cls);
                    }
                }
                default -> json.from(json.to(object), cls);
            };
            try {
                updater.accept(object);
                beforeSave(object);
                if (Fu.notEqual(object, oldValue)) {
                    log.debug("Objects are different, saving to disc");
                    flushCurrentToDisc();
                } else {
                    log.debug("Objects are equal, skip saving to disc");
                }
            } catch (Exception e) {
                object = oldValue;
                flushCurrentToDisc();
                exc[0] = e;
            }
        });
        if (exc[0] != null) {
            throw exc[0];
        }
    }

    private void flushCurrentToDisc() {
        String text = json.to(object);
        log.debug("Saving meta file[%d]: %s".formatted(text.length(), filePath));
        saveToDisc(filePath, text);
    }

    protected void saveToDisc(String filePath, String text) {
        Io.reWrite(filePath, w -> w.append(text));
    }

    protected O<String> loadFromDisc(String filePath) {
        return Io.sRead(filePath).oString();
    }

    protected void beforeSave(T updatedObject) {}
}
