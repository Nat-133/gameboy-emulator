# Mooneye Test Analysis: vblank_stat_intr-GS

## Test Overview

This test validates the timing relationship between VBLANK interrupts and STAT Mode 2 (OAM) interrupts at the start of line 144. On DMG/MGB/SGB/SGB2 hardware, when STAT bit 5 (Mode 2 OAM interrupt enable) is set, a STAT interrupt is triggered at line 144 simultaneously with the VBLANK interrupt. The test measures the cycle timing between these interrupts to verify they occur at exactly the same time.

**Expected to pass on:** DMG, MGB, SGB, SGB2
**Expected to fail on:** CGB, AGB, AGS (Color Game Boy models)

## What The Test Does

The test performs 4 timing measurement rounds to compare interrupt latencies:

### Round 1: VBLANK baseline (54 nops)
1. Wait until LY=143
2. Execute 54 NOPs for precise timing alignment
3. Reset DIV register to 0
4. HALT waiting for VBLANK interrupt
5. When VBLANK fires, read DIV value and store in `round1`

### Round 2: VBLANK baseline (55 nops)
1. Wait until LY=143
2. Execute 55 NOPs (one more than Round 1)
3. Reset DIV register to 0
4. HALT waiting for VBLANK interrupt
5. When VBLANK fires, read DIV value and store in `round2`

### Round 3: STAT Mode 2 interrupt (54 nops, STAT bit 5 enabled)
1. Enable STAT bit 5 (Mode 2 OAM interrupt) by writing 0x20 to STAT
2. Wait until LY=143
3. Execute 54 NOPs (same as Round 1)
4. Reset DIV register to 0
5. HALT waiting for STAT interrupt (not VBLANK)
6. When STAT fires, read DIV value and store in `round3`

### Round 4: STAT Mode 2 interrupt (55 nops, STAT bit 5 enabled)
1. STAT bit 5 still enabled from Round 3
2. Wait until LY=143
3. Execute 55 NOPs (same as Round 2)
4. Reset DIV register to 0
5. HALT waiting for STAT interrupt
6. When STAT fires, read DIV value and store in `round4`

## What The Test Expects

After all 4 rounds complete, the test checks these register values:

```
B (round1) = 0x01
C (round2) = 0x00
D (round3) = 0x01
E (round4) = 0x00
```

### Timing Analysis

The DIV register increments at 16384 Hz (every 256 CPU cycles). The difference between round1 and round2:
- Round1 (54 NOPs): DIV = 0x01 when interrupt fires
- Round2 (55 NOPs): DIV = 0x00 when interrupt fires

This indicates there's a critical timing window where adding just 1 NOP (4 cycles) changes which DIV increment has occurred by the time the interrupt handler executes.

The key expectation: **Round 3 should match Round 1, and Round 4 should match Round 2**. This proves that:
1. The STAT Mode 2 interrupt at line 144 fires at the exact same time as VBLANK
2. The interrupt latency is identical between VBLANK and STAT interrupts
3. Both interrupts are triggered simultaneously on the same scanline transition

## What The Test Is Testing

This test validates a specific hardware behavior on DMG-era Game Boys:

1. **STAT Mode 2 interrupt at line 144**: When entering line 144 (start of VBLANK period), the PPU mode changes from Mode 0 (HBLANK) to Mode 1 (VBLANK). However, if STAT bit 5 (Mode 2 OAM interrupt enable) is set, a STAT interrupt is also triggered.

2. **Simultaneous interrupt timing**: The test verifies that both VBLANK and the STAT Mode 2 interrupt are triggered at exactly the same cycle when transitioning to line 144.

3. **Hardware quirk**: On DMG/MGB/SGB/SGB2, when entering line 144:
   - VBLANK interrupt is triggered
   - If STAT bit 5 is enabled, STAT interrupt is also triggered
   - Both happen at the same instant (not sequentially)
   - This is because the hardware treats the start of line 144 as both entering VBLANK mode AND as the beginning of a new scanline (which would normally start with Mode 2/OAM scan)

