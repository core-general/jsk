package sk.services.clusterworkers.kvonofflocker;

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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import sk.exceptions.NotImplementedException;
import sk.services.async.IAsync;
import sk.services.clusterworkers.CluOnOffWithLockWorker;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.model.CluOnOffKvKey;
import sk.services.kv.IKvLimitedStore;
import sk.services.kv.keys.KvLockOrRenewKey;
import sk.services.nodeinfo.INodeInfo;
import sk.services.time.ITime;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.C1;
import sk.utils.functional.Gett;
import sk.utils.functional.O;

import javax.inject.Inject;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
@Log4j2
public class CluKvBasedOnOffWithLockWorker<CONFIG extends CluKvBasedOnOffWithLockWorker.IConf>
        extends CluOnOffWithLockWorker<CONFIG> {
    protected @Inject IKvLimitedStore kv;
    protected @Inject INodeInfo nodeInfo;

    public CluKvBasedOnOffWithLockWorker(String workerName) {
        super(workerName);
    }

    public CluKvBasedOnOffWithLockWorker(String workerName, IAsync async, ITime times, IKvLimitedStore kv, INodeInfo nodeInfo) {
        super(workerName, async, times);
        this.kv = kv;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public synchronized void start(CONFIG c) throws RuntimeException {
        CluOnOffKvKey onOffKey = new CluOnOffKvKey(name);
        KvLockOrRenewKey lockKey = new KvLockOrRenewKey(name);
        IConf conf = new IConf() {
            @Override
            public Gett<Boolean> getOnOffSwitchChecker() {
                return () -> {
                    Boolean apply = null;
                    try {
                        apply = c.getOnOffSwitchChecker() != null
                                ? c.getOnOffSwitchChecker().apply()
                                : null;
                    } catch (Exception e) {
                    }
                    return apply == null ? kv.getAsBool(onOffKey) : apply;
                };
            }

            @Override
            public Gett<Boolean> getLocker() {
                return () -> kv.tryLockOrRenew(lockKey, nodeInfo.getNodeId(), O.of(getSchedulerCheckPeriod()));
            }

            @Override
            public Gett<Boolean> getLockRenewer() {
                return () -> kv.tryLockOrRenew(lockKey, nodeInfo.getNodeId(), O.empty());
            }

            @Override
            public long getSchedulerCheckPeriod() {
                return c.getSchedulerCheckPeriod();
            }

            @Override
            public long getMainTaskIsOldAfter() {
                return c.getMainTaskIsOldAfter();
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
            public C1<Throwable> getErrorConsumer() {
                return c.getErrorConsumer();
            }
        };
        super.start((CONFIG) conf);
    }

    public interface IConf extends CluOnOffWithLockWorker.IConf {
        @Override
        default Gett<Boolean> getOnOffSwitchChecker() {
            return () -> null;//should be overriden
        }

        @Override
        default Gett<Boolean> getLocker() {
            throw new NotImplementedException();//must be overriden
        }

        @Override
        default Gett<Boolean> getLockRenewer() {
            throw new NotImplementedException();//must be overriden
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Conf implements IConf {
        long schedulerCheckPeriod;
        long mainTaskIsOldAfter;
        CluDelay mainTaskDelay;
        C1<CancelGetter> mainTaskRunner;

        C1<Throwable> errorConsumer;
    }
}
