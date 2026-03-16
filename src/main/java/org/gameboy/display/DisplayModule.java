package org.gameboy.display;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import org.gameboy.common.*;
import org.gameboy.display.annotations.*;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

public class DisplayModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PpuRegisters.class).in(Singleton.class);

        // Registers
        bind(ByteRegister.class).annotatedWith(Ly.class).toInstance(new IntBackedRegister());
        bind(ByteRegister.class).annotatedWith(Lyc.class).toInstance(new IntBackedRegister());
        bind(ByteRegister.class).annotatedWith(Scx.class).toInstance(new IntBackedRegister());
        bind(ByteRegister.class).annotatedWith(Scy.class).toInstance(new IntBackedRegister());
        bind(ByteRegister.class).annotatedWith(Wx.class).toInstance(new IntBackedRegister());
        bind(ByteRegister.class).annotatedWith(Wy.class).toInstance(new IntBackedRegister());
        bind(ByteRegister.class).annotatedWith(Lcdc.class).toInstance(new IntBackedRegister(0x91));
        bind(ByteRegister.class).annotatedWith(Stat.class).toInstance(new StatRegister(0x85));
        bind(ByteRegister.class).annotatedWith(Bgp.class).toInstance(new IntBackedRegister(0xFC));
        bind(ByteRegister.class).annotatedWith(Obp0.class).toInstance(new IntBackedRegister(0xFF));
        bind(ByteRegister.class).annotatedWith(Obp1.class).toInstance(new IntBackedRegister(0xFF));

        // PPU clock and FIFOs
        SynchronisedClock ppuClock = new SynchronisedClock();
        bind(SynchronisedClock.class).annotatedWith(PpuClock.class).toInstance(ppuClock);
        bind(Clock.class).annotatedWith(PpuClock.class).toInstance(ppuClock);
        bind(new TypeLiteral<Fifo<TwoBitValue>>() {}).annotatedWith(BackgroundFifo.class).toInstance(new Fifo<>());
        bind(new TypeLiteral<Fifo<SpritePixel>>() {}).annotatedWith(SpriteFifo.class).toInstance(new Fifo<>());

        // Components with @Inject constructors
        bind(SpriteBuffer.class).in(Singleton.class);
        bind(ObjectAttributeMemory.class).in(Singleton.class);
        bind(PixelCombinator.class).in(Singleton.class);
        bind(BackgroundFetcher.class).in(Singleton.class);
        bind(SpriteFetcher.class).in(Singleton.class);
        bind(ScanlineController.class).in(Singleton.class);
        bind(OamScanController.class).in(Singleton.class);
        bind(PictureProcessingUnit.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    DisplayInterruptController provideDisplayInterruptController(InterruptController interruptController, PpuRegisters ppuRegisters, Memory memory) {
        DisplayInterruptController controller = new DisplayInterruptController(interruptController, ppuRegisters);
        // Register listener for LYC writes (0xFF45) to handle mid-scanline LYC changes
        memory.registerMemoryListener((short) 0xFF45, controller::checkAndSendLyCoincidence);
        // Register listener for STAT writes (0xFF41) to re-evaluate STAT interrupt condition
        memory.registerMemoryListener((short) 0xFF41, controller::checkStatCondition);
        return controller;
    }
}
