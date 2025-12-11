# Mooneye Test Analysis: lcdon_timing-GS

## Test Overview

The `lcdon_timing-GS` test validates the exact timing behavior when the LCD is turned on by setting bit 7 of the LCDC register. It specifically tests that when the PPU is enabled, line 0 starts with mode 0 (H-Blank) and then transitions directly to mode 3 (Drawing), and that line 0 has different timings because the PPU is delayed by 2 T-cycles compared to normal operation.

This test is designed for DMG (original Game Boy) hardware and passes on DMG, MGB, SGB, and SGB2, but fails on CGB and later models which have different LCD enable timing behavior.

## What The Test Does

The test performs the following steps:

1. **Setup Phase:**
   - Disables the PPU safely (waits for VBlank, then clears bit 7 of LCDC)
   - Clears VRAM and OAM memory

2. **Five Separate Test Passes:**

   Each test pass follows this pattern:
   - Writes $81 to LCDC (enables LCD: bit 7=1, enables BG: bit 0=1)
   - Optionally delays by 0, 1, or 2 T-cycles (nops)
   - Reads a specific memory location (LY, STAT, OAM, or VRAM) at 8 precise cycle counts after LCD enable
   - Stores the 8 read values in HRAM
   - Disables the PPU safely

   The 8 cycle counts where reads occur are: **0, 17, 60, 110, 130, 174, 224, 244** T-cycles after LCDC write.

3. **Test Pass Details:**

   **Test Pass 1 (cycle offset 0):**
   - Enable LCD
   - Read at cycles: 0, 17, 60, 110, 130, 174, 224, 244

   **Test Pass 2 (cycle offset 1):**
   - Enable LCD
   - Wait 1 T-cycle (nop)
   - Read at cycles: 1, 18, 61, 111, 131, 175, 225, 245

   **Test Pass 3 (cycle offset 2):**
   - Enable LCD
   - Wait 2 T-cycles (nops 2)
   - Read at cycles: 2, 19, 62, 112, 132, 176, 226, 246

4. **Five Test Categories:**

   a. **LY Test:** Reads LY register at all timing points

   b. **STAT Test with LYC=0:** Reads STAT register with LYC set to 0

   c. **STAT Test with LYC=1:** Reads STAT register with LYC set to 1

   d. **OAM Access Test:** Reads OAM memory ($FE00) to check if it's accessible

   e. **VRAM Access Test:** Reads VRAM memory ($8000) to check if it's accessible

5. **Verification:**
   - Each test compares the 24 read values (3 passes x 8 reads) against expected values
   - Any mismatch results in a detailed error report showing:
     - Which test failed (LY, STAT LYC=0, STAT LYC=1, OAM access, or VRAM access)
     - The specific cycle count where the mismatch occurred
     - Expected vs actual values

## What The Test Expects

### LY Register Values:
```
Pass 1 (offset 0):  $00 $00 $00 $00 $01 $01 $01 $02
Pass 2 (offset 1):  $00 $00 $00 $01 $01 $01 $02 $02
Pass 3 (offset 2):  $00 $00 $00 $01 $01 $01 $02 $02
```

**Interpretation:**
- LY should be 0 for the first ~110 T-cycles after LCD enable
- Then increment to 1, then to 2 as scanlines progress
- The exact transition points vary slightly based on when LCD was enabled within a T-cycle

### STAT Register Values with LYC=0:
```
Pass 1 (offset 0):  $84 $84 $87 $84 $82 $83 $80 $82
Pass 2 (offset 1):  $84 $87 $84 $80 $82 $80 $80 $82
Pass 3 (offset 2):  $84 $87 $84 $82 $83 $80 $82 $83
```

**Binary breakdown (STAT register format: `[unused][LYC_EN][OAM_EN][VBL_EN][HBL_EN][LYC_FLAG][MODE1][MODE0]`):**
- $84 = 10000100 = Mode 0 (H-Blank), LYC flag set
- $87 = 10000111 = Mode 3 (Drawing), LYC flag set
- $82 = 10000010 = Mode 2 (OAM Scan), no LYC flag
- $83 = 10000011 = Mode 3 (Drawing), no LYC flag
- $80 = 10000000 = Mode 0 (H-Blank), no LYC flag

**Key insight:** Line 0 starts in mode 0 (H-Blank) with LYC coincidence flag set, then transitions to mode 3 (Drawing), skipping mode 2 (OAM Scan).

### STAT Register Values with LYC=1:
```
Pass 1 (offset 0):  $80 $80 $83 $80 $86 $87 $84 $82
Pass 2 (offset 1):  $80 $83 $80 $80 $86 $84 $80 $82
Pass 3 (offset 2):  $80 $83 $80 $86 $87 $84 $82 $83
```

