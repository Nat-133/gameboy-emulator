package org.gameboy;

import com.google.inject.AbstractModule;
import org.gameboy.common.CoreModule;
import org.gameboy.cpu.CpuModule;
import org.gameboy.display.DisplayModule;

public class EmulatorModule extends AbstractModule {
    private final boolean useSimpleMemory;
    
    public EmulatorModule() {
        this(true);
    }
    
    public EmulatorModule(boolean useSimpleMemory) {
        this.useSimpleMemory = useSimpleMemory;
    }
    
    @Override
    protected void configure() {
        install(new CoreModule(useSimpleMemory));
        install(new CpuModule());
        install(new DisplayModule());
    }
}