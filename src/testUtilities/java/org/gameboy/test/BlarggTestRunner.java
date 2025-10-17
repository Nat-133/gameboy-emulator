package org.gameboy.test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gameboy.EmulatorModule;
import org.gameboy.common.MappedMemory;
import org.gameboy.common.Memory;
import org.gameboy.common.MemoryDump;
import org.gameboy.common.SerialController;
import org.gameboy.cpu.Cpu;

import java.util.List;

public class BlarggTestRunner {
    private final Cpu cpu;
    private final SerialController serialController;
    private int cycleCount = 0;

    public BlarggTestRunner(byte[] testRomData) {
        Injector injector = Guice.createInjector(new EmulatorModule());

        List<MemoryDump> memoryDumps = new java.util.ArrayList<>();
        memoryDumps.add(MemoryDump.fromZero(testRomData));

        Memory memory = injector.getInstance(Memory.class);
        if (memory instanceof MappedMemory mappedMemory) {
            mappedMemory.loadMemoryDumps(memoryDumps);
        }

        cpu = injector.getInstance(Cpu.class);
        serialController = injector.getInstance(SerialController.class);
    }

    public TestResult runTest(int maxCycles) {
        while (cycleCount < maxCycles) {
                cpu.cycle();
                cycleCount++;

            String output = serialController.getOutput();

            if (outputIndicatesCompletion(output)) {
                return new TestResult(
                        testsPass(output),
                        output,
                        cycleCount,
                        extractTestName(output)
                );
            }
        }

        return new TestResult(
                false,
                serialController.getOutput() + "\n[Test timed out after " + cycleCount + " cycles]",
                cycleCount,
                extractTestName(serialController.getOutput())
        );
    }

    private boolean outputIndicatesCompletion(String output) {
        return output.contains("Passed all tests") ||
                output.contains("Failed") ||
                output.contains("Passed") ||
                output.contains("FAILED");
    }

    private boolean testsPass(String output) {
        return output.contains("Passed all tests") ||
                (output.contains("Passed") && !output.contains("Failed"));
    }

    private String extractTestName(String output) {
        String[] lines = output.split("\n");
        if (lines.length > 0 && !lines[0].trim().isEmpty()) {
            return lines[0].trim();
        }
        return "Unknown Test";
    }

    public record TestResult(boolean passed, String output, int cycles, String testName) {
        public void print() {
            System.out.println("=".repeat(70));
            System.out.println("Result: " + (passed ? "PASSED" : "FAILED"));
            System.out.println("Cycles: " + cycles);
        }
    }
}