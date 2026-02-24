package org.gameboy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gameboy.common.Cartridge;
import org.gameboy.cartridge.RomOnlyCartridge;
import org.gameboy.cpu.Cpu;
import org.gameboy.cpu.components.CpuStructure;

public class MooneyeTestRunner {
    private static final int STAGNATION_THRESHOLD = 100; // Cycles with unchanged PC = infinite loop

    private final Cpu cpu;
    private final CpuStructure cpuStructure;
    private int cycleCount = 0;
    private short lastPC = -1;
    private int stagnationCount = 0;

    public MooneyeTestRunner(byte[] romData) {
        Cartridge cartridge = new RomOnlyCartridge(romData);
        Injector injector = Guice.createInjector(new EmulatorModule(cartridge));

        cpu = injector.getInstance(Cpu.class);
        cpuStructure = injector.getInstance(CpuStructure.class);
    }

    public RegisterState runUntilCompletion(int maxCycles) {
        while (cycleCount < maxCycles && !isInfiniteLoop()) {
            cpu.cycle();
            cycleCount++;

            short currentPC = cpuStructure.registers().PC();
            if (currentPC == lastPC) {
                stagnationCount++;
            } else {
                stagnationCount = 0;
                lastPC = currentPC;
            }
        }

        return new RegisterState(
                cpuStructure.registers().B(),
                cpuStructure.registers().C(),
                cpuStructure.registers().D(),
                cpuStructure.registers().E(),
                cpuStructure.registers().H(),
                cpuStructure.registers().L(),
                cpuStructure.registers().PC(),
                cycleCount
        );
    }

    private boolean isInfiniteLoop() {
        return stagnationCount >= STAGNATION_THRESHOLD;
    }

    public record RegisterState(byte b, byte c, byte d, byte e, byte h, byte l, short pc, int cycles) {
        public int cycles() {
            return cycles;
        }

        public boolean isSuccess() {
            return b == 3 && c == 5 && d == 8 && e == 13 && h == 21 && l == 34;
        }

        public boolean isExpectedFailure() {
            return b == 0x42 && c == 0x42 && d == 0x42 && e == 0x42 && h == 0x42 && l == 0x42;
        }

        public String toHexString() {
            return String.format("B=0x%02X, C=0x%02X, D=0x%02X, E=0x%02X, H=0x%02X, L=0x%02X",
                    b & 0xFF, c & 0xFF, d & 0xFF, e & 0xFF, h & 0xFF, l & 0xFF);
        }
    }
}
