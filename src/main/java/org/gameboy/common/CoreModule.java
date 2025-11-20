package org.gameboy.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.gameboy.common.annotations.*;
import org.gameboy.components.DividerRegister;
import org.gameboy.components.InternalTimerCounter;
import org.gameboy.components.TacRegister;
import org.gameboy.components.Timer;

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
    InternalTimerCounter provideInternalTimerCounter() {
        return new InternalTimerCounter();
    }

    @Provides
    @Singleton
    @Div
    ByteRegister provideDivRegister(InternalTimerCounter internalCounter) {
        return new DividerRegister(internalCounter);
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
    TacRegister provideTacRegister() {
        return new TacRegister();
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

    @Provides
    @Singleton
    Timer provideTimer(InternalTimerCounter internalCounter,
                       @Tma ByteRegister tma,
                       @Tac TacRegister tac,
                       InterruptController interruptController) {
        ByteRegister unwrappedTima = new IntBackedRegister();
        return new Timer(internalCounter, unwrappedTima, tma, tac, interruptController);
    }

    @Provides
    @Singleton
    @Tima
    ByteRegister provideTimaRegister(Timer timer) {
        return timer.getTimaRegister();
    }

}