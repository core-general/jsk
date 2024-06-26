package sk.db.relational.spring.services.impl;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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
import jakarta.persistence.OptimisticLockException;
import lombok.NoArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import sk.db.relational.spring.services.EntityManagerProvider;
import sk.db.relational.spring.services.RdbTransactionManager;
import sk.db.relational.spring.services.RdbTransactionWrapper;
import sk.services.retry.IRepeat;
import sk.utils.functional.F0;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.javafixes.FlushableDetachable;
import sk.utils.tuples.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static sk.utils.statics.Cc.s;

@SuppressWarnings("unused")
@NoArgsConstructor
public abstract class RdbTransactionManagerImpl implements RdbTransactionManager {
    @Inject SessionFactory sf;
    @Inject EntityManagerProvider em;
    @Inject IRepeat retry;
    @Inject RdbTransactionWrapper trans;

    public RdbTransactionManagerImpl(EntityManagerProvider em, RdbTransactionWrapper trans) {
        this.trans = trans;
        this.em = em;
    }

    @Override
    public <T> T transactional(Supplier<T> sup) {
        return trans.transactional(sup);
    }

    @Override
    public void transactionalRun(Runnable run) {
        trans.transactionalRun(run);
    }

    @Override
    public <T> T transactionalForceNew(Supplier<T> sup) {
        return trans.transactionalForceNew(sup);
    }

    @Override
    public void transactionalRunForceNew(Runnable run) {
        trans.transactionalRunForceNew(run);
    }

    @SuppressWarnings("WeakerAccess")
    protected abstract void saveSingleItem(Object singleItem);

    @Override
    public <A, T extends X1<A>> T transactionWithSaveX1(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 1, false);
    }

    @Override
    public <A1, A2, T extends X2<A1, A2>> T transactionWithSaveX2(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 2, false);
    }

    @Override
    public <A1, A2, A3, T extends X3<A1, A2, A3>> T transactionWithSaveX3(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 3, false);
    }

    @Override
    public <A1, A2, A3, A4, T extends X4<A1, A2, A3, A4>> T transactionWithSaveX4(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 4, false);
    }

    @Override
    public <A1, A2, A3, A4, A5, T extends X5<A1, A2, A3, A4, A5>> T transactionWithSaveX5(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 5, false);
    }

    @Override
    public <A1, A2, A3, A4, A5, A6, T extends X6<A1, A2, A3, A4, A5, A6>> T transactionWithSaveX6(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 6, false);
    }

    @Override
    public <A1, A2, A3, A4, A5, A6, A7, T extends X7<A1, A2, A3, A4, A5, A6, A7>> T transactionWithSaveX7(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 7, false);
    }

    @Override
    public <A, T extends X1<A>> T transactionWithSaveX1ForceNew(Supplier<T> howToGet, F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 1, true);
    }

    @Override
    public <A1, A2, T extends X2<A1, A2>> T transactionWithSaveX2ForceNew(Supplier<T> howToGet, F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 2, true);
    }

    @Override
    public <A1, A2, A3, T extends X3<A1, A2, A3>> T transactionWithSaveX3ForceNew(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 3, true);
    }

    @Override
    public <A1, A2, A3, A4, T extends X4<A1, A2, A3, A4>> T transactionWithSaveX4ForceNew(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 4, true);
    }

    @Override
    public <A1, A2, A3, A4, A5, T extends X5<A1, A2, A3, A4, A5>> T transactionWithSaveX5ForceNew(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 5, true);
    }

    @Override
    public <A1, A2, A3, A4, A5, A6, T extends X6<A1, A2, A3, A4, A5, A6>> T transactionWithSaveX6ForceNew(Supplier<T> howToGet,
            F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 6, true);
    }

    @Override
    public <A1, A2, A3, A4, A5, A6, A7, T extends X7<A1, A2, A3, A4, A5, A6, A7>> T transactionWithSaveX7ForceNew(
            Supplier<T> howToGet, F1<T, List<Object>> whatToSave) {
        return transactionWithSaveUni(howToGet, whatToSave, 7, true);
    }

    private <T> T transactionWithSaveUni(Supplier<T> howToGet, F1<T, List<Object>> toSave, int count, boolean forceNew) {
        return transactionWithSaveUniUni(() -> {
            final Supplier<T> lambda = () -> {
                T t = howToGet.get();
                List<Object> apply = toSave.apply(t);
                apply.forEach(this::tryDetachIfNeeded);
                apply.forEach(this::trySave);
                return t;
            };
            return forceNew ? trans.transactionalForceNew(lambda) : trans.transactional(lambda);
        });
    }

    private void tryDetachIfNeeded(Object toDetach) {
        if (toDetach == null) {
            throw new RuntimeException("must not be null");
        }
        if (toDetach instanceof O) {
            //noinspection unchecked
            ((O) toDetach).ifPresent(this::tryDetachIfNeeded);
        } else if (toDetach instanceof Optional) {
            //noinspection unchecked
            ((Optional) toDetach).ifPresent(this::tryDetachIfNeeded);
        } else if (toDetach instanceof Collection) {
            //noinspection unchecked
            ((Collection) toDetach).forEach(this::tryDetachIfNeeded);
        } else {
            if (toDetach instanceof FlushableDetachable detach && detach.isDetach()) {
                em.getEntityManager().detach(toDetach);
            }
        }
    }

    private void trySave(Object toSave) {
        if (toSave == null) {
            throw new RuntimeException("must not be null");
        }
        if (toSave instanceof O) {
            //noinspection unchecked
            ((O) toSave).ifPresent(this::trySave);
        } else if (toSave instanceof Optional) {
            //noinspection unchecked
            ((Optional) toSave).ifPresent(this::trySave);
        } else if (toSave instanceof Collection) {
            //noinspection unchecked
            ((Collection) toSave).forEach(this::trySave);
        } else {
            saveSingleItem(toSave);
            if (toSave instanceof FlushableDetachable flush && flush.isFlush()) {
                em.getEntityManager().flush();
            }
        }
    }

    private <T> T transactionWithSaveUniUni(F0<T> sup) {
        return retry.repeat(sup, 50, 100, s(
                ObjectOptimisticLockingFailureException.class,
                OptimisticLockException.class,
                OptimisticEntityLockException.class,
                StaleObjectStateException.class
        ));
    }
}
