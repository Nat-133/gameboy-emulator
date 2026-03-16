package org.gameboy.audio;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.gameboy.audio.annotations.ApuRegisters;
import org.gameboy.common.ByteRegister;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(org.gameboy.audio.ApuRegisters.class).in(Singleton.class);
        bind(Apu.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    ConcurrentLinkedQueue<short[]> provideSampleQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Provides
    @Singleton
    @ApuRegisters
    Map<Integer, ByteRegister> provideApuRegisterMap(org.gameboy.audio.ApuRegisters registers) {
        return registers.getRegisterMap();
    }
}
