package org.gameboy.common;

public class RealTimeFramePacer implements FramePacer {
    private static final double DMG_CLOCK_HZ = 4_194_304.0;
    private static final int T_CYCLES_PER_FRAME = 70_224;
    private static final double FRAMES_PER_SECOND = DMG_CLOCK_HZ / T_CYCLES_PER_FRAME;
    private static final long FRAME_DURATION_NS = (long) (1_000_000_000L / FRAMES_PER_SECOND);

    private long frameStartNanos;

    public RealTimeFramePacer() {
        frameStartNanos = System.nanoTime();
    }

    @Override
    public void sync() {
        long deadline = frameStartNanos + FRAME_DURATION_NS;
        long now = System.nanoTime();
        long remaining = deadline - now;

        if (remaining > 1_000_000) {
            try {
                Thread.sleep((remaining - 1_000_000) / 1_000_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        while (System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }

        frameStartNanos = System.nanoTime();
    }
}
