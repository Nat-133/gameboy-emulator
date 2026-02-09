package org.gameboy;

import com.google.inject.AbstractModule;
import org.gameboy.common.CoreModule;
import org.gameboy.components.joypad.JoypadModule;
import org.gameboy.cpu.CpuModule;
import org.gameboy.display.DisplayModule;

public class EmulatorModule extends AbstractModule {

    public EmulatorModule() {
    }
    
    @Override
    protected void configure() {
        install(new CoreModule());
        install(new CpuModule());
        install(new DisplayModule());
        install(new JoypadModule());
    }
}