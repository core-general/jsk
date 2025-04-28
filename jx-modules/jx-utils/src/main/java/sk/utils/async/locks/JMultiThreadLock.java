package sk.utils.async.locks;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import sk.exceptions.NotImplementedException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class JMultiThreadLock implements JLock {
    private final Semaphore sema = new Semaphore(1, true);

    @Override
    public boolean isLocked() {
        return sema.availablePermits() == 0;
    }

    @Override
    public void lock() {
        try {
            sema.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sema.acquireUninterruptibly();
    }

    @Override
    public boolean tryLock() {
        return sema.tryAcquire();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sema.tryAcquire(time, unit);
    }

    @Override
    public void unlock() {
        synchronized (sema) {
            if (sema.availablePermits() == 0) {
                sema.release();
            }
        }
    }

    @Override
    public synchronized Condition newCondition() {
        throw new NotImplementedException();
    }
}
