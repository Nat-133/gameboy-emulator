package org.gameboy.display;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SignalObserver {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition signal = lock.newCondition();
    private volatile boolean signalOccured = false;

    public void waitForSignal() {
        lock.lock();
        try {
            if (!signalOccured) {
                signal.await();
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
            signalOccured = false;
        } finally {
            lock.unlock();
        }
    }

    public void signal() {
        lock.lock();
        try {
            signalOccured = true;
            signal.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
