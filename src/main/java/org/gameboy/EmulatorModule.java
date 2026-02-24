package org.gameboy;

import com.google.inject.AbstractModule;
import org.gameboy.common.Cartridge;
import org.gameboy.common.CoreModule;
import org.gameboy.components.joypad.JoypadModule;
import org.gameboy.cpu.CpuModule;
import org.gameboy.display.DisplayModule;
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
    }
}