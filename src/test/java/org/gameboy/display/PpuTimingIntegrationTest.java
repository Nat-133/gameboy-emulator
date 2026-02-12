package org.gameboy.display;

import org.gameboy.TestMemory;
import org.gameboy.common.IntBackedRegister;
import org.gameboy.common.InterruptController;
import org.gameboy.common.SynchronisedClock;
import org.gameboy.utils.MultiBitValue.TwoBitValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.display.PpuRegisters.PpuRegister.*;

/**
 * Integration tests for PPU timing accuracy.
 *
 * These tests verify that the PPU completes scanlines in the correct number of T-cycles.
 * On the Game Boy, each scanline takes exactly 456 T-cycles:
 * - OAM Scan: 80 T-cycles
 * - Drawing: ~172 T-cycles (variable based on sprites/scrolling)
 * - HBlank: ~204 T-cycles (remainder to reach 456)
 */
public class PpuTimingIntegrationTest {

    private static final int SCANLINE_T_CYCLES = 456;
    private static final int FRAME_SCANLINES = 154; // 144 visible + 10 VBlank

    private PictureProcessingUnit ppu;
    private PpuRegisters registers;
    private TestMemory memory;
    private SynchronisedClock ppuClock;

    @BeforeEach
    void setUp() {
        memory = new TestMemory();
        ppuClock = new SynchronisedClock();

        // Create PPU registers with default values
        registers = new PpuRegisters(
            new IntBackedRegister(0),      // LY
            new IntBackedRegister(0),      // LYC
            new IntBackedRegister(0),      // SCX
            new IntBackedRegister(0),      // SCY
            new IntBackedRegister(0),      // WX
            new IntBackedRegister(0),      // WY
            new IntBackedRegister(0x91),   // LCDC - LCD enabled, BG enabled
            new StatRegister(0x00),        // STAT
            new IntBackedRegister(0xFC),   // BGP
            new IntBackedRegister(0xFF),   // OBP0
            new IntBackedRegister(0xFF)    // OBP1
        );

        // Create interrupt controller with a dummy register
        InterruptController interruptController = new InterruptController(new IntBackedRegister(0));

        // Create display interrupt controller
        DisplayInterruptController displayInterruptController =
            new DisplayInterruptController(interruptController, registers);

        // Create OAM
        ObjectAttributeMemory oam = new ObjectAttributeMemory(memory);

        // Create sprite buffer
        SpriteBuffer spriteBuffer = new SpriteBuffer();

        // Create OAM scan controller
        OamScanController oamScanController = new OamScanController(oam, ppuClock, spriteBuffer, registers);

        // Create FIFOs
        Fifo<TwoBitValue> backgroundFifo = new Fifo<>();
        Fifo<SpritePixel> spriteFifo = new Fifo<>();

        // Create pixel combinator
        PixelCombinator pixelCombinator = new PixelCombinator(registers);

        // Create fetchers
        BackgroundFetcher backgroundFetcher = new BackgroundFetcher(memory, registers, backgroundFifo, ppuClock);
        SpriteFetcher spriteFetcher = new SpriteFetcher(spriteBuffer, memory, registers, spriteFifo, ppuClock);

        // Create a no-op display
        Display display = (x, y, value) -> {};

        // Create scanline controller
        ScanlineController scanlineController = new ScanlineController(
            ppuClock, display, backgroundFifo, spriteFifo,
            pixelCombinator, registers, backgroundFetcher, spriteFetcher, spriteBuffer
        );

        // Create PPU
        ppu = new PictureProcessingUnit(
            scanlineController, registers, ppuClock,
            oamScanController, displayInterruptController, display
        );
    }

    @Test
    void oneScanline_shouldTakeExactly456TCycles() {
        // Given: LY starts at 0
        assertThat(registers.read(LY)).isEqualTo((byte) 0);

        // When: Run PPU for exactly 456 T-cycles (one scanline)
        for (int i = 0; i < SCANLINE_T_CYCLES; i++) {
            ppu.tCycle();
        }

        // Then: LY should have incremented to 1
        assertThat(registers.read(LY))
            .withFailMessage("After %d T-cycles, LY should be 1 but was %d",
                SCANLINE_T_CYCLES, registers.read(LY))
            .isEqualTo((byte) 1);
    }