**Binary breakdown:**
- $80 = 10000000 = Mode 0 (H-Blank), no LYC flag
- $83 = 10000011 = Mode 3 (Drawing), no LYC flag
- $86 = 10000110 = Mode 2 (OAM Scan), LYC flag set
- $87 = 10000111 = Mode 3 (Drawing), LYC flag set
- $84 = 10000100 = Mode 0 (H-Blank), LYC flag set
- $82 = 10000010 = Mode 2 (OAM Scan), no LYC flag

**Key insight:** When LYC=1, the coincidence flag is clear during line 0, then gets set when LY increments to 1.

### OAM Access ($FE00):
```
Pass 1 (offset 0):  $00 $00 $FF $00 $FF $FF $00 $FF
Pass 2 (offset 1):  $00 $FF $00 $FF $FF $00 $FF $FF
Pass 3 (offset 2):  $00 $FF $00 $FF $FF $00 $FF $FF
```

**Interpretation:**
- $00 = OAM is accessible (returns actual memory value)
- $FF = OAM is blocked (PPU is using it)

OAM should be accessible during mode 0 (H-Blank) and blocked during modes 2 (OAM Scan) and 3 (Drawing).

### VRAM Access ($8000):
```
Pass 1 (offset 0):  $00 $00 $FF $00 $00 $FF $00 $00
Pass 2 (offset 1):  $00 $FF $00 $00 $FF $00 $00 $FF
Pass 3 (offset 2):  $00 $FF $00 $00 $FF $00 $00 $FF
```

**Interpretation:**
- $00 = VRAM is accessible (returns actual memory value)
- $FF = VRAM is blocked (PPU is using it)

VRAM should only be blocked during mode 3 (Drawing) and accessible during all other modes.

## What The Test Is Testing

This test validates several critical aspects of Game Boy PPU behavior when the LCD is turned on:

1. **LCD Enable Timing:** The PPU should start exactly when bit 7 of LCDC is set, with a specific 2 T-cycle delay.

2. **Line 0 Special Behavior:** When the LCD is turned on:
   - Line 0 (LY=0) starts in mode 0 (H-Blank) instead of mode 2 (OAM Scan)
   - Mode 0 lasts only briefly before transitioning directly to mode 3 (Drawing)
   - This is a special case unique to LCD enable - subsequent lines follow normal timing

3. **Mode Transitions:** The test verifies the exact T-cycle timing of:
   - Mode 0 (H-Blank) at LCD enable
   - Transition to Mode 3 (Drawing) around cycle 17-60
   - Mode transitions for subsequent scanlines (lines 1 and 2)

4. **LY Increment Timing:** The exact T-cycle when LY increments from 0→1→2.

5. **LYC Coincidence Flag:** The coincidence flag in STAT bit 2 should:
   - Be set immediately when LCD is enabled if LY=LYC
   - Update correctly as LY increments
   - Be computed and set at precise timing

6. **OAM Access Restrictions:** CPU reads to OAM ($FE00-$FE9F) should:
   - Return $FF when PPU is in mode 2 (OAM Scan) or mode 3 (Drawing)
   - Return actual memory values during mode 0 (H-Blank) and mode 1 (V-Blank)
   - Follow the special line 0 timing when LCD is enabled

7. **VRAM Access Restrictions:** CPU reads to VRAM ($8000-$9FFF) should:
   - Return $FF when PPU is in mode 3 (Drawing)
   - Return actual memory values during modes 0, 1, and 2
   - Follow the special line 0 timing when LCD is enabled

8. **Sub-T-Cycle Timing:** By testing with 0, 1, and 2 T-cycle offsets, the test validates that the emulator handles sub-timing correctly and all transitions occur at the right moment.

## Potential Failure Reasons

Based on analysis of the emulator code at `/Users/nathaniel.manley/vcs/personal/gameboy-emulator`, here are the likely reasons this test would fail:

