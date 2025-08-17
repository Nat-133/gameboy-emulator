package org.gameboy.display;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.gameboy.common.*;

public class DisplayModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PixelCombinator.class).in(Singleton.class);
        
        bind(Display.class).to(WindowDisplay.class).in(Singleton.class);
        
        bind(PpuRegisters.class).in(Singleton.class);
        
    }
    
    @Provides
    @Singleton
    DisplayInterruptController provideDisplayInterruptController(InterruptController interruptController, PpuRegisters ppuRegisters) {
        return new DisplayInterruptController(interruptController, ppuRegisters);
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
        return new IntBackedRegister(0x85);
    }
    
    @Provides
    @Singleton
    ObjectAttributeMemory provideObjectAttributeMemory(Memory memory) {
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
    PixelFifo provideBackgroundFifo() {
        return new PixelFifo();
    }
    
    @Provides
    @Singleton
    @Named("spriteFifo")
    PixelFifo provideSpriteFifo() {
        return new PixelFifo();
    }
    
    @Provides
    @Singleton
    SpriteBuffer provideSpriteBuffer() {
        return new SpriteBuffer();
    }
    
    @Provides
    @Singleton
    BackgroundFetcher provideBackgroundFetcher(Memory memory, PpuRegisters registers, 
                                               @Named("backgroundFifo") PixelFifo backgroundFifo, 
                                               @Named("ppuClock") SynchronisedClock ppuClock) {
        return new BackgroundFetcher(memory, registers, backgroundFifo, ppuClock);
    }
    
    @Provides
    @Singleton
    SpriteFetcher provideSpriteFetcher(SpriteBuffer spriteBuffer, Memory memory, 
                                       PpuRegisters registers, @Named("spriteFifo") PixelFifo spriteFifo, 
                                       @Named("ppuClock") SynchronisedClock ppuClock) {
        return new SpriteFetcher(spriteBuffer, memory, registers, spriteFifo, ppuClock);
    }
    
    @Provides
    @Singleton
    ScanlineController provideScanlineController(@Named("ppuClock") SynchronisedClock ppuClock, Display display,
                                                @Named("backgroundFifo") PixelFifo backgroundFifo,
                                                @Named("spriteFifo") PixelFifo spriteFifo,
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
                                                      DisplayInterruptController displayInterruptController) {
        return new PictureProcessingUnit(scanlineController, registers, ppuClock, oamScanController, displayInterruptController);
    }
}