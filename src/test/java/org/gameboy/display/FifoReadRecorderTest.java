package org.gameboy.display;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.gameboy.TestUtils.waitFor;

class FifoReadRecorderTest {
    @Test
    void givenReadHasHappened_whenWaitForRead_thenImmediateReturn() throws TimeoutException {
        FifoReadRecorder fifoReadRecorder = new FifoReadRecorder();

        fifoReadRecorder.onRead();

        Thread waitThread = new Thread(fifoReadRecorder::waitForFifoRead);

        waitThread.start();
        waitFor(() -> !waitThread.isAlive());
    }

    @Test
    void givenNoRead_whenWaitForRead_thenOnlyReturnsWhenReadHappens() throws TimeoutException {
        FifoReadRecorder fifoReadRecorder = new FifoReadRecorder();

        Thread waitThread = new Thread(fifoReadRecorder::waitForFifoRead);

        waitThread.start();
        waitFor(() -> waitThread.getState() == Thread.State.WAITING || waitThread.getState() == Thread.State.BLOCKED);

        fifoReadRecorder.onRead();

        waitFor(() -> !waitThread.isAlive());
    }

    @Test
    void givenRead_whenReset_thenListenerWaitsForNextRead() throws TimeoutException {
        FifoReadRecorder fifoReadRecorder = new FifoReadRecorder();
        fifoReadRecorder.onRead();

        fifoReadRecorder.reset();

        Thread waitThread = new Thread(fifoReadRecorder::waitForFifoRead);
        waitThread.start();
        waitFor(() -> waitThread.getState() == Thread.State.WAITING || waitThread.getState() == Thread.State.BLOCKED);
        fifoReadRecorder.onRead();
        waitFor(() -> !waitThread.isAlive());
    }
}