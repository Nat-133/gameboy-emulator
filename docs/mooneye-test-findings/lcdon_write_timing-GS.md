# Mooneye Test Analysis: lcdon_write_timing-GS

## Test Overview

The `lcdon_write_timing-GS` test validates the timing of when writes to OAM and VRAM become accessible after enabling the PPU by writing to the LCDC register. This is a DMG/MGB/SGB-specific test that checks precise cycle-accurate behavior of memory access restrictions following LCD enable.

## What The Test Does

The test performs two main test suites - one for OAM access and one for VRAM access. Each suite runs 19 test cases with different NOP delays after enabling the LCD.

### Step-by-Step Execution Flow

For each test case:

1. **Setup Phase**:
   - Clear OAM and VRAM memory locations to $00
   - Build test code dynamically in WRAM at `wram.test_code`

2. **Test Code Generation**:
   - Copy prologue code that writes $81 to LCDC (enabling LCD)
   - Insert a specific number of NOPs (delays) based on test case
   - Copy epilogue code that attempts to write $81 to target memory (OAM or VRAM)
   - Return from test code

3. **Test Execution**:
   - Execute the dynamically generated code from WRAM
   - This performs: `LCDC = $81` -> `N NOPs` -> `write $81 to [DE]` (where DE is OAM or VRAM address)
   - Disable PPU after each test
   - Read back the value from the target address and store in results array

4. **Verification**:
   - Compare actual results with expected patterns
   - Expected patterns indicate which NOP counts allow successful writes ($81) vs blocked writes ($00)

### Test Cases (NOP Counts)

The test runs with these specific NOP delays after LCDC write:
```
0, 17, 18, 60, 61, 110, 111, 112, 130, 131, 132, 174, 175, 224, 225, 226, 244, 245, 246
```

These specific values are chosen to test boundaries where memory access transitions from blocked to accessible.

## What The Test Expects

### Expected OAM Access Pattern
```
nop_counts:        0   17  18  60  61  110 111 112 130 131 132 174 175 224 225 226 244 245 246
expect_oam_access: $81 $81 $00 $00 $81 $81 $81 $00 $00 $81 $00 $00 $81 $81 $81 $00 $00 $81 $00
```

### Expected VRAM Access Pattern
```
nop_counts:         0   17  18  60  61  110 111 112 130 131 132 174 175 224 225 226 244 245 246
expect_vram_access: $81 $81 $00 $00 $81 $81 $81 $81 $81 $81 $00 $00 $81 $81 $81 $81 $81 $81 $00
```

### Interpretation

- **$81**: Write succeeded - memory was accessible at this cycle count
- **$00**: Write blocked - memory was inaccessible at this cycle count

The patterns show that:
1. OAM and VRAM have different access windows after LCD enable
2. Access is initially allowed (0-17 NOPs)
3. Access becomes blocked during certain PPU mode transitions
4. VRAM has slightly broader access windows than OAM (as expected - OAM is only accessible during H-Blank and V-Blank, while VRAM is accessible during H-Blank, V-Blank, and some other periods)

## What The Test Is Testing

This test validates several critical PPU timing behaviors:

### 1. LCD Enable Timing
When LCDC bit 7 is set (LCD enabled), the PPU does not immediately enter mode 2 (OAM scan). There is a brief initialization period.

### 2. Memory Access Windows Based on PPU Mode
The Game Boy hardware restricts CPU access to OAM and VRAM based on PPU mode:

- **OAM ($FE00-$FE9F)**:
  - Accessible during: H-Blank (mode 0), V-Blank (mode 1)
  - Blocked during: OAM Scan (mode 2), Drawing (mode 3)

- **VRAM ($8000-$9FFF)**:
  - Accessible during: H-Blank (mode 0), V-Blank (mode 1), OAM Scan (mode 2)
  - Blocked during: Drawing (mode 3)

### 3. Cycle-Accurate PPU Mode Transitions
The test verifies that the emulator transitions between PPU modes at the exact correct cycle counts after LCD enable, specifically:
- When OAM scanning begins
- When drawing begins
- When H-Blank periods occur
- Duration of each mode

### 4. Memory Access Behavior During Transitions
The specific NOP counts test boundary conditions where the PPU is transitioning between modes to ensure access restrictions apply at precisely the right moments.

## Potential Failure Reasons

Based on analysis of the emulator code at `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/`, several issues could cause this test to fail:

### 1. Missing Memory Access Restrictions (PRIMARY ISSUE)

**Location**: Memory read/write operations in `MappedMemory.java` and `MemoryBus.java`

**Problem**: The emulator does NOT implement PPU-mode-based memory access restrictions for OAM and VRAM.

**Evidence**:
- `MappedMemory.java` (lines 69-87): read/write methods have no PPU mode checks
- `MemoryBus.java` (lines 61-81): Only implements DMA-related blocking, no PPU mode blocking
- No interaction between PPU state and Memory classes for access control

