package sk.utils.async.locks;

import lombok.AllArgsConstructor;

import java.util.concurrent.locks.ReadWriteLock;

@AllArgsConstructor
public class JReadWriteLockDecorator implements JReadWriteLock {
    private ReadWriteLock lock;

    @Override
    public JLock writeLock() {
        return new JLockDecorator(lock.writeLock());
    }

    @Override
    public JLock readLock() {
        return new JLockDecorator(lock.readLock());
    }
}
