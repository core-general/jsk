package jsk.outer.telegram.mtc.beans.telegram.poling;

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

import sk.services.kv.IKvLimitedStore;
import sk.services.kv.keys.KvSimpleKeyWithName;

import javax.inject.Inject;

import static sk.utils.functional.O.of;

public abstract class TelKvPolingImpl implements TelPolingConf {
    @Inject IKvLimitedStore kv;

    final private KvSimpleKeyWithName pollingKey;

    public TelKvPolingImpl(String kvKeySuffix) {
        pollingKey = new KvSimpleKeyWithName("TelPolingConf_" + kvKeySuffix, "0");
    }

    @Override
    public boolean usePolling() {
        return true;
    }

    @Override
    public int getLastTelegramMessageId() {
        return kv.getAsInt(pollingKey);
    }

    @Override
    public void setLastTelegramMessageId(int newId) {
        kv.updateInt(pollingKey, old -> of(newId)).left();
    }

    @Override
    public int getTelegramPollingPeriodSec() {
        return 2;
    }
}
