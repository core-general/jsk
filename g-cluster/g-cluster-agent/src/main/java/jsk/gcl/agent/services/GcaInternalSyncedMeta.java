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
import sk.services.json.IJson;
import sk.utils.async.locks.JLock;
import sk.utils.async.locks.JLockDecorator;
import sk.utils.functional.C1;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class GcaInternalSyncedMeta<T> {
    private volatile T object;
    private final IJson json;
    private final String filePath;
    private final JLock lock = new JLockDecorator(new ReentrantLock());

    public GcaInternalSyncedMeta(IJson json, String filePath, Class<T> cls, T defaultValue) {
        this.json = json;
        this.filePath = filePath;
        object = Io.sRead(filePath).oString().map($ -> json.from($, cls)).orElse(defaultValue);
    }

    public void readOrUpdateObject(C1<T> updater) throws Exception {
        Exception[] exc = new Exception[1];
        lock.runInLock(() -> {
            T oldValue = object;
            try {
                updater.accept(object);
                beforeSave(object);
                if (Fu.notEqual(object, oldValue)) {
                    flushCurrentToDisc();
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
        Io.reWrite(filePath, w -> w.append(text));
    }

    protected void beforeSave(T updatedObject) {}
}
