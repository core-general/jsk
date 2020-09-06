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

import static sk.services.clusterworkers.CluOnOffWorker.State.OFF;
import static sk.services.clusterworkers.CluOnOffWorker.State.ON;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class CluOnOffWorker<C extends CluOnOffWorker.IConf> extends CluWorker<CluOnOffWorker.State, CluOnOffWorker.Msg> {
    private CluScheduler<State, Msg> onOffScheduler;
    private CluScheduler<State, Msg> mainTaskScheduler;
    private final CancelToken cancellationToken;

    @SuppressWarnings("WeakerAccess")
    public CluOnOffWorker(String workerName) {
        super(workerName + "_OnOff", OFF);
        cancellationToken = new CancelToken();
    }

    @SuppressWarnings("WeakerAccess")
    public CluOnOffWorker(String workerName, IAsync async, ITime times) {
        super(workerName + "_OnOff", OFF, async, times);
        cancellationToken = new CancelToken();
    }

    enum State implements CluState<State> {OFF, ON}

    @SuppressWarnings("unused")
    public synchronized void start(C config) throws RuntimeException {
        if (onOffScheduler == null) {
            onOffScheduler = addScheduler("OnOffSch",
                    CluDelay.fixed(config.getOnOffCheckPeriod()), O.empty(),
                    () -> onOffCheckerScheduler(config.getOnOffSwitchChecker()),
                    false);
            mainTaskScheduler =
                    addScheduler("MainTaskSch", config.getMainTaskDelay(), O.of(Cc.s(ON)),
                            () -> mainTaskScheduler(config.getMainTaskRunner()), true);
        }
        super.start((m, s) -> messageProcessor(config.getErrorConsumer(), m, s), config.getErrorConsumer());
    }

    private O<Msg> mainTaskScheduler(C1<CancelGetter> mainTaskRunner) {
        return withExceptionHandler(() -> {
            mainTaskRunner.accept(cancellationToken);
            return O.empty();
        });
    }

    private O<Msg> onOffCheckerScheduler(F0<Boolean> onOffSwitchChecker) {
        return withExceptionHandler(() -> {
            Boolean onOrOff = onOffSwitchChecker.apply();
            return O.ofNullable(onOrOff ? Msg.Simple.TO_ON : Msg.Simple.TO_OFF);
        });
    }

    private void messageProcessor(C1<Throwable> errorConsumer, Msg msg, State curState) {
        if (msg instanceof Msg.Simple && ((Msg.Simple) msg).toState != curState) {
            setState(((Msg.Simple) msg).toState);
            if (((Msg.Simple) msg).toState == ON) {
                cancellationToken.setCancelled(false);
                mainTaskScheduler.restart();
            } else {
                cancellationToken.setCancelled(true);
            }
        } else if (msg instanceof Msg.Error && ((Msg.Error) msg).error != null) {
            errorConsumer.accept(((Msg.Error) msg).error);
        }
    }

    private O<Msg> withExceptionHandler(F0<O<Msg>> provider) {
        try {
            return provider.apply();
        } catch (Exception e) {
            return O.of(new Msg.Error(e));
        }
    }

    static interface Msg extends CluMessage {
        @AllArgsConstructor
        enum Simple implements Msg {
            TO_ON(ON), TO_OFF(OFF);
            @Getter State toState;
        }

        @Getter
        @AllArgsConstructor
        static class Error implements Msg {
            Throwable error;
        }
    }

    public interface IConf {
        CluDelay getMainTaskDelay();

        C1<CancelGetter> getMainTaskRunner();

        long getOnOffCheckPeriod();

        Gett<Boolean> getOnOffSwitchChecker();

        C1<Throwable> getErrorConsumer();
    }

    @Getter
    @AllArgsConstructor
    public static class Config implements IConf {
        CluDelay mainTaskDelay;
        C1<CancelGetter> mainTaskRunner;

        long onOffCheckPeriod;
        Gett<Boolean> onOffSwitchChecker;

        C1<Throwable> errorConsumer;
    }
}
