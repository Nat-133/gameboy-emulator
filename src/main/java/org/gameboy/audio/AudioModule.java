package org.gameboy.audio;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.gameboy.common.ByteRegister;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioModule extends AbstractModule {
    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    ConcurrentLinkedQueue<short[]> provideSampleQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Provides
    @Singleton
    ApuRegisters provideApuRegisters() {
        return new ApuRegisters();
    }

    @Provides
    @Singleton
    @Named("apuRegisters")
    Map<Integer, ByteRegister> provideApuRegisterMap(ApuRegisters registers) {
        return registers.getRegisterMap();
    }

    @Provides
    @Singleton
    Apu provideApu(ApuRegisters registers, ConcurrentLinkedQueue<short[]> sampleQueue) {
        return new Apu(registers, sampleQueue);
    }
}