4. **CGB difference**: On CGB and later models, this simultaneous triggering behavior changed, which is why the test is marked to fail on those systems.

## Potential Failure Reasons

### Issue 1: Mode Transition Timing at Line 144

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java`

**Lines 104-122 (hblank method):**

```java
private Step hblank() {
    clock.tick();
    count++;

    if (count < SCANLINE_TICK_COUNT) {
        return Step.HBLANK;
    }

    updateLY((byte) (registers.read(LY) + 1));

    if (uint(registers.read(LY)) >= Display.DISPLAY_HEIGHT) {
        count = 0;
        displayInterruptController.sendVblank();
        display.onVBlank();
        return Step.VBLANK;
    }

    return Step.OAM_SETUP;
}
```

**Problem:** When transitioning from line 143 to line 144:
1. LY is incremented to 144
2. `sendVblank()` is called, which sets STAT mode to V_BLANK
3. The next step goes to Step.VBLANK, not Step.OAM_SETUP

**Expected DMG Behavior:** On line 144, even though we're entering VBLANK period, if STAT bit 5 (OAM interrupt enable) is set, an interrupt should fire as if we're starting a new scanline with Mode 2 (OAM scanning). This is the quirk the test is checking.

### Issue 2: STAT Interrupt Logic at Line 144

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/DisplayInterruptController.java`

**Lines 70-87 (sendVblank method):**

```java
public void sendVblank() {
    registers.setStatMode(StatParser.PpuMode.V_BLANK);

    interruptController.setInterrupt(Interrupt.VBLANK);

    byte stat = registers.read(PpuRegister.STAT);
    // VBlank STAT interrupt can be triggered by either:
    // - Bit 4: VBlank mode interrupt enable
    // - Bit 5: OAM mode interrupt enable (triggers at start of ANY scanline, including line 144)
    boolean vblankStatEnabled = StatParser.vblankStatInterruptEnabled(stat);
    boolean oamStatEnabled = StatParser.oamInterruptEnabled(stat);

    activeModeLine = (vblankStatEnabled || oamStatEnabled)
            ? Optional.of(ActiveModeLine.VBLANK)
            : Optional.empty();

    checkAndTriggerStatInterrupt();
}
```

**Current Behavior:** The code already attempts to handle this quirk by checking both `vblankStatEnabled` (bit 4) and `oamStatEnabled` (bit 5) when entering VBLANK. If either is set, it sets `activeModeLine` to VBLANK and triggers a STAT interrupt.

**Potential Issue:** The comment correctly identifies the quirk, and the logic looks correct. However, there might be a timing issue with when this interrupt is triggered relative to the VBLANK interrupt.

### Issue 3: STAT Blocking and Rising Edge Detection

**Lines 130-138 (checkAndTriggerStatInterrupt):**

```java
private void checkAndTriggerStatInterrupt() {
    boolean newStatLine = activeModeLine.isPresent() || lycLine;

    if (newStatLine && !statLine) {  // this is for stat blocking, only trigger on rising edge
        interruptController.setInterrupt(Interrupt.STAT);
    }

    statLine = newStatLine;
}
```

**Potential Problem:** STAT blocking logic requires a rising edge (transition from low to high) to trigger an interrupt. If the test is measuring precise timing:

1. At the end of line 143, we're in HBLANK (Mode 0)
2. If STAT bit 3 (HBLANK interrupt) is enabled, `statLine` would be high
3. When transitioning to line 144, we call `sendVblank()`
4. If STAT bit 5 (OAM interrupt) is enabled, we want a STAT interrupt
5. But if `statLine` was already high from HBLANK, the rising edge check `!statLine` would be false
6. No STAT interrupt would fire even though bit 5 is enabled

**However:** Looking at the test code, it sets STAT to 0x20 (only bit 5), so HBLANK interrupt is NOT enabled. This means `statLine` should be low going into line 144, so the rising edge should work.

### Issue 4: Interrupt Timing Order

