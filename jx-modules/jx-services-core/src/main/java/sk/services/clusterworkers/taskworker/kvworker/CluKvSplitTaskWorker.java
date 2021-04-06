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
import sk.services.async.IAsync;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.model.CluOnOffKvKey;
import sk.services.clusterworkers.taskworker.CluSplitTaskWorker;
import sk.services.clusterworkers.taskworker.model.CluAsyncTaskExecutor;
import sk.services.clusterworkers.taskworker.model.CluSplitTask;
import sk.services.clusterworkers.taskworker.model.CluWorkMetaInfo;
import sk.services.ids.IIds;
import sk.services.json.IJson;
import sk.services.kv.IKvLimitedStore;
import sk.services.kv.KvAllValues;
import sk.services.kv.keys.KvKey2Categories;
import sk.services.kv.keys.KvKeyWithDefault;
import sk.services.time.ITime;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.*;
import sk.utils.ifaces.Identifiable;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;
import sk.utils.tuples.X1;
import sk.utils.tuples.X2;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static sk.utils.functional.OneOf.right;

@SuppressWarnings("Convert2MethodRef")
@Log4j2
public class CluKvSplitTaskWorker<CUSTOM_META, TASK_INPUT extends Identifiable<String>, RESULT,
        CONFIG extends CluKvSplitTaskWorker.IConf<CUSTOM_META, TASK_INPUT, RESULT>>
        extends CluSplitTaskWorker<CUSTOM_META, RESULT, CONFIG> {
    @Getter final KvKeyWithDefault kvKey4Task;

    @Inject IIds ids;
    @Inject IJson json;
    @Inject IKvLimitedStore kv;

    volatile F2<O<CUSTOM_META>, List<TASK_INPUT>, Boolean> restarter;
    volatile F1<O<byte[]>, CluWorkFullInfo<TASK_INPUT, RESULT>> decompressor;

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
    public synchronized void start(CONFIG c) throws RuntimeException {
        final CONFIG config = wrapConfig(c);

        restarter = (meta, work) -> kv.updateObjectAndRaw(kvKey4Task, getMetaType(config.getMetaClass()), (metaAndWork) -> {
            CluWorkHelper<CUSTOM_META, TASK_INPUT, RESULT> helper =
                    new CluWorkHelper<>(null, metaAndWork, new ConverterImpl<>(
                            a -> decompressor.apply(a),
                            b -> O.of(compressFullWork(config, b))),
                            times.nowZ(), config.getMsForTaskToExpire()
                    );

            helper.initWork(ids.shortIdS(), meta, work);

            return O.of(helper.serializeBack());
        }).collect($ -> true, e -> {
            config.getErrorConsumer().accept(e);
            return false;
        });

        decompressor = (obytes) -> decompressFullWork(config, obytes);

        super.start(config);
    }

    public boolean restart(O<CUSTOM_META> meta, List<TASK_INPUT> work) {
        return restarter.apply(meta, work);
    }

    public void updateAdditionalInfo(O<CUSTOM_META> meta, Class<CUSTOM_META> cls) {
        kv.updateObject(kvKey4Task, getMetaType(cls),
                (m) -> {
                    m.setAdditionalInfo(meta);
                    return O.of(m);
                });
    }

    public CluWorkMetaInfo<CUSTOM_META> getMeta(Class<CUSTOM_META> cls) {
        return kv.getAsObject(kvKey4Task, getMetaType(cls));
    }

    public CluWorkMetaInfo<?> getMeta() {
        return kv.getAsObject(kvKey4Task, getMetaType(Object.class));
    }

    public void stopByFail(String failReason) {
        kv.updateObject(kvKey4Task, CluWorkMetaInfo.class,
                (m) -> {
                    m.setStatus(CluWorkMetaInfo.Status.FAILED);
                    m.setFailMessage(O.of(failReason));
                    return O.of(m);
                });
    }

    public CompletableFuture<List<X2<TASK_INPUT, OneOf<RESULT, String>>>> getFullWorkInfoAfterFinish() {
        return getFullWorkInfoAfterFinish(1000);
    }

    public CompletableFuture<List<X2<TASK_INPUT, OneOf<RESULT, String>>>> getFullWorkInfoAfterFinish(long sleepTime) {
        return async.supplyBuf(() -> {
            while (true) {
                final CluWorkMetaInfo<?> meta = getMeta();
                if (!meta.isWorkActive()) {
                    break;
                }
                async.sleep(sleepTime);
            }

            final CluWorkFullInfo<TASK_INPUT, RESULT> fullWork =
                    decompressor.apply(kv.getAsObjectWithRaw(kvKey4Task, CluWorkMetaInfo.class).getRawValue());

            List<X2<TASK_INPUT, OneOf<RESULT, String>>> toRet = Cc.l();

            final Function<CluKvSplitWorkPartInfo<TASK_INPUT, RESULT>, X2<TASK_INPUT, OneOf<RESULT, String>>> converteR =
                    $ -> X.x($.getWorkDescription(), $.getLastResult().map($$ -> OneOf.<RESULT, String>left($$))
                            .orElseGet(() -> right($.getLastError().get())));

            fullWork.getWorkFail().stream().map(converteR).forEach(toRet::add);
            fullWork.getWorkSuccess().stream().map(converteR).forEach(toRet::add);

            return toRet;
        });
    }

    private CluSplitTask<CUSTOM_META, RESULT> createTaskForCurrentRun(CONFIG config) {
        String currentTaskId = ids.shortIdS();

        CluSplitTask<CUSTOM_META, RESULT> dontStartSplitTask;
        {
            CluWorkMetaInfo<CUSTOM_META> oldMeta = kv.getAsObject(kvKey4Task, getMetaType(config.getMetaClass()));
            dontStartSplitTask = new CluSplitTask<>(oldMeta);
            if (!oldMeta.isWorkActive()) {
                return dontStartSplitTask;
            }
        }

        X1<List<CluWorkChunk<RESULT>>> todo = new X1<>();
        O<KvAllValues<CluWorkMetaInfo<CUSTOM_META>>> lockSuccess =
                kv.updateObjectAndRaw(kvKey4Task, getMetaType(config.getMetaClass()),
                        (metaAndWork) -> prepareTasksAndLockKVStateIfPossible(config, currentTaskId, todo, metaAndWork))
                        .collect($ -> $, e -> O.empty());

        if (lockSuccess.isEmpty()) {
            return dontStartSplitTask;
        }
        return new CluSplitTask<>(lockSuccess.get().getValue(), O.of(todo.get()), O.of(config.getTaskExecutor()),
                O.of(finishedWork -> finisher(config, finishedWork, currentTaskId)));
    }

    private O<KvAllValues<CluWorkMetaInfo<CUSTOM_META>>> prepareTasksAndLockKVStateIfPossible(CONFIG config, String currentTaskId,
            X1<List<CluWorkChunk<RESULT>>> todo, KvAllValues<CluWorkMetaInfo<CUSTOM_META>> metaAndWork) {
        CluWorkHelper<CUSTOM_META, TASK_INPUT, RESULT> helper =
                new CluWorkHelper<>(currentTaskId, metaAndWork, new ConverterImpl<>(
                        a -> decompressor.apply(a),
                        b -> O.of(compressFullWork(config, b))),
                        times.nowZ(), config.getMsForTaskToExpire()
                );

        if (helper.noMoreActiveWork()) {
            return O.empty();
        }

        List<CluWorkChunk<RESULT>> work = helper.getActualWork(config.getNumberOfTasksInSplit()).stream()
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

    private void finisher(CONFIG config, O<List<CluWorkChunkResult<RESULT>>> finishedWork, String currentTaskId) {
        if (finishedWork.isEmpty()) {
            return; //do nothing if no work
        }

        kv.updateObjectAndRaw(kvKey4Task, getMetaType(config.getMetaClass()), (metaAndWork) -> {
            CluWorkHelper<CUSTOM_META, TASK_INPUT, RESULT> helper = new CluWorkHelper<>(currentTaskId, metaAndWork,
                    new ConverterImpl<>(
                            a -> decompressor.apply(a),
                            b -> O.of(compressFullWork(config, b))
                    ),
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
    private CluWorkFullInfo<TASK_INPUT, RESULT> decompressFullWork(CONFIG config, O<byte[]> work) {
        return work
                .map($ -> config.getRawValueConverter().convertThere($))
                .map($ -> (CluWorkFullInfo<TASK_INPUT, RESULT>) json
                        .from($, (TypeWrap<?>) TypeWrap
                                .getCustom(CluWorkFullInfo.class, config.getWorkInfoCls(), config.getResultClass())))
                .orElseGet(() -> new CluWorkFullInfo<TASK_INPUT, RESULT>());
    }

    private byte[] compressFullWork(CONFIG config, CluWorkFullInfo<TASK_INPUT, RESULT> fullWork) {
        return config.getRawValueConverter().convertBack(json.to(fullWork));
    }

    @NotNull
    private TypeWrap<CluWorkMetaInfo<CUSTOM_META>> getMetaType(Class<?> metaClass) {
        return (TypeWrap<CluWorkMetaInfo<CUSTOM_META>>)
                TypeWrap.getHolder(CluWorkMetaInfo.class, metaClass);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private CONFIG wrapConfig(CONFIG config) {
        CluOnOffKvKey onOffKey = new CluOnOffKvKey(name);
        IConf<CUSTOM_META, TASK_INPUT, RESULT> iConf = new IConf<CUSTOM_META, TASK_INPUT, RESULT>() {
            @Override
            public Gett<CluSplitTask<CUSTOM_META, RESULT>> getSplitTask() {
                return () -> {
                    CluSplitTask<CUSTOM_META, RESULT> apply = null;
                    try {
                        apply = config.getSplitTask() != null
                                ? config.getSplitTask().apply()
                                : null;
                    } catch (Exception e) {
                    }
                    return apply == null ? createTaskForCurrentRun(config) : apply;
                };
            }

            @Override
            public void preRun(CluSplitTask<CUSTOM_META, RESULT> task) {
                config.preRun(task);
            }

            @Override
            public Gett<Boolean> getOnOffSwitchChecker() {
                return () -> {
                    Boolean apply = null;
                    try {
                        apply = config.getOnOffSwitchChecker() != null
                                ? config.getOnOffSwitchChecker().apply()
                                : null;
                    } catch (Exception e) {
                    }
                    return apply == null ? kv.getAsBool(onOffKey) : apply;
                };
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
            public F3<CUSTOM_META, TASK_INPUT, CancelGetter, CluWorkChunkResult<RESULT>> getMainPayloadProcessor() {
                return config.getMainPayloadProcessor();
            }

            @Override
            public Converter<byte[], String> getRawValueConverter() {
                return config.getRawValueConverter();
            }

            @Override
            public CluAsyncTaskExecutor<RESULT> getTaskExecutor() {
                return config.getTaskExecutor();
            }

            @Override
            public Class<CUSTOM_META> getMetaClass() {
                return config.getMetaClass();
            }

            @Override
            public Class<TASK_INPUT> getWorkInfoCls() {
                return config.getWorkInfoCls();
            }

            @Override
            public Class<RESULT> getResultClass() {
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
        return (CONFIG) iConf;
    }

    public interface IConf<CUSTOM_META, TASK_INPUT, RESULT> extends CluSplitTaskWorker.IConf<CUSTOM_META, RESULT> {
        @Override
        default Gett<CluSplitTask<CUSTOM_META, RESULT>> getSplitTask() {
            return null;
        }

        @Override
        default Gett<Boolean> getOnOffSwitchChecker() {
            return null;
        }

        int getNumberOfTasksInSplit();

        F3<CUSTOM_META, TASK_INPUT, CancelGetter, CluWorkChunkResult<RESULT>> getMainPayloadProcessor();

        Converter<byte[], String> getRawValueConverter();

        CluAsyncTaskExecutor<RESULT> getTaskExecutor();

        Class<CUSTOM_META> getMetaClass();

        Class<TASK_INPUT> getWorkInfoCls();

        Class<RESULT> getResultClass();

        long getMsForTaskToExpire();

        int getMaxTaskRetries();

        CluWorkBackoffStrategy getBackoff();
    }

    @Getter
    @AllArgsConstructor
    public static class Config<CUSTOM_META, TASK_INPUT, RESULT> implements IConf<CUSTOM_META, TASK_INPUT, RESULT> {
        CluDelay mainTaskDelay;
        long onOffCheckPeriod;
        C1<Throwable> errorConsumer;

        F3<CUSTOM_META, TASK_INPUT, CancelGetter, CluWorkChunkResult<RESULT>> mainPayloadProcessor;
        int numberOfTasksInSplit;
        Converter<byte[], String> rawValueConverter;
        CluAsyncTaskExecutor<RESULT> taskExecutor;
        Class<CUSTOM_META> metaClass;
        int maxTaskRetries;
        Class<TASK_INPUT> workInfoCls;
        long msForTaskToExpire;
        Class<RESULT> resultClass;
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
