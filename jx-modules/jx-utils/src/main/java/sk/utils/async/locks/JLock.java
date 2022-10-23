package sk.utils.async.locks;

import sk.utils.functional.Gett;
import sk.utils.functional.R;

import java.util.concurrent.locks.Lock;

public interface JLock extends Lock {
    //public default boolean runIfLockFree(R toRun) {
    //    return getIfLockFree(() -> {
    //        toRun.run();
    //        return true;
    //    }).orElse(false);
    //}

    public default void runInLock(R toRun) {
        getInLock(() -> {
            toRun.run();
            return null;
        });
    }
    //
    //public default <T> O<T> getIfLockFree(Gett<T> getter) {
    //    if (tryLock()) {
    //        try {
    //            return O.of(getter.get());
    //        } finally {
    //            unlock();
    //        }
    //    } else {
    //        return O.empty();
    //    }
    //}

    public default <T> T getInLock(Gett<T> toRun) {
        try {
            lock();
            return toRun.get();
        } finally {
            unlock();
        }
    }
}
