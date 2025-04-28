package org.gameboy.display;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.gameboy.TestUtils.waitFor;

class SignalObserverTest {
    @Test
    void givenReadHasHappened_whenWaitForRead_thenImmediateReturn() throws TimeoutException {
        SignalObserver signalObserver = new SignalObserver();

        signalObserver.signal();

        Thread waitThread = new Thread(signalObserver::waitForSignal);

        waitThread.start();
        waitFor(() -> !waitThread.isAlive());
    }

    @Test
    void givenNoRead_whenWaitForRead_thenOnlyReturnsWhenReadHappens() throws TimeoutException {
        SignalObserver signalObserver = new SignalObserver();

        Thread waitThread = new Thread(signalObserver::waitForSignal);

        waitThread.start();
        waitFor(() -> waitThread.getState() == Thread.State.WAITING || waitThread.getState() == Thread.State.BLOCKED);

        signalObserver.signal();

        waitFor(() -> !waitThread.isAlive());
    }

    @Test
    void givenRead_whenReset_thenListenerWaitsForNextRead() throws TimeoutException {
        SignalObserver signalObserver = new SignalObserver();
        signalObserver.signal();

        signalObserver.reset();

        Thread waitThread = new Thread(signalObserver::waitForSignal);
        waitThread.start();
        waitFor(() -> waitThread.getState() == Thread.State.WAITING || waitThread.getState() == Thread.State.BLOCKED);
        signalObserver.signal();
        waitFor(() -> !waitThread.isAlive());
    }
}