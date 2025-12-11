# Test Analysis: hblank_ly_scx_timing-GS

## Test Overview

This test verifies that the timing between the STAT mode=0 (HBLANK) interrupt and the LY register increment is affected by the SCX (scroll X) register value. Specifically, it tests how the lower 3 bits of SCX (SCX mod 8) affect when LY increments relative to the HBLANK interrupt trigger.

The test is designed for DMG/MGB/SGB/SGB2 hardware and is expected to fail on CGB/AGB/AGS.

## What The Test Does

### Setup Phase (Lines 45-52)
1. Sets stack pointer to DEFAULT_SP
2. Waits for VBLANK
3. Loads HL register with address of LY register (0xFF44)
4. Writes 0x08 to STAT register (enables HBLANK interrupt: bit 3)
5. Writes INTR_STAT to IE register (enables STAT interrupts globally)

### Test Iteration Pattern
For each test, the test does the following:

1. **setup_and_wait** (lines 122-132):
   - Waits for VBLANK
   - Waits for LY to equal scanline-1 (register D)
   - Clears interrupt flags
   - Enables interrupts with `ei`
   - Executes `halt` which pauses until HBLANK interrupt fires
   - After interrupt, continues with `nop` and `jp fail_halt`

2. **Interrupt Handler** (lines 137-139):
   - Located at INTR_VEC_STAT (0x48)
   - Executes `add sp,+2` to skip the return address (avoiding the `jp fail_halt`)
   - Returns directly to the test code after the halt/nop

3. **Timing Measurement** (lines 65-77):
   - After returning from interrupt handler, precise cycle counting begins:
     - Interrupt processing: 5 cycles
     - Interrupt vector execution: 4 (call) + 4 (add sp) = 8 cycles
     - Call to standard_delay: 6 cycles
     - standard_delay itself: 23 nops + 4 (ret) = 27 cycles
     - Variable delay: N nops
     - `ld a, (hl)` instruction: 1 cycle for decode before memory read
   - Total: 5 + 8 + 6 + 27 + N + 1 = 47 + N cycles after HBLANK interrupt
   - The memory read of LY happens at exactly 48 + N cycles after the HBLANK interrupt

4. **Verification**:
   - First iteration checks that LY still equals scanline-1 (D register)
   - Second iteration checks that LY now equals scanline (E register)
   - This determines the exact cycle when LY increments

### Test Cases (Lines 79-104)

The test performs iterations with different SCX values:

**SCX = 0 (default):**
- Tests scanlines 0x42 and 0x43
- delay_a=2, delay_b=3
- LY should increment at 49-50 cycles (changes between 49 and 50)
- Expected: 51 cycles after STAT interrupt

**SCX = 1, 2, 3, 4:**
- Tests scanlines 0x42 and 0x43 for each
- delay_a=1, delay_b=2
- LY should increment at 48-49 cycles
- Expected: 50 cycles after STAT interrupt

**SCX = 5, 6, 7:**
- Tests scanlines 0x42 and 0x43 for each
- delay_a=0, delay_b=1
- LY should increment at 47-48 cycles
- Expected: 49 cycles after STAT interrupt

**SCX = 8:**
- Tests scanlines 0x42 and 0x43
- delay_a=2, delay_b=3 (same as SCX=0)
- Expected: 51 cycles after STAT interrupt (pattern repeats)

## What The Test Expects

The test expects the following timing relationship between the HBLANK STAT interrupt and LY increment:

| SCX mod 8 | Cycles from STAT interrupt to LY increment |
|-----------|---------------------------------------------|
| 0         | 51 cycles                                   |
| 1-4       | 50 cycles                                   |
| 5-7       | 49 cycles                                   |

This pattern is explicitly documented in the test comments (lines 24-27).

The test performs binary search timing measurements using two consecutive delay values:
- If LY hasn't incremented at 47+delay_a cycles, test fails
- If LY has incremented at 47+delay_b cycles, test fails
- Success means LY increments between these two precise timings

## What The Test Is Testing

This test validates the **timing relationship between SCX scrolling and the PPU's HBLANK/LY increment behavior**.

### Core Behavior Being Tested

1. **HBLANK Interrupt Timing**: The STAT interrupt should fire when the PPU enters HBLANK mode (mode 0)

2. **LY Increment Timing**: The LY register should increment at the END of the scanline (when count reaches 456 cycles)

3. **SCX-Dependent HBLANK Duration**: The key insight is that SCX affects when HBLANK mode begins:
   - The PPU must render 160 pixels to the screen
   - SCX causes pixel discarding at the start of scanline rendering
   - When `SCX mod 8` is non-zero, the fetcher must discard (SCX mod 8) pixels
   - Pixel discarding takes 1 cycle per pixel
   - This delays when the 160th pixel is rendered
   - HBLANK starts after the 160th pixel
   - Therefore, higher (SCX mod 8) means HBLANK starts later in the scanline
   - Later HBLANK start means fewer cycles until the 456-cycle scanline completes
   - Fewer cycles between HBLANK interrupt and LY increment

### Why The Timing Changes

The test reveals that:
- **SCX mod 8 = 0**: No pixels to discard, HBLANK starts earliest, 51 cycles until LY increment
- **SCX mod 8 = 1-4**: 1-4 pixels to discard, HBLANK starts 1-4 cycles later, 50 cycles until LY increment
- **SCX mod 8 = 5-7**: 5-7 pixels to discard, HBLANK starts 5-7 cycles later, 49 cycles until LY increment

However, the relationship is not perfectly linear because the pixel fetcher operates in 8-cycle chunks and there are other timing subtleties in the PPU pipeline.

## Potential Failure Reasons

