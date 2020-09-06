package sk.services.clusterworkers.taskworker.kvworker;

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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KINDither express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import sk.exceptions.NotImplementedException;
import sk.services.async.IAsync;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.model.CluOnOffKvKey;
import sk.services.clusterworkers.taskworker.CluSplitTaskWorker;
import sk.services.clusterworkers.taskworker.kvworker.model.CluWorkChunk;
import sk.services.clusterworkers.taskworker.kvworker.model.CluWorkChunkResult;
import sk.services.clusterworkers.taskworker.kvworker.model.CluWorkFullInfo;
import sk.services.clusterworkers.taskworker.kvworker.model.CluWorkHelper;
import sk.services.clusterworkers.taskworker.kvworker.model.backoff.CluWorkBackoffStrategy;
import sk.services.clusterworkers.taskworker.model.CluAsyncTaskExecutor;
import sk.services.clusterworkers.taskworker.model.CluSplitTask;
import sk.services.clusterworkers.taskworker.model.CluWorkMetaInfo;
import sk.services.ids.IIds;
import sk.services.json.IJson;
import sk.services.kv.IKvLimitedStore;
import sk.services.kv.KvAllValues;
import sk.services.kv.keys.KvKey;
import sk.services.kv.keys.KvKey2Categories;
import sk.services.time.ITime;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.*;
import sk.utils.ifaces.Identifiable;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.tuples.X1;

import javax.inject.Inject;
import java.util.List;

