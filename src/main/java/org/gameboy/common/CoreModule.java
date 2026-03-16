package org.gameboy.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.gameboy.common.annotations.*;
import org.gameboy.components.DividerRegister;
import org.gameboy.components.InternalTimerCounter;
import org.gameboy.components.TacRegister;
import org.gameboy.components.TimaRegister;
import org.gameboy.components.Timer;

public class CoreModule extends AbstractModule {

    public CoreModule() {
    }

    @Override
    protected void configure() {
        bind(Clock.class).to(SynchronisedClock.class).in(Singleton.class);

        bind(Memory.class).annotatedWith(UnderlyingMemory.class).to(MappedMemory.class).in(Singleton.class);
        bind(MemoryBus.class).in(Singleton.class);
        bind(Memory.class).to(MemoryBus.class);
        bind(DmaController.class).to(MemoryBus.class);

        bind(SerialController.class).in(Singleton.class);
        bind(InterruptController.class).in(Singleton.class);
        bind(Timer.class).in(Singleton.class);

        bind(ByteRegister.class).annotatedWith(Tma.class).toInstance(new IntBackedRegister());
        bind(ByteRegister.class).annotatedWith(Dma.class).toInstance(new IntBackedRegister());
        bind(ByteRegister.class).annotatedWith(InterruptFlags.class).toInstance(new InterruptFlagsRegister());
        bind(ByteRegister.class).annotatedWith(InterruptEnable.class).toInstance(new IntBackedRegister());
    }

    @Provides
    @Singleton
    InternalTimerCounter provideInternalTimerCounter() {
        // Post-boot state for DMG/MGB (DIV = $AB, aligned for boot_sclk_align test)
        return new InternalTimerCounter(0xAAC8);
    }

    @Provides
    @Singleton
    @Div
    ByteRegister provideDivRegister(InternalTimerCounter internalCounter) {
        return new DividerRegister(internalCounter);
    }

    @Provides
    @Singleton
    @Tac
    TacRegister provideTacRegister() {
        return new TacRegister();
    }

    @Provides
    @Singleton
    @Tac
    ByteRegister provideTacByteRegister(@Tac TacRegister tacRegister) {
        return tacRegister;
    }

    @Provides
    @Singleton
    @Tima
    ByteRegister provideTimaRegister(TimaRegister timaRegister) {
        return timaRegister;
    }

}