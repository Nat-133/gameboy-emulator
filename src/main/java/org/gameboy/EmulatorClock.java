package org.gameboy;

import org.gameboy.audio.Apu;
import org.gameboy.common.Clock;
import org.gameboy.common.ClockWithParallelProcess;
import org.gameboy.common.DmaController;
import org.gameboy.common.RealTimeFramePacer;
import org.gameboy.common.SerialController;
import org.gameboy.components.Timer;
import org.gameboy.display.PictureProcessingUnit;

public class EmulatorClock implements Clock {
    private final ClockWithParallelProcess clock;

    public EmulatorClock(PictureProcessingUnit ppu,
                         Timer timer,
                         DmaController dmaController,
                         SerialController serialController,
                         Apu apu) {
        this.clock = new ClockWithParallelProcess(() -> {
            timer.mCycle();
            serialController.mCycle();
            dmaController.mCycle();
            for (int i = 0; i < 4; i++) {
                ppu.tCycle();
                apu.tCycle();
            }
        }, new RealTimeFramePacer());
    }

    @Override
    public void tick() {
        clock.tick();
    }

    @Override
    public long getTime() {
        return clock.getTime();
    }

    @Override
    public void stop() {
        clock.stop();
    }

    @Override
    public void start() {
        clock.start();
    }
}
