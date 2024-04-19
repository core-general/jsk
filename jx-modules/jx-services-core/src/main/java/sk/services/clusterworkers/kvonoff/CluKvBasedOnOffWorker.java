package sk.services.clusterworkers.kvonoff;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sk.exceptions.NotImplementedException;
import sk.services.async.IAsync;
import sk.services.clusterworkers.CluOnOffWorker;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.model.CluOnOffKvKey;
import sk.services.kv.IKvLimitedStore;
import sk.services.time.ITime;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.C1;
import sk.utils.functional.Gett;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
@Slf4j
public class CluKvBasedOnOffWorker<CONFIG extends CluKvBasedOnOffWorker.IConf> extends CluOnOffWorker<CONFIG> {
    protected @Inject IKvLimitedStore kv;

    public CluKvBasedOnOffWorker(String workerName) {
        super(workerName);
    }

    public CluKvBasedOnOffWorker(String workerName, IAsync async, ITime times, IKvLimitedStore kv) {
        super(workerName, async, times);
        this.kv = kv;
    }

    @Override
    public synchronized void start(CONFIG c) throws RuntimeException {
        CluOnOffKvKey onOffKey = new CluOnOffKvKey(name);
        IConf conf = new IConf() {
            @Override
            public Gett<Boolean> getOnOffSwitchChecker() {
                return () -> kv.getAsBool(onOffKey);
            }

            @Override
            public CluDelay getMainTaskDelay() {
                return c.getMainTaskDelay();
            }

            @Override
            public C1<CancelGetter> getMainTaskRunner() {
                return c.getMainTaskRunner();
            }

            @Override
            public long getOnOffCheckPeriod() {
                return c.getOnOffCheckPeriod();
            }

            @Override
            public C1<Throwable> getErrorConsumer() {
                return c.getErrorConsumer();
            }
        };
        super.start((CONFIG) conf);
    }

    public interface IConf extends CluOnOffWorker.IConf {
        @Override
        default Gett<Boolean> getOnOffSwitchChecker() {
            throw new NotImplementedException();//must be overriden
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Conf implements IConf {
        long onOffCheckPeriod;
        CluDelay mainTaskDelay;
        C1<CancelGetter> mainTaskRunner;

        C1<Throwable> errorConsumer;
    }

}