### Issue 1: HBLANK Timing Independent of SCX

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java` (lines 92-102)

The current implementation transitions to HBLANK immediately when `scanlineController.drawingComplete()` returns true. The timing to reach this state is determined by the ScanlineController's pixel rendering.

**In ScanlineController.java** (lines 156-160):
```java
private boolean shouldDiscardPixel() {
    int pixelsToDiscard = mod(registers.read(SCX), 8);
    int discardedPixels = (8 - backgroundFifo.size());
    return discardedPixels % 8 != pixelsToDiscard;
}
```

The pixel discard logic correctly uses `SCX mod 8`, but let's trace the timing:

### Issue 2: HBLANK to LY Increment Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java` (lines 104-122)

```java
private Step hblank() {
    clock.tick();
    count++;

    if (count < SCANLINE_TICK_COUNT) {
        return Step.HBLANK;
    }

    updateLY((byte) (registers.read(LY) + 1));
    // ...
}
```

The current implementation:
1. HBLANK interrupt is triggered at the START of HBLANK (line 100: `displayInterruptController.sendHblank()`)
2. The `count` variable tracks total cycles for the entire scanline (OAM + Drawing + HBLANK)
3. LY increments when `count >= SCANLINE_TICK_COUNT` (456 cycles)

**Problem**: The test expects the timing from HBLANK interrupt to LY increment to vary based on SCX:
- The `count` variable already includes all the cycles from OAM scan (80 cycles) + Drawing (variable based on SCX)
- When SCX causes more cycles in the Drawing phase, HBLANK should be shorter
- However, the code waits until `count < 456` regardless

**Analysis**:
- OAM scan always takes 80 cycles (40*2 at line 82)
- Drawing phase takes variable time based on SCX and sprite fetching
- When Drawing takes longer (higher SCX mod 8), less time remains until cycle 456
- The HBLANK interrupt fires when Drawing completes
- LY increments at cycle 456

**Expected behavior**:
- SCX mod 8 = 0: Drawing completes around cycle 405, HBLANK interrupt, 51 cycles until LY increment at 456
- SCX mod 8 = 5: Drawing completes around cycle 407, HBLANK interrupt, 49 cycles until LY increment at 456

### Issue 3: Pixel Discard Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/ScanlineController.java` (lines 80-87)

```java
private State discardPixel() {
    backgroundFetcher.runSingleTickCycle();
    backgroundFifo.read();
    ppuClock.tick();
    return shouldDiscardPixel() ? State.DISCARD_PIXELS : State.PIXEL_FETCHING;
}
```

Each discarded pixel:
1. Runs one fetcher tick cycle
2. Reads (discards) one pixel from FIFO
3. Ticks the clock (1 cycle)

**Analysis**: The discard logic takes 1 cycle per discarded pixel, which is correct. However, we need to verify that:
1. The fetcher properly fills the FIFO with 8 pixels before discarding begins
2. The discard count correctly matches `SCX mod 8`
3. The timing properly affects when Drawing completes

### Issue 4: Verification of Drawing Completion Time

The key question is: **When does `scanlineController.drawingComplete()` return true relative to SCX?**

Looking at `ScanlineController.java` (lines 61-63):
```java
public boolean drawingComplete() {
    return state == State.COMPLETE;
}
```

And the `executeFetch()` method (lines 89-101):
```java
private State executeFetch() {
    if (LX >= DISPLAY_WIDTH) {
        return nop();  // Sets state to COMPLETE
    }
    // ...
}
```

So Drawing completes when `LX >= 160` (DISPLAY_WIDTH).

The question is: Does the time to reach `LX == 160` vary correctly with SCX?

**Trace**:
1. Start of scanline: `LX = 0`, `backgroundFifo` is empty
2. Enter `DISCARD_PIXELS` state if `shouldDiscardPixel()` is true
3. Discard `SCX mod 8` pixels (takes `SCX mod 8` cycles)
4. Enter `PIXEL_FETCHING` state
5. Push pixels to screen until `LX >= 160`
6. Each pixel push takes 1 cycle (via `pushPixel()` which calls `ppuClock.tick()`)

**Expected Drawing phase cycles**:
- Initial FIFO filling: ~12 cycles (fetcher needs to fill the FIFO with first 8 pixels)
- Pixel discarding: `SCX mod 8` cycles
- Pixel rendering: 160 cycles (one per pixel)
- Fetcher overhead: Additional cycles for fetching new tiles

The total Drawing time should increase with `SCX mod 8`, which would decrease HBLANK duration.

### Root Cause Hypothesis

The emulator likely implements the basic timing correctly (SCX affects Drawing duration, which affects HBLANK duration), but there may be subtle cycle-counting inaccuracies:

1. **Fetcher timing**: The fetcher may not accurately model the 8-cycle tile fetch pattern
2. **Pixel discard timing**: The relationship between FIFO state and discard cycles might have off-by-one errors
3. **HBLANK entry timing**: The exact cycle when HBLANK interrupt fires relative to when the last pixel is pushed might be off

**Most Likely Issue**: The test expects very precise 1-cycle differences based on `SCX mod 8`. The categories (0, 1-4, 5-7) suggest there's a non-linear relationship where multiple SCX values map to the same timing. This could indicate:
- The fetcher's 8-cycle chunks create step functions in timing
- The FIFO state at the end of pixel discarding affects when fetching resumes
- There may be additional cycles for FIFO operations that aren't perfectly modeled

To fully diagnose, one would need to:
1. Log the exact cycle when `sendHblank()` is called
2. Log the exact cycle when `updateLY()` is called
3. Verify the difference matches the expected values (51/50/49) for each SCX value
4. Compare against the test's expected behavior with cycle-accurate debugging
