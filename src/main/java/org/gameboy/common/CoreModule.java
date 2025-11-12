package org.gameboy.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.gameboy.common.annotations.*;

public class CoreModule extends AbstractModule {

    public CoreModule() {
    }

    @Override
    protected void configure() {
        bind(Clock.class).to(SynchronisedClock.class).in(Singleton.class);

        bind(Memory.class).annotatedWith(Names.named("underlying")).to(MappedMemory.class).in(Singleton.class);

        bind(RomLoader.class).in(Singleton.class);
        bind(MemoryInitializer.class).in(Singleton.class);
        bind(SerialController.class).in(Singleton.class);
        bind(InterruptController.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    MemoryBus provideMemoryBus(@Named("underlying") Memory underlying) {
        return new MemoryBus(underlying);
    }

    @Provides
    @Singleton
    Memory provideMemory(MemoryBus memoryBus) {
        return memoryBus;
    }

    @Provides
    @Singleton
    DmaController provideDmaController(MemoryBus memoryBus) {
        return memoryBus;
    }
    
    
    
    @Provides
    @Singleton
    @Div
    ByteRegister provideDivRegister() {
        return new IntBackedRegister();
    }
    
    @Provides
    @Singleton
    @Tima
    ByteRegister provideTimaRegister() {
        return new IntBackedRegister();
    }
    
    @Provides
    @Singleton
    @Tma
    ByteRegister provideTmaRegister() {
        return new IntBackedRegister();
    }
    
    @Provides
    @Singleton
    @Tac
    ByteRegister provideTacRegister() {
        return new IntBackedRegister(0xF8);
    }

    @Provides
    @Singleton
    @Dma
    ByteRegister provideDmaRegister() {
        return new IntBackedRegister();
    }

    @Provides
    @Singleton
    @InterruptFlags
    ByteRegister provideInterruptFlagsRegister() {
        return new InterruptFlagsRegister();
    }

    @Provides
    @Singleton
    @InterruptEnable
    ByteRegister provideInterruptEnableRegister() {
        return new IntBackedRegister();
    }

}