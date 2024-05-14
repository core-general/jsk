package sk.test.land.core;

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

import lombok.Getter;
import sk.utils.async.locks.JLock;
import sk.utils.async.locks.JLockDecorator;
import sk.utils.functional.O;
import sk.utils.functional.RE;
import sk.utils.ifaces.Identifiable;

import static sk.test.land.core.JskLand.JskLandStatus.*;

public abstract class JskLand implements Identifiable<Class<? extends JskLand>> {
    @Getter private volatile JskLandStatus status = NOT_INITED;
    @Getter protected final JLock statusLock = new JLockDecorator();

    public final JskLandStatus start() throws Exception {
        return startOrShutDown(NOT_INITED, STARTING, STARTED, () -> doInit());
    }

    public final JskLandStatus stop() throws Exception {
        return startOrShutDown(STARTED, FINISHING, FINISHED, () -> doShutdown());
    }

    protected abstract void doInit() throws Exception;

    protected abstract void doShutdown() throws Exception;

    private JskLandStatus startOrShutDown(JskLandStatus initial, JskLandStatus inProgress, JskLandStatus done, RE toDo)
            throws Exception {
        O<Exception> isError = statusLock.getInLock(() -> switch (status) {
            case JskLandStatus w when w == initial -> {
                try {
                    status = inProgress;
                    toDo.run();
                    status = done;
                    yield O.empty();
                } catch (Exception e) {
                    status = FAILED;
                    yield O.of(e);
                }
            }
            default -> O.empty();
        });
        if (isError.isPresent()) {
            throw isError.get();
        }
        return status;
    }

    public enum JskLandStatus {
        NOT_INITED,
        STARTING,
        STARTED,
        FINISHING,
        FINISHED,
        FAILED
    }
}
