package org.gameboy.display;

import org.gameboy.common.IntBackedRegister;
import org.gameboy.common.Interrupt;
import org.gameboy.common.InterruptController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.gameboy.display.PpuRegisters.PpuRegister.*;
import static org.gameboy.utils.BitUtilities.get_bit;
import static org.mockito.Mockito.*;

class DisplayInterruptControllerTest {
    private InterruptController interruptController;
    private PpuRegisters ppuRegisters;
    private DisplayInterruptController controller;

    @BeforeEach
    void setUp() {
        interruptController = Mockito.mock(InterruptController.class);
        ppuRegisters = new PpuRegisters(
            new IntBackedRegister(),  // LY
            new IntBackedRegister(),  // LYC
            new IntBackedRegister(),  // SCX
            new IntBackedRegister(),  // SCY
            new IntBackedRegister(),  // WX
            new IntBackedRegister(),  // WY
            new IntBackedRegister(0x91),  // LCDC (LCD enabled by default)
            new StatRegister(0x80),  // STAT - uses StatRegister for proper read-only bit handling
            new IntBackedRegister(0xFC),  // BGP
            new IntBackedRegister(0xFF),  // OBP0
            new IntBackedRegister(0xFF)   // OBP1
        );
        controller = new DisplayInterruptController(interruptController, ppuRegisters);
    }

    @Nested
    class LcdDisabledBehaviorTests {
        /**
         * Test for stat_lyc_onoff: When LCD is disabled, LYC writes should NOT
         * update the coincidence flag or trigger LYC interrupts.
         */
        @Test
        void checkAndSendLyCoincidence_whenLcdDisabled_doesNotUpdateCoincidenceFlag() {
            // Setup: LY=144 (0x90), LYC=144, coincidence flag set
            ppuRegisters.write(LY, (byte) 0x90);
            ppuRegisters.write(LYC, (byte) 0x90);
            ppuRegisters.write(STAT, (byte) 0x40);  // LYC interrupt enabled

            // First, verify coincidence with LCD enabled
            ppuRegisters.write(LCDC, (byte) 0x91);  // LCD enabled
            controller.checkAndSendLyCoincidence();

            // Coincidence flag should be set (bit 2)
            byte stat = ppuRegisters.read(STAT);
            assert get_bit(stat, 2) : "Expected coincidence flag to be set";
            reset(interruptController);

            // Now disable LCD
            ppuRegisters.write(LCDC, (byte) 0x00);

            // Change LYC to a non-matching value
            ppuRegisters.write(LYC, (byte) 0x01);

            // Check LYC while LCD is disabled
            controller.checkAndSendLyCoincidence();

            // Coincidence flag should STILL be set (retained from before)
            stat = ppuRegisters.read(STAT);
            assert get_bit(stat, 2) : "Expected coincidence flag to be retained when LCD disabled";

            // No interrupt should fire when LCD is disabled
            verify(interruptController, never()).setInterrupt(Interrupt.STAT);
        }

        @Test
        void checkAndSendLyCoincidence_whenLcdDisabled_retainsCoincidenceFlagAsFalse() {
            // Setup: LY=144 (0x90), LYC=0 (different), coincidence flag NOT set
            ppuRegisters.write(LY, (byte) 0x90);
            ppuRegisters.write(LYC, (byte) 0x00);
            ppuRegisters.write(STAT, (byte) 0x40);  // LYC interrupt enabled

            // First, verify non-coincidence with LCD enabled
            ppuRegisters.write(LCDC, (byte) 0x91);  // LCD enabled
            controller.checkAndSendLyCoincidence();

            // Coincidence flag should NOT be set (bit 2)
            byte stat = ppuRegisters.read(STAT);
            assert !get_bit(stat, 2) : "Expected coincidence flag to be clear";
            reset(interruptController);

            // Now disable LCD
            ppuRegisters.write(LCDC, (byte) 0x00);

            // Change LYC to a matching value (would normally set coincidence)
            ppuRegisters.write(LYC, (byte) 0x90);

            // Check LYC while LCD is disabled
            controller.checkAndSendLyCoincidence();

            // Coincidence flag should STILL be clear (retained from before)
            stat = ppuRegisters.read(STAT);
            assert !get_bit(stat, 2) : "Expected coincidence flag to stay clear when LCD disabled";

            // No interrupt should fire when LCD is disabled
            verify(interruptController, never()).setInterrupt(Interrupt.STAT);
        }
    }

    @Nested
    class VblankStatInterruptTests {
        /**
         * Test for vblank_stat_intr-GS: When OAM interrupt (bit 5) is enabled,
         * a STAT interrupt should fire when entering VBlank at line 144.
         * This is because the OAM interrupt triggers at the start of ANY scanline,
         * including the VBlank period.
         */
        @Test
        void sendVblank_withOamInterruptEnabled_triggersStatInterrupt() {
            // STAT bit 5 = OAM interrupt enable (Mode 2)
            ppuRegisters.write(STAT, (byte) 0x20);

            controller.sendVblank();

            verify(interruptController).setInterrupt(Interrupt.VBLANK);
            verify(interruptController).setInterrupt(Interrupt.STAT);
        }

