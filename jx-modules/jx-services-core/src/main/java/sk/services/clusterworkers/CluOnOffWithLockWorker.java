package sk.services.clusterworkers;

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
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import sk.services.async.IAsync;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.model.CluMessage;
import sk.services.clusterworkers.model.CluState;
import sk.services.time.ITime;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.async.cancel.CancelToken;
import sk.utils.functional.C1;
import sk.utils.functional.F0;
import sk.utils.functional.Gett;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import static sk.services.clusterworkers.CluOnOffWithLockWorker.Messages.*;
import static sk.services.clusterworkers.CluOnOffWithLockWorker.Messages.Simple.*;
import static sk.services.clusterworkers.CluOnOffWithLockWorker.State.*;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
@Log4j2
public class CluOnOffWithLockWorker<C extends CluOnOffWithLockWorker.IConf>
        extends CluWorker<CluOnOffWithLockWorker.State, BaseMessage> {
    private CluScheduler<State, BaseMessage> onOffScheduler;
    private CluScheduler<State, BaseMessage> mainTaskScheduler;
    private CluScheduler<State, BaseMessage> lockScheduler;
    private CluScheduler<State, BaseMessage> lockRenewScheduler;
    private CluScheduler<State, BaseMessage> mainTaskHealthScheduler;
    private final CancelToken cancellationToken;

    private volatile long mainTaskStartedAt = -1;

    public CluOnOffWithLockWorker(String workerName) {
        super(workerName + "_OnOffLock", OFF);
        cancellationToken = new CancelToken();
    }

    public CluOnOffWithLockWorker(String workerName, IAsync async, ITime times) {
        super(workerName + "_OnOffLock", OFF, async, times);
        cancellationToken = new CancelToken();
    }

    @SuppressWarnings("unused")
    public synchronized void start(C conf) throws RuntimeException {
        if (onOffScheduler == null) {
            onOffScheduler =
                    addScheduler("OnOffSch", CluDelay.fixed(conf.getSchedulerCheckPeriod()), O.empty(),
                            () -> onOffCheckerScheduler(conf.getOnOffSwitchChecker()),
                            false);
            mainTaskScheduler =
                    addScheduler("MainTaskSch", conf.getMainTaskDelay(), O.of(Cc.s(LOCK_OBTAINED)),
                            () -> mainTaskScheduler(conf.getMainTaskRunner()),
                            true);
            lockScheduler =
                    addScheduler("LockSch", CluDelay.fixed(conf.getSchedulerCheckPeriod()), O.of(Cc.s(LOCK_IS_NOT_OBTAINED)),
                            () -> lockScheduler(conf.getLocker()),
                            false);
            lockRenewScheduler =
                    addScheduler("LockRenewSch", CluDelay.fixed(Math.max(conf.getSchedulerCheckPeriod() / 4, 1)),
                            O.of(Cc.s(LOCK_OBTAINED)),
                            () -> lockRenewScheduler(conf.getLockRenewer()),
                            false);
            mainTaskHealthScheduler =
                    addScheduler("MainHealthSch", CluDelay.fixed(conf.getSchedulerCheckPeriod()), O.of(Cc.s(LOCK_OBTAINED)),
                            () -> mainTaskHealthScheduler(conf.getMainTaskIsOldAfter()),
                            false);
        }

        super.start((m, s) -> messageProcessor(conf.getErrorConsumer(), m, s), conf.getErrorConsumer());
    }

    enum State implements CluState<State> {OFF, LOCK_IS_NOT_OBTAINED, LOCK_OBTAINED}

    @SuppressWarnings({"ConstantConditions", "StatementWithEmptyBody"})
    private void messageProcessor(C1<Throwable> errorConsumer, BaseMessage m, State curState) {
        if (m instanceof ErrorMessage) {
            errorConsumer.accept(((ErrorMessage) m).getError());
        } else if (m == OnMessage && curState == OFF) {
            toLockNotObtainedState();
        } else if (m == OnMessage && curState != OFF) {
            /*nothing*/
        } else if (m == OffMessage && curState != OFF) {
            toOffState();
        } else if (m == OffMessage && curState == OFF) {
            /*nothing*/
        } else if (m == LockSuccessMessage && curState == LOCK_IS_NOT_OBTAINED) {
            toLockObtainedState();
        } else if (m == LockRenewFailed && curState == LOCK_OBTAINED) {
            toLockNotObtainedState();
        } else if (m instanceof MainTaskThrowsException) {
            errorConsumer.accept(((MainTaskThrowsException) m).getError());
            if (curState == LOCK_OBTAINED) {
                toLockNotObtainedState();
            }
        } else if (m == MainTaskIdOld) {
            toLockNotObtainedState();
        } else {
            log.debug(() -> name + " - In state " + curState + " " + m.getClass() + " is unknown for CluOnOffWithLockWorker");
        }
    }

    private void toLockObtainedState() {
        setState(LOCK_OBTAINED);
        cancellationToken.setCancelled(false);
        mainTaskReset();
    }

    private void toOffState() {
        setState(OFF);
        cancellationToken.setCancelled(true);
        mainTaskReset();
    }

    private void toLockNotObtainedState() {
        setState(LOCK_IS_NOT_OBTAINED);
        cancellationToken.setCancelled(true);
        mainTaskReset();
    }

    private void mainTaskReset() {
        mainTaskStartedAt = -1;
        mainTaskScheduler.restart();
    }

    private O<BaseMessage> mainTaskScheduler(C1<CancelGetter> mainTaskRunner) {
        return withExceptionHandler(() -> {
            mainTaskStartedAt = times.now();
            try {
                mainTaskRunner.accept(cancellationToken);
            } catch (Exception e) {
                return O.of(new MainTaskThrowsException(e));
            } finally {
                mainTaskStartedAt = -1;
            }
            return O.empty();
        });
    }

    private O<BaseMessage> onOffCheckerScheduler(Gett<Boolean> onOffSwitchChecker) {
        return withExceptionHandler(() -> {
            Boolean onOrOff = onOffSwitchChecker.apply();
            return O.ofNullable(onOrOff ? OnMessage : OffMessage);
        });
    }

    @SuppressWarnings("CodeBlock2Expr")
    private O<BaseMessage> lockScheduler(Gett<Boolean> tryLock) {
        return withExceptionHandler(() -> {
            return (tryLock.apply()) ? O.of(LockSuccessMessage) : O.empty();
        });
    }

    private O<BaseMessage> lockRenewScheduler(Gett<Boolean> lockRenewer) {
        return withExceptionHandler(() -> (lockRenewer.apply()) ? O.empty() : O.of(LockRenewFailed));
    }


    private O<BaseMessage> mainTaskHealthScheduler(long mainTaskIsOldAfter) {
        return withExceptionHandler(() ->
                (mainTaskStartedAt != -1) && (times.now() - mainTaskStartedAt > mainTaskIsOldAfter)
                        ? O.of(MainTaskIdOld)
                        : O.empty());
    }

    private O<BaseMessage> withExceptionHandler(F0<O<BaseMessage>> provider) {
        try {
            return provider.apply();
        } catch (Exception e) {
            return O.of(new ErrorMessage(e));
        }
    }

    @Value
    static class Messages {
        interface BaseMessage extends CluMessage {}

        enum Simple implements BaseMessage {
            OnMessage, OffMessage, LockSuccessMessage, LockRenewFailed, MainTaskIdOld
        }

        @Value
        static class MainTaskThrowsException implements BaseMessage {
            Throwable error;
        }

        @Value
        static class ErrorMessage implements BaseMessage {
            Throwable error;
        }
    }


    public interface IConf {
        long getSchedulerCheckPeriod();

        long getMainTaskIsOldAfter();

        CluDelay getMainTaskDelay();

        C1<CancelGetter> getMainTaskRunner();

        Gett<Boolean> getOnOffSwitchChecker();

        Gett<Boolean> getLocker();

        Gett<Boolean> getLockRenewer();

        C1<Throwable> getErrorConsumer();
    }

    @AllArgsConstructor
    @Getter
    public static class Config implements IConf {
        long schedulerCheckPeriod;
        long mainTaskIsOldAfter;
        CluDelay mainTaskDelay;
        C1<CancelGetter> mainTaskRunner;

        Gett<Boolean> onOffSwitchChecker;
        Gett<Boolean> locker;
        Gett<Boolean> lockRenewer;
        C1<Throwable> errorConsumer;
    }
}