@SuppressWarnings("Convert2MethodRef")
@Log4j2
public class CluKvSplitTaskWorker<M, T extends Identifiable<String>, R, C extends CluKvSplitTaskWorker.IConf<M, T, R>>
        extends CluSplitTaskWorker<M, R, C> {
    @Getter final KvKey kvKey4Task;

    @Inject IIds ids;
    @Inject IJson json;
    @Inject IKvLimitedStore kv;

    F2<O<M>, List<T>, Boolean> restarter;

    public CluKvSplitTaskWorker(String workerName) {
        super(workerName);
        kvKey4Task = new SplitKvKey(workerName);
    }

    public CluKvSplitTaskWorker(String workerName, IAsync async, ITime times, IIds ids, IJson json, IKvLimitedStore kv) {
        super(workerName, async, times);
        kvKey4Task = new SplitKvKey(workerName);
        this.ids = ids;
        this.json = json;
        this.kv = kv;
    }

    @Override
    public synchronized void start(C c) throws RuntimeException {
        final C config = wrapConfig(c);

        restarter = (meta, work) -> kv.updateObjectAndRaw(kvKey4Task, getMetaType(config.getMetaClass()),
                (metaAndWork) -> {
                    CluWorkHelper<M, T, R> helper = new CluWorkHelper<>(null, metaAndWork, new ConverterImpl<>(
                            a -> decompressFullWork(config, a),
                            b -> O.of(compressFullWork(config, b))),
                            times.nowZ(), config.getMsForTaskToExpire()
                    );

                    helper.initWork(ids.shortIdS(), meta, work);

                    return O.of(helper.serializeBack());
                }).collect($ -> true, e -> false);

        super.start(config);
    }

    public boolean restart(O<M> meta, List<T> work) {
        return restarter.apply(meta, work);
    }

    public void updateAdditionalInfo(O<M> meta, Class<M> cls) {
        kv.updateObject(kvKey4Task, getMetaType(cls),
                (m) -> {
                    m.setAdditionalInfo(meta);
                    return O.of(m);
                });
    }

    public CluWorkMetaInfo<M> getMeta(Class<M> cls) {
        return kv.getAsObject(kvKey4Task, getMetaType(cls));
    }

    public void stopByFail(String failReason) {
        kv.updateObject(kvKey4Task, CluWorkMetaInfo.class,
                (m) -> {
                    m.setStatus(CluWorkMetaInfo.Status.FAILED);
                    m.setFailMessage(O.of(failReason));
                    return O.of(m);
                });
    }

    private CluSplitTask<M, R> createTaskForCurrentRun(C config) {
        String currentTaskId = ids.shortIdS();

        CluSplitTask<M, R> dontStartSplitTask;
        {
            CluWorkMetaInfo<M> oldMeta = kv.getAsObject(kvKey4Task, getMetaType(config.getMetaClass()));
            dontStartSplitTask = new CluSplitTask<>(oldMeta);
            if (!oldMeta.isWorkActive()) {
                return dontStartSplitTask;
            }
        }

        X1<List<CluWorkChunk<R>>> todo = new X1<>();
        O<KvAllValues<CluWorkMetaInfo<M>>> lockSuccess =
                kv.updateObjectAndRaw(kvKey4Task, getMetaType(config.getMetaClass()),
                        (metaAndWork) -> prepareTasksAndLockKVStateIfPossible(config, currentTaskId, todo, metaAndWork))
                        .collect($ -> $, e -> O.empty());

        if (lockSuccess.isEmpty()) {
            return dontStartSplitTask;
        }
        return new CluSplitTask<>(lockSuccess.get().getValue(), O.of(todo.get()), O.of(config.getTaskExecutor()),
                O.of(finishedWork -> finisher(config, finishedWork, currentTaskId)));
    }

    private O<KvAllValues<CluWorkMetaInfo<M>>> prepareTasksAndLockKVStateIfPossible(C config, String currentTaskId,
            X1<List<CluWorkChunk<R>>> todo, KvAllValues<CluWorkMetaInfo<M>> metaAndWork) {
        CluWorkHelper<M, T, R> helper = new CluWorkHelper<>(currentTaskId, metaAndWork, new ConverterImpl<>(
                a -> decompressFullWork(config, a),
                b -> O.of(compressFullWork(config, b))),
                times.nowZ(), config.getMsForTaskToExpire()
        );

        if (helper.noMoreActiveWork()) {
            return O.empty();
        }

        List<CluWorkChunk<R>> work = helper.getActualWork(config.getNumberOfTasksInSplit()).stream()
                .filter($ -> $ != null)
                .map($ -> new CluWorkChunk<>($.getWorkDescription().getId(), $.getTryCount(),
                        (cancel) -> config.getMainPayloadProcessor()
                                .apply(metaAndWork.getValue().getAdditionalInfo().orElse(null), $.getWorkDescription(), cancel)))
                .collect(Cc.toL());

        if (work.size() == 0) {
            return O.empty();
        }

        todo.set(work);

        return O.of(helper.serializeBack());
    }

    private void finisher(C config, O<List<CluWorkChunkResult<R>>> finishedWork, String currentTaskId) {
        if (finishedWork.isEmpty()) {
            return; //do nothing if no work
        }

        kv.updateObjectAndRaw(kvKey4Task, getMetaType(config.getMetaClass()), (metaAndWork) -> {
            CluWorkHelper<M, T, R> helper = new CluWorkHelper<>(currentTaskId, metaAndWork, new ConverterImpl<>(
                    a -> decompressFullWork(config, a),
                    b -> O.of(compressFullWork(config, b))),
                    times.nowZ(), config.getMsForTaskToExpire()
            );

            if (!helper.isWorkActive()) {
                return O.empty();
            }

            helper.finishProcessing(finishedWork.get(), config.getMaxTaskRetries(), config.getBackoff());

            return O.of(helper.serializeBack());
        });
    }

    @SuppressWarnings("unchecked")
    private CluWorkFullInfo<T, R> decompressFullWork(C config, O<byte[]> work) {
        return work
                .map($ -> config.getRawValueConverter().convertThere($))
                .map($ -> (CluWorkFullInfo<T, R>) json
                        .from($, (TypeWrap<?>) TypeWrap
                                .getCustom(CluWorkFullInfo.class, config.getWorkInfoCls(), config.getResultClass())))
                .orElseGet(() -> new CluWorkFullInfo<T, R>());
    }

    private byte[] compressFullWork(C config, CluWorkFullInfo<T, R> fullWork) {
        return config.getRawValueConverter().convertBack(json.to(fullWork));
    }

    @NotNull
    private TypeWrap<CluWorkMetaInfo<M>> getMetaType(Class<M> metaClass) {
        return (TypeWrap<CluWorkMetaInfo<M>>)
                TypeWrap.getHolder(CluWorkMetaInfo.class, metaClass);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private C wrapConfig(C config) {
        CluOnOffKvKey onOffKey = new CluOnOffKvKey(name);
        IConf<M, T, R> iConf = new IConf<M, T, R>() {
            @Override
            public Gett<CluSplitTask<M, R>> getSplitTask() {
                return () -> createTaskForCurrentRun(config);
            }

            @Override
            public void preRun(CluSplitTask<M, R> task) {
                config.preRun(task);
            }

            @Override
            public Gett<Boolean> getOnOffSwitchChecker() {
                return () -> kv.getAsBool(onOffKey);
            }

            @Override
            public CluDelay getMainTaskDelay() {
                return config.getMainTaskDelay();
            }

            @Override
            public long getOnOffCheckPeriod() {
                return config.getOnOffCheckPeriod();
            }

            @Override
            public C1<Throwable> getErrorConsumer() {
                return config.getErrorConsumer();
            }

            @Override
            public int getNumberOfTasksInSplit() {
                return config.getNumberOfTasksInSplit();
            }

            @Override
            public F3<M, T, CancelGetter, CluWorkChunkResult<R>> getMainPayloadProcessor() {
                return config.getMainPayloadProcessor();
            }

            @Override
            public Converter<byte[], String> getRawValueConverter() {
                return config.getRawValueConverter();
            }

            @Override
            public CluAsyncTaskExecutor<R> getTaskExecutor() {
                return config.getTaskExecutor();
            }

            @Override
            public Class<M> getMetaClass() {
                return config.getMetaClass();
            }

            @Override
            public Class<T> getWorkInfoCls() {
                return config.getWorkInfoCls();
            }

            @Override
            public Class<R> getResultClass() {
                return config.getResultClass();
            }

            @Override
            public long getMsForTaskToExpire() {
                return config.getMsForTaskToExpire();
            }

            @Override
            public int getMaxTaskRetries() {
                return config.getMaxTaskRetries();
            }

            @Override
            public CluWorkBackoffStrategy getBackoff() {
                return config.getBackoff();
            }
        };
        return (C) iConf;
    }

    public interface IConf<M, T, R> extends CluSplitTaskWorker.IConf<M, R> {
        @Override
        default Gett<CluSplitTask<M, R>> getSplitTask() {
            throw new NotImplementedException();//must be overriden
        }

        @Override
        default Gett<Boolean> getOnOffSwitchChecker() {
            throw new NotImplementedException();//must be overriden
        }

        int getNumberOfTasksInSplit();

        F3<M, T, CancelGetter, CluWorkChunkResult<R>> getMainPayloadProcessor();

        Converter<byte[], String> getRawValueConverter();

        CluAsyncTaskExecutor<R> getTaskExecutor();

        Class<M> getMetaClass();

        Class<T> getWorkInfoCls();

        Class<R> getResultClass();

        long getMsForTaskToExpire();

        int getMaxTaskRetries();

        CluWorkBackoffStrategy getBackoff();
    }

    @Getter
    @AllArgsConstructor
    public static class Config<M, T, R> implements IConf<M, T, R> {
        CluDelay mainTaskDelay;
        long onOffCheckPeriod;
        C1<Throwable> errorConsumer;

        F3<M, T, CancelGetter, CluWorkChunkResult<R>> mainPayloadProcessor;
        int numberOfTasksInSplit;
        Converter<byte[], String> rawValueConverter;
        CluAsyncTaskExecutor<R> taskExecutor;
        Class<M> metaClass;
        int maxTaskRetries;
        Class<T> workInfoCls;
        long msForTaskToExpire;
        Class<R> resultClass;
        CluWorkBackoffStrategy backoff;
    }

    private static class SplitKvKey implements KvKey2Categories {
        private final String workerName;

        public SplitKvKey(String workerName) {this.workerName = workerName;}

        @Override
        public String getKey1() {
            return "SPLIT_TASK";
        }

        @Override
        public O<String> getKey2() {
            return O.of(workerName + "_KvSplitTaskWorker");
        }

        @Override
        public String getDefaultValue() {
            return "{}";
        }

    }
}