### 1. Missing Line 0 Special Behavior

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java`

**Issue:** When LCD is re-enabled (lines 52-60), the PPU resets to `Step.OAM_SETUP`:

```java
// LCD just got enabled - reset PPU state
if (!wasLcdEnabled) {
    wasLcdEnabled = true;
    registers.write(LY, (byte) 0);
    count = 0;
    step = Step.OAM_SETUP;  // WRONG - should start in H-Blank for line 0
    displayInterruptController.checkAndSendLyCoincidence();
    return;  // Start fresh on next tCycle
}
```

**Expected Behavior:** According to the test, when LCD is enabled:
- Line 0 should start in **mode 0 (H-Blank)**, not mode 2 (OAM Scan)
- After a brief period (~17-60 T-cycles), it should transition to mode 3 (Drawing)
- There should be a 2 T-cycle delay before the PPU truly starts

**Fix Required:** The LCD enable logic should:
1. Set initial mode to H-Blank (not OAM_SETUP)
2. Implement a 2 T-cycle delay before starting normal operation
3. Transition from H-Blank to Drawing (skipping OAM Scan) for line 0 only

### 2. Missing VRAM Access Restrictions

**Files:**
- `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MappedMemory.java`
- `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java`

**Issue:** The memory system does NOT implement any PPU mode-based access restrictions for VRAM ($8000-$9FFF).

In `MappedMemory.java`, reads to VRAM simply return the stored value:
```java
@Override
public byte read(short address) {
    int addr = uint(address);
    MemoryLocation mappedValue = memoryMap[addr];
    return (mappedValue != null) ? mappedValue.read() : defaultMemory[addr];
}
```

**Expected Behavior:**
- VRAM reads should return $FF when PPU is in mode 3 (Drawing)
- VRAM reads should return actual memory during modes 0, 1, 2

**Fix Required:** The memory system needs to:
1. Check the current PPU mode before allowing VRAM reads
2. Return $FF if in mode 3
3. Allow normal reads otherwise

### 3. Missing OAM Access Restrictions

**Files:**
- `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MappedMemory.java`
- `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java`

**Issue:** While `MemoryBus.java` implements DMA-based access blocking, it does NOT implement PPU mode-based access restrictions for OAM ($FE00-$FE9F).

The `isAccessibleDuringDma` method only blocks during DMA:
```java
private boolean isAccessibleDuringDma(short address) {
    int addr = address & 0xFFFF;
    // During DMA, allow access to HRAM (0xFF80-0xFFFE) and I/O registers (0xFF00-0xFF7F)
    return (addr >= 0xFF00 && addr <= 0xFFFE);
}
```

**Expected Behavior:**
- OAM reads should return $FF when PPU is in mode 2 (OAM Scan) or mode 3 (Drawing)
- OAM reads should return actual memory during modes 0 and 1

**Fix Required:** The memory system needs to:
1. Check the current PPU mode before allowing OAM reads
2. Return $FF if in mode 2 or 3
3. Allow normal reads during modes 0 and 1

### 4. PPU Mode Not Immediately Set on LCD Enable

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java`

**Issue:** When LCD is re-enabled, the code sets `step = Step.OAM_SETUP` but doesn't immediately set the STAT mode to H-Blank. The mode gets set later when `setupOamScan()` is called on the next cycle:

```java
private Step setupOamScan() {
    displayInterruptController.sendOamScan();  // Sets mode to OAM_SCANNING
    oamScanController.setupOamScan(uint(registers.read(LY)));
    count = 0;
    return oamScan();
}
```

**Expected Behavior:** According to the test expectations (STAT=$84 at cycle 0), mode 0 (H-Blank) should be set **immediately** when LCDC bit 7 is written, not on the next PPU cycle.

**Fix Required:** Set STAT mode to H-Blank immediately when LCD is enabled.

### 5. Missing 2 T-Cycle Delay

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java`

**Issue:** The test comments state: "line 0 has different timings because the PPU is late by 2 T-cycles". The current implementation doesn't account for this delay.

**Expected Behavior:** When LCD is enabled:
1. STAT mode should be set to mode 0 immediately
2. But the PPU should delay 2 T-cycles before actually starting to render
3. This affects all subsequent timing

**Fix Required:** Implement a 2 T-cycle startup delay when LCD is enabled.

### 6. Incorrect Mode Sequence for Line 0

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java`

**Issue:** Normal scanlines follow the sequence:
- Mode 2 (OAM Scan) → Mode 3 (Drawing) → Mode 0 (H-Blank)

But line 0 after LCD enable should be:
- Mode 0 (H-Blank) → Mode 3 (Drawing) → Mode 0 (H-Blank)

The current code doesn't implement this special case.

**Expected Behavior:** Line 0 after LCD enable skips mode 2 entirely.

**Fix Required:** Add special logic to skip OAM scan for the first line after LCD enable.

## Summary

The `lcdon_timing-GS` test will fail in the emulator due to:

1. **No special line 0 behavior** - Line 0 should start in mode 0, not mode 2
2. **No VRAM access restrictions** - Mode 3 should block VRAM reads
3. **No OAM access restrictions** - Modes 2 and 3 should block OAM reads
4. **Missing 2 T-cycle startup delay** - PPU should delay after LCD enable
5. **Incorrect initial mode** - Mode 0 should be set immediately when LCD is enabled
6. **Wrong mode sequence** - Line 0 should skip mode 2 (OAM Scan)

These issues are all related to accurate hardware timing emulation, which is critical for passing hardware test ROMs but may not be noticeable during normal game play.
