package org.gameboy.display;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.gameboy.common.*;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

public class DisplayModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PpuRegisters.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    PixelCombinator providePixelCombinator(PpuRegisters registers) {
        return new PixelCombinator(registers);
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
    
    @Provides
    @Singleton
    @Named("ly")
    ByteRegister provideLyRegister() {
        return new IntBackedRegister();
    }
    
    @Provides
    @Singleton
    @Named("lyc")
    ByteRegister provideLycRegister() {
        return new IntBackedRegister();
    }
    
    @Provides
    @Singleton
    @Named("scx")
    ByteRegister provideScxRegister() {
        return new IntBackedRegister();
    }
    
    @Provides
    @Singleton
    @Named("scy")
    ByteRegister provideScyRegister() {
        return new IntBackedRegister();
    }
    
    @Provides
    @Singleton
    @Named("wx")
    ByteRegister provideWxRegister() {
        return new IntBackedRegister();
    }
    
    @Provides
    @Singleton
    @Named("wy")
    ByteRegister provideWyRegister() {
        return new IntBackedRegister();
    }
    
    @Provides
    @Singleton
    @Named("lcdc")
    ByteRegister provideLcdcRegister() {
        return new IntBackedRegister(0x91);
    }
    
    @Provides
    @Singleton
    @Named("stat")
    ByteRegister provideStatRegister() {
        // STAT register has read-only bits (0-2 for mode and coincidence flag)
        return new StatRegister(0x85);
    }

    @Provides
    @Singleton
    @Named("bgp")
    ByteRegister provideBgpRegister() {
        return new IntBackedRegister(0xFC);  // Default palette value
    }

    @Provides
    @Singleton
    @Named("obp0")
    ByteRegister provideObp0Register() {
        return new IntBackedRegister(0xFF);  // Default palette value
    }

    @Provides
    @Singleton
    @Named("obp1")
    ByteRegister provideObp1Register() {
        return new IntBackedRegister(0xFF);  // Default palette value
    }

    @Provides
    @Singleton
    ObjectAttributeMemory provideObjectAttributeMemory(@Named("underlying") Memory memory) {
        return new ObjectAttributeMemory(memory);
    }
    
    @Provides
    @Singleton
    @Named("ppuClock")
    SynchronisedClock providePpuClock() {
        return new SynchronisedClock();
    }
    
    @Provides
    @Singleton
    @Named("backgroundFifo")
    Fifo<TwoBitValue> provideBackgroundFifo() {
        return new Fifo<>();
    }

    @Provides
    @Singleton
    @Named("spriteFifo")
    Fifo<SpritePixel> provideSpriteFifo() {
        return new Fifo<>();
    }
    
    @Provides
    @Singleton
    SpriteBuffer provideSpriteBuffer() {
        return new SpriteBuffer();
    }
    
    @Provides
    @Singleton
    BackgroundFetcher provideBackgroundFetcher(@Named("underlying") Memory memory, PpuRegisters registers,
                                               @Named("backgroundFifo") Fifo<TwoBitValue> backgroundFifo,
                                               @Named("ppuClock") SynchronisedClock ppuClock) {
        return new BackgroundFetcher(memory, registers, backgroundFifo, ppuClock);
    }

    @Provides
    @Singleton
    SpriteFetcher provideSpriteFetcher(SpriteBuffer spriteBuffer, @Named("underlying") Memory memory,
                                       PpuRegisters registers, @Named("spriteFifo") Fifo<SpritePixel> spriteFifo,
                                       @Named("ppuClock") SynchronisedClock ppuClock) {
        return new SpriteFetcher(spriteBuffer, memory, registers, spriteFifo, ppuClock);
    }

    @Provides
    @Singleton
    ScanlineController provideScanlineController(@Named("ppuClock") SynchronisedClock ppuClock, Display display,
                                                @Named("backgroundFifo") Fifo<TwoBitValue> backgroundFifo,
                                                @Named("spriteFifo") Fifo<SpritePixel> spriteFifo,
                                                PixelCombinator pixelCombinator, PpuRegisters registers,
                                                BackgroundFetcher backgroundFetcher, SpriteFetcher spriteFetcher,
                                                SpriteBuffer spriteBuffer) {
        return new ScanlineController(ppuClock, display, backgroundFifo, spriteFifo,
                                    pixelCombinator, registers, backgroundFetcher, spriteFetcher, spriteBuffer);
    }
    
    @Provides
    @Singleton
    OamScanController provideOamScanController(ObjectAttributeMemory oam, @Named("ppuClock") SynchronisedClock ppuClock,
                                              SpriteBuffer spriteBuffer, PpuRegisters registers) {
        return new OamScanController(oam, ppuClock, spriteBuffer, registers);
    }
    
    @Provides
    @Singleton
    PictureProcessingUnit providePictureProcessingUnit(ScanlineController scanlineController,
                                                      PpuRegisters registers,
                                                      @Named("ppuClock") SynchronisedClock ppuClock,
                                                      OamScanController oamScanController,
                                                      DisplayInterruptController displayInterruptController,
                                                      Display display) {
        return new PictureProcessingUnit(scanlineController, registers, ppuClock, oamScanController, displayInterruptController, display);
    }
}