        @Test
        void sendVblank_withVblankStatInterruptEnabled_triggersStatInterrupt() {
            // STAT bit 4 = VBlank STAT interrupt enable (Mode 1)
            ppuRegisters.write(STAT, (byte) 0x10);

            controller.sendVblank();

            verify(interruptController).setInterrupt(Interrupt.VBLANK);
            verify(interruptController).setInterrupt(Interrupt.STAT);
        }

        @Test
        void sendVblank_withNoStatInterruptEnabled_doesNotTriggerStatInterrupt() {
            // No STAT interrupt bits enabled
            ppuRegisters.write(STAT, (byte) 0x00);

            controller.sendVblank();

            verify(interruptController).setInterrupt(Interrupt.VBLANK);
            verify(interruptController, never()).setInterrupt(Interrupt.STAT);
        }
    }

    @Nested
    class StatWriteTests {
        /**
         * Test for stat_irq_blocking round 1: When we are already in VBlank mode
         * and then WRITE to STAT to enable the mode 1 interrupt, a STAT interrupt
         * should be triggered on the rising edge.
         */
        @Test
        void checkStatCondition_whenInVblankAndVblankInterruptNewlyEnabled_triggersStatInterrupt() {
            // First, enter VBlank with no STAT interrupts enabled
            ppuRegisters.write(STAT, (byte) 0x00);
            controller.sendVblank();

            verify(interruptController).setInterrupt(Interrupt.VBLANK);
            verify(interruptController, never()).setInterrupt(Interrupt.STAT);
            reset(interruptController);

            // Now enable mode 1 interrupt while still in VBlank
            // This simulates writing $10 to STAT register
            ppuRegisters.write(STAT, (byte) 0x11);  // 0x11 = mode 1 enabled + vblank mode bits
            controller.checkStatCondition();

            verify(interruptController).setInterrupt(Interrupt.STAT);
        }

        @Test
        void checkStatCondition_whenInOamScanAndOamInterruptNewlyEnabled_triggersStatInterrupt() {
            // First, enter OAM scan with no STAT interrupts enabled
            ppuRegisters.write(STAT, (byte) 0x00);
            controller.sendOamScan();

            verify(interruptController, never()).setInterrupt(Interrupt.STAT);
            reset(interruptController);

            // Now enable OAM interrupt while still in OAM mode
            ppuRegisters.write(STAT, (byte) 0x22);  // 0x22 = OAM enabled + OAM mode bits
            controller.checkStatCondition();

            verify(interruptController).setInterrupt(Interrupt.STAT);
        }
    }

    @Nested
    class StatIrqBlockingTests {
        /**
         * Test for stat_irq_blocking: Mode 3 (drawing) should clear the activeModeLine,
         * allowing the STAT IRQ line to go low if no other conditions are keeping it high.
         * This is important because Mode 3 is the only mode that can clear the internal
         * STAT IRQ signal when all other modes have interrupts enabled.
         */
        @Test
        void sendDrawing_clearsActiveModeLine_allowingNewInterruptOnNextModeChange() {
            // Enable OAM interrupt (bit 5) and enter OAM scan
            ppuRegisters.write(STAT, (byte) 0x20);
            controller.sendOamScan();

            // STAT interrupt should have fired
            verify(interruptController).setInterrupt(Interrupt.STAT);
            reset(interruptController);

            // Enter Drawing mode - this should clear the active mode line
            controller.sendDrawing();

            // Now entering OAM scan again should trigger another interrupt
            // because the line went low during Mode 3
            controller.sendOamScan();

            verify(interruptController).setInterrupt(Interrupt.STAT);
        }

        @Test
        void sendDrawing_withLycMatchStillActive_doesNotTriggerNewInterrupt() {
            // Set LY=LYC=0 and enable LYC interrupt (bit 6)
            ppuRegisters.write(LY, (byte) 0);
            ppuRegisters.write(LYC, (byte) 0);
            ppuRegisters.write(STAT, (byte) 0x40);  // LYC interrupt enabled

            controller.checkAndSendLyCoincidence();
            verify(interruptController).setInterrupt(Interrupt.STAT);
            reset(interruptController);

            // Enable OAM interrupt too
            ppuRegisters.write(STAT, (byte) 0x60);  // LYC + OAM interrupt enabled

            // Enter OAM scan - no new interrupt because LYC line is still high
            controller.sendOamScan();
            verify(interruptController, never()).setInterrupt(Interrupt.STAT);

            // Enter Drawing - mode line goes low but LYC line keeps STAT line high
            controller.sendDrawing();

            // Enter OAM again - still no interrupt because LYC line never went low
            controller.sendOamScan();
            verify(interruptController, never()).setInterrupt(Interrupt.STAT);
        }
    }
}