**Expected Behavior**:
- Reads/writes to $FE00-$FE9F should return $FF when PPU is in mode 2 or 3
- Writes to $FE00-$FE9F should be ignored when PPU is in mode 2 or 3
- Reads/writes to $8000-$9FFF should return $FF / be ignored when PPU is in mode 3

**Current Behavior**: All memory accesses succeed regardless of PPU mode

### 2. LCD Enable Timing Issues

**Location**: `PictureProcessingUnit.java` lines 52-60

**Problem**: When LCD is enabled, the implementation:
```java
if (!wasLcdEnabled) {
    wasLcdEnabled = true;
    registers.write(LY, (byte) 0);
    count = 0;
    step = Step.OAM_SETUP;
    displayInterruptController.checkAndSendLyCoincidence();
    return;  // Start fresh on next tCycle
}
```

This immediately returns after setting up, meaning the first cycle after LCD enable doesn't execute any PPU logic. The PPU starts running on the NEXT cycle.

**Expected Behavior**: The test expects specific timing for when the PPU enters various modes after LCD enable. The current "skip first cycle" approach may cause off-by-one errors in mode transitions.

### 3. Missing Timing Adjustment for LCD Enable

**Location**: `PictureProcessingUnit.java` line 56

**Problem**: When LCD is enabled, the code resets `count = 0` and `step = Step.OAM_SETUP`, but this doesn't account for the actual hardware behavior.

**Hardware Behavior**: On real hardware, when LCD is enabled:
- LY starts at 0
- The PPU enters mode 0 (H-Blank) initially for a few cycles
- Then begins the first scanline with OAM scan

**Current Behavior**: Immediately sets step to OAM_SETUP, which will enter OAM scan mode on the next cycle, potentially skipping the initial H-Blank period.

### 4. Mode Transition Timing

**Location**: `PictureProcessingUnit.java` lines 72-142

**Problem**: The test's specific NOP counts (0, 17, 18, 60, 61, 110, 111, etc.) suggest very specific cycle boundaries where access changes. If the PPU mode durations are even slightly off, the access patterns will not match expectations.

**Timing Requirements** (from Game Boy documentation):
- Mode 2 (OAM Scan): 80 dots (20 M-cycles)
- Mode 3 (Drawing): 168-291 dots (42-73 M-cycles, variable)
- Mode 0 (H-Blank): remainder of 456 dots per scanline
- First scanline after LCD enable should match standard timing

### 5. Memory Access Returns Wrong Values When Blocked

**Location**: Not implemented anywhere

**Problem**: When OAM/VRAM is blocked, the emulator currently returns the actual memory value.

**Expected Behavior**:
- Reads during blocked periods should return $FF
- Writes during blocked periods should be ignored (not modify memory)

**Test Impact**: This is why the test writes $81 and then reads back the value. If writes are properly blocked, it should read $00 (the cleared value). If writes succeed improperly, it reads $81.

### 6. T-Cycle vs M-Cycle Confusion

**Location**: Throughout PPU implementation

**Problem**: The test counts in terms of CPU cycles (NOPs are 1 M-cycle = 4 T-cycles each). The PPU runs at T-cycle granularity (4x CPU speed via `ClockWithParallelProcess`).

**Verification Needed**:
- Ensure NOP count properly corresponds to M-cycles
- Ensure PPU mode transitions occur at correct T-cycle boundaries
- The `count` variable in `PictureProcessingUnit.java` counts T-cycles, not M-cycles

### 7. Stat Mode Not Updated on LCD Disable

**Location**: `PictureProcessingUnit.java` lines 41-48

**Current Implementation**: Sets mode to H_BLANK when LCD disabled
```java
if (!lcdEnabled) {
    if (wasLcdEnabled) {
        registers.setStatMode(StatParser.PpuMode.H_BLANK);
        wasLcdEnabled = false;
    }
    return;
}
```

This is correct, but when re-enabled, there may be a mismatch if the test expects the PPU to start from a specific mode state.

## Summary of Required Fixes

To pass this test, the emulator needs:

1. **Implement Memory Access Restrictions** (Critical):
   - Add PPU mode awareness to memory bus
   - Block OAM reads/writes during modes 2 and 3
   - Block VRAM reads/writes during mode 3
   - Return $FF for blocked reads
   - Ignore blocked writes

2. **Fix LCD Enable Timing** (Critical):
   - Ensure PPU enters correct initial mode after LCD enable
   - Account for any initial delay before OAM scan begins
   - Verify mode transition timing matches hardware

3. **Verify T-Cycle Accuracy** (Important):
   - Ensure NOP timing properly translates to PPU cycles
   - Verify PPU mode durations are cycle-accurate
   - Test that mode transitions happen at exact expected boundaries

4. **Add PPU-Memory Integration** (Critical):
   - Create a mechanism for Memory classes to query current PPU mode
   - Consider adding a `MemoryAccessController` that wraps memory and checks PPU state
   - Ensure this works with both `MappedMemory` and `SimpleMemory` implementations

The test failure is most likely caused by issue #1 - the complete absence of PPU-mode-based memory access restrictions. Even if all timing is perfect, without checking PPU mode before allowing OAM/VRAM access, the test cannot pass.
