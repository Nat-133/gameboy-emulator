package org.gameboy.display;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FifoReadRecorder implements FifoReadListener {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition read = lock.newCondition();
    private volatile boolean readOccurred = false;

    public void waitForFifoRead() {
        lock.lock();
        try {
            if (!readOccurred) {
                read.await();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public void reset() {
        lock.lock();
        try {
            readOccurred = false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onRead() {
        lock.lock();
        try {
            readOccurred = true;
            read.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
