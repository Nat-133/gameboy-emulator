package org.gameboy.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.gameboy.common.annotations.Div;
import org.gameboy.common.annotations.Tac;
import org.gameboy.common.annotations.Tima;
import org.gameboy.common.annotations.Tma;

public class CoreModule extends AbstractModule {
    private final boolean useSimpleMemory;
    
    public CoreModule() {
        this(false);
    }
    
    public CoreModule(boolean useSimpleMemory) {
        this.useSimpleMemory = useSimpleMemory;
    }
    
    @Override
    protected void configure() {
        bind(Clock.class).to(SynchronisedClock.class).in(Singleton.class);
        
        if (useSimpleMemory) {
            bind(Memory.class).to(SimpleMemory.class).in(Singleton.class);
        } else {
            bind(Memory.class).to(MappedMemory.class).in(Singleton.class);
        }
        
        bind(RomLoader.class).in(Singleton.class);
        bind(MemoryInitializer.class).in(Singleton.class);
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
        return new IntBackedRegister(0b0000_0100);
    }
}