**Critical Question:** When both VBLANK and STAT interrupts should fire simultaneously at line 144, in what order are they set in the interrupt flag register?

Looking at `sendVblank()`:
1. Sets STAT mode to V_BLANK
2. Calls `interruptController.setInterrupt(Interrupt.VBLANK)`
3. Then checks STAT conditions and possibly calls `interruptController.setInterrupt(Interrupt.STAT)`

This means VBLANK interrupt flag is set first, then STAT. However, the test uses separate interrupt handlers and enables only one interrupt at a time (using `halt_until` macro), so the order shouldn't matter for the test's timing measurement.

### Issue 5: Mode Transition Not Matching Hardware

**Key Insight:** On DMG hardware, line 144 is special:
- It's the first line of the VBLANK period (lines 144-153)
- The PPU mode should be Mode 1 (VBLANK)
- BUT, if STAT bit 5 is set, the hardware acts as if it's also starting a Mode 2 (OAM scan) phase
- This is likely because the PPU's scanline state machine treats line 144 as the start of a new scanline, even though it won't actually draw anything

**Current Emulator Behavior:**
- When LY reaches 144, we immediately go to VBLANK mode
- We never enter OAM_SETUP or OAM_SCAN states
- This might not trigger the STAT Mode 2 interrupt at the exact right cycle

### Issue 6: Missing OAM Interrupt Signal at Line 144

**Root Cause Analysis:**

On real DMG hardware, when transitioning from line 143 to line 144:
1. The PPU completes HBLANK on line 143
2. At the end of the scanline (456 T-cycles), LY increments to 144
3. At this exact moment, TWO things happen simultaneously:
   - VBLANK period begins (mode becomes V_BLANK, mode bits = 01)
   - The scanline state machine starts a new scanline cycle (which would normally begin with OAM scan)
4. If STAT bit 5 (OAM interrupt enable) is set, a STAT interrupt fires because we're at the "start of scanline" point, even though we're in VBLANK mode
5. If STAT bit 4 (VBLANK interrupt enable) is set, a STAT interrupt also fires because mode changed to VBLANK

The emulator's current implementation:
- Sets mode to V_BLANK
- Checks if bit 4 OR bit 5 is enabled
- Triggers STAT interrupt if either is set

**This should be correct**, but the timing might be off by a few cycles compared to hardware.

### Issue 7: Exact Cycle Timing

The test is extremely sensitive to cycle-perfect timing. The difference between round1 (DIV=0x01) and round2 (DIV=0x00) shows that a single 4-cycle NOP instruction changes the result.

DIV increments every 256 CPU cycles. The values suggest:
- Round 1: Interrupt fires after ~256-511 cycles (DIV incremented once)
- Round 2: Interrupt fires after ~0-255 cycles (DIV hasn't incremented yet)

The test is measuring the precise cycle when the interrupt is acknowledged and the interrupt handler begins executing. Any cycle-level inaccuracy in:
- When LY increments
- When the mode changes
- When interrupts are flagged
- Interrupt dispatch latency

Could cause this test to fail.

## Summary of Most Likely Failure

The emulator's DisplayInterruptController.sendVblank() method appears to correctly implement the DMG quirk where STAT bit 5 (OAM interrupt enable) causes a STAT interrupt at line 144. However, the test may fail due to:

1. **Cycle-level timing precision**: The exact cycle when interrupts are set and when LY increments must match hardware behavior exactly. Even a 1-4 cycle difference would cause the test to fail given the sensitivity shown by the NOP timing.

2. **Interrupt dispatch order**: If there's any delay between setting the VBLANK interrupt and setting the STAT interrupt, the timing won't match hardware behavior of simultaneous triggering.

3. **STAT mode timing**: The mode bits in STAT register must change to V_BLANK at the exact cycle that matches hardware, as this affects when the STAT interrupt condition is evaluated.

The test passing on DMG but failing on CGB suggests this is a specific hardware quirk that changed between Game Boy models. The emulator needs to ensure cycle-perfect accuracy in the line 144 transition to pass this test.
