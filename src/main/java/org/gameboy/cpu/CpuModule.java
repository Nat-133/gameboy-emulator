package org.gameboy.cpu;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.gameboy.common.*;
import org.gameboy.common.annotations.InterruptEnable;
import org.gameboy.common.annotations.InterruptFlags;
import org.gameboy.components.Timer;
import org.gameboy.cpu.annotations.Prefixed;
import org.gameboy.cpu.annotations.Unprefixed;
import org.gameboy.cpu.components.*;
import org.gameboy.display.PictureProcessingUnit;

public class CpuModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ArithmeticUnit.class).in(Singleton.class);
        bind(IncrementDecrementUnit.class).in(Singleton.class);
        
        bind(OpcodeTable.class).annotatedWith(Unprefixed.class)
            .to(UnprefixedOpcodeTable.class).in(Singleton.class);
        bind(OpcodeTable.class).annotatedWith(Prefixed.class)
            .to(PrefixedOpcodeTable.class).in(Singleton.class);
    }
    
    @Provides
    CpuRegisters provideCpuRegisters() {
        return new CpuRegisters(
            (short) 0x01B0,  // af - A=0x01, F=0xB0 (Z=1, N=0, H=1, C=1)
            (short) 0x0013,  // bc - B=0x00, C=0x13
            (short) 0x00D8,  // de - D=0x00, E=0xD8
            (short) 0x014D,  // hl - H=0x01, L=0x4D
            (short) 0xFFFE,  // sp - Stack pointer at 0xFFFE
            (short) 0x0100,  // pc - starts at 0x100 after boot ROM
            (byte) 0x00,     // instructionRegister
            false            // ime - disabled after boot ROM
        );
    }
    
    @Provides
    Decoder provideDecoder(@Unprefixed OpcodeTable unprefixedTable, @Prefixed OpcodeTable prefixedTable) {
        return new Decoder(unprefixedTable, prefixedTable);
    }
    
    @Provides
    @Singleton
    @Named("cpuClock")
    Clock provideCpuClock(Provider<PictureProcessingUnit> ppuProvider,
                          Provider<Timer> timerProvider,
                          Provider<DmaController> dmaControllerProvider,
                          Provider<SerialController> serialControllerProvider) {
        Timer timer = timerProvider.get();
        PictureProcessingUnit ppu = ppuProvider.get();
        DmaController dmaController = dmaControllerProvider.get();
        SerialController serialController = serialControllerProvider.get();

        return new ClockWithParallelProcess(() -> {
            timer.mCycle();
            serialController.mCycle();
            dmaController.mCycle();
            for (int i = 0; i < 4; i++) {
                ppu.tCycle();
            }
        });
    }
    
    @Provides
    @Singleton
    CpuStructure provideCpuStructure(CpuRegisters registers, Memory memory, ArithmeticUnit alu,
                                     IncrementDecrementUnit idu, @Named("cpuClock") Clock clock,
                                     Decoder decoder,
                                     @InterruptFlags ByteRegister interruptFlagsRegister,
                                     @InterruptEnable ByteRegister interruptEnableRegister) {
        InterruptBus interruptBus = new InterruptBus(clock, interruptFlagsRegister, interruptEnableRegister);
        return new CpuStructure(registers, memory, alu, idu, clock, interruptBus, decoder);
    }
    
    @Provides
    @Singleton
    Cpu provideCpu(CpuStructure cpuStructure) {
        return new Cpu(cpuStructure);
    }
}