    @Test
    void twoScanlines_shouldTakeExactly912TCycles() {
        // Given: LY starts at 0
        assertThat(registers.read(LY)).isEqualTo((byte) 0);

        // When: Run PPU for exactly 912 T-cycles (two scanlines)
        for (int i = 0; i < SCANLINE_T_CYCLES * 2; i++) {
            ppu.tCycle();
        }

        // Then: LY should have incremented to 2
        assertThat(registers.read(LY))
            .withFailMessage("After %d T-cycles, LY should be 2 but was %d",
                SCANLINE_T_CYCLES * 2, registers.read(LY))
            .isEqualTo((byte) 2);
    }

    @Test
    void lyIncrements_atCorrectIntervals() {
        // Track when LY changes
        int[] lyChangeAtCycle = new int[10];
        int lyChangeCount = 0;
        byte lastLy = registers.read(LY);

        // Run for several scanlines worth of T-cycles
        int totalCycles = SCANLINE_T_CYCLES * 5;
        for (int cycle = 0; cycle < totalCycles; cycle++) {
            ppu.tCycle();

            byte currentLy = registers.read(LY);
            if (currentLy != lastLy && lyChangeCount < lyChangeAtCycle.length) {
                lyChangeAtCycle[lyChangeCount++] = cycle + 1; // +1 because we're after the tCycle
                lastLy = currentLy;
            }
        }

        // Verify LY changed at the expected intervals (456, 912, 1368, 1824, 2280)
        assertThat(lyChangeCount).isGreaterThanOrEqualTo(5);

        for (int i = 0; i < 5; i++) {
            int expectedCycle = SCANLINE_T_CYCLES * (i + 1);
            assertThat(lyChangeAtCycle[i])
                .withFailMessage("LY change %d occurred at cycle %d, expected cycle %d",
                    i + 1, lyChangeAtCycle[i], expectedCycle)
                .isEqualTo(expectedCycle);
        }
    }

    @Test
    void fullFrame_shouldTake70224TCycles() {
        // A full frame is 154 scanlines * 456 T-cycles = 70224 T-cycles
        int frameTCycles = FRAME_SCANLINES * SCANLINE_T_CYCLES;

        // Given: LY starts at 0
        assertThat(registers.read(LY)).isEqualTo((byte) 0);

        // When: Run PPU for exactly one frame
        for (int i = 0; i < frameTCycles; i++) {
            ppu.tCycle();
        }

        // Then: LY should wrap back to 0 after a full frame
        assertThat(registers.read(LY))
            .withFailMessage("After %d T-cycles (one frame), LY should be 0 but was %d",
                frameTCycles, registers.read(LY))
            .isEqualTo((byte) 0);
    }

    @Test
    void oamScanPhase_shouldTake80TCycles() {
        // The OAM scan phase should take exactly 80 T-cycles
        // After 80 T-cycles, the PPU transitions to Drawing mode (mode 3)
        // Note: The mode change occurs at the START of cycle 81

        // Given: LCD is enabled, starting fresh
        assertThat(registers.read(LY)).isEqualTo((byte) 0);

        // Run for 80 T-cycles - completes OAM scan, but mode change happens next cycle
        for (int i = 0; i < 80; i++) {
            ppu.tCycle();
        }

        byte statBefore = registers.read(STAT);
        int modeBefore = statBefore & 0x03;

        // Run one more T-cycle - this is when Drawing mode begins
        ppu.tCycle();

        byte statAfter = registers.read(STAT);
        int modeAfter = statAfter & 0x03;

        // Mode should have transitioned from 2 (OAM scan) to 3 (Drawing)
        assertThat(modeBefore)
            .withFailMessage("After 80 T-cycles, mode should be 2 (OAM scan) but was %d", modeBefore)
            .isEqualTo(2);
        assertThat(modeAfter)
            .withFailMessage("After 81 T-cycles, mode should be 3 (Drawing) but was %d", modeAfter)
            .isEqualTo(3);
    }
}
