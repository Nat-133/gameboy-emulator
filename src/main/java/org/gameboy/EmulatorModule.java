package org.gameboy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.gameboy.cpu.annotations.CpuClock;
import org.gameboy.audio.Apu;
import org.gameboy.audio.AudioModule;
import org.gameboy.common.Cartridge;
import org.gameboy.common.Clock;
import org.gameboy.common.DmaController;
import org.gameboy.common.CoreModule;
import org.gameboy.common.SerialController;
import org.gameboy.components.Timer;
import org.gameboy.components.joypad.JoypadModule;
import org.gameboy.cpu.CpuModule;
import org.gameboy.display.DisplayModule;
import org.gameboy.display.PictureProcessingUnit;
import org.gameboy.io.IoModule;

public class EmulatorModule extends AbstractModule {
    private final Cartridge cartridge;

    public EmulatorModule(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    @Override
    protected void configure() {
        bind(Cartridge.class).toInstance(cartridge);
        install(new CoreModule());
        install(new CpuModule());
        install(new DisplayModule());
        install(new JoypadModule());
        install(new IoModule());
        install(new AudioModule());
    }

    @Provides
    @Singleton
    @CpuClock
    Clock provideCpuClock(PictureProcessingUnit ppu,
                          Timer timer,
                          DmaController dmaController,
                          SerialController serialController,
                          Apu apu) {
        return new EmulatorClock(ppu, timer, dmaController, serialController, apu);
    }
}