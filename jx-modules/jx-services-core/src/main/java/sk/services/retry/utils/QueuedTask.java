package sk.services.retry.utils;

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

import lombok.Getter;
import sk.utils.functional.OneOf;

@SuppressWarnings("unused")
public class QueuedTask<ID, T, A extends IdCallable<ID, T>> {
    public static final QueuedTask STOP = new QueuedTask();

    @Getter private A task;
    @Getter private int tryCount;
    @Getter private boolean finished = false;
    @Getter private volatile OneOf<T, Exception> result;

    public boolean isOk() {
        return isFinished() && result.isLeft();
    }

    public boolean isUnknownException() {
        return result != null && !finished && result.isRight();
    }

    public boolean isRepeatException() {
        return result != null && finished && result.isRight();
    }

    public QueuedTask(A task) {
        this.task = task;
        tryCount = 0;
        result = null;
    }

    private QueuedTask(A task, int tryCount, boolean finished, OneOf<T, Exception> result) {
        this.task = task;
        this.tryCount = tryCount;
        this.finished = finished;
        this.result = result;
    }

    /**
     * stop sign
     */
    private QueuedTask() {
        this.task = null;
        tryCount = -1;
        result = null;
    }

    public void incTryCount() {
        tryCount++;
    }

    public void ok(T call) {
        finished = true;
        result = OneOf.left(call);
    }

    public void unknownExcept(Exception e) {
        result = OneOf.right(e);
    }

    public void repeatExcept(Exception e) {
        finished = true;
        result = OneOf.right(e);
    }

    public static <ID, T, A extends IdCallable<ID, T>> QueuedTask<ID, T, A> cloneQueTask(A task, int tryCount, boolean finished,
            OneOf<T, Exception> result) {
        return new QueuedTask<>(task, tryCount, finished, result);
    }
}
