# Mooneye Test Analysis: boot_div-dmgABCmgb

## Test Overview

The `boot_div-dmgABCmgb` test verifies the initial value and relative timing phase of the DIV (Divider Register) after the boot ROM completes execution. This test is designed to pass on DMG models A, B, C and the MGB (Game Boy Pocket), but fail on other hardware variants (DMG 0, SGB, SGB2, CGB, AGB, AGS).

The test is located at: `/Users/nathaniel.manley/vcs/personal/mooneye-test-suite/acceptance/boot_div-dmgABCmgb.s`

## What The Test Does

The test performs a series of precisely-timed DIV register reads with varying NOP delays to test both the initial value and the timing phase of DIV increments:

### Step-by-Step Execution Flow

1. **Initial Delay (6 NOPs)**
   - Executes 6 NOP instructions (24 T-cycles total)
   - Comment states: "This read should happen immediately after DIV has incremented"
   - Reads DIV register into A and pushes AF to stack

2. **First Phase-Consistent Read (57 NOPs)**
   - Executes 57 NOPs (228 T-cycles)
   - Plus ldh a, (<DIV) = 3 M-cycles = 12 T-cycles
   - Plus push af = 4 M-cycles = 16 T-cycles
   - Total: 256 T-cycles between DIV reads
   - Comment: "the next read should happen immediately after the next increment"
   - DIV should have incremented by exactly 1

3. **Phase Shift Read (56 NOPs)**
   - Executes 56 NOPs (224 T-cycles)
   - Plus ldh + push = 28 T-cycles
   - Total: 252 T-cycles between reads
   - Comment: "the next read should happen immediately *before* the increment"
   - This shifts the phase by 4 T-cycles earlier
   - DIV should NOT have incremented yet (same value as previous)

4. **Phase Maintained Read (57 NOPs)**
   - Back to 57 NOPs (256 T-cycles total)
   - Comment: "the next read should happen once again immediately *before* the increment"
   - Phase shift from step 3 is maintained
   - DIV should have incremented by 1

5. **Another Phase Maintained Read (57 NOPs)**
   - Another 57 NOPs (256 T-cycles)
   - Comment: "Same thing here..."
   - Still reading before increment
   - DIV should have incremented by 1

6. **Phase Shift Back (58 NOPs)**
   - Executes 58 NOPs (232 T-cycles)
   - Plus ldh + push = 28 T-cycles
   - Total: 260 T-cycles
   - Comment: "the read should happen after the increment once again"
   - This shifts phase by 4 T-cycles later
   - DIV should have incremented by 2 (once for the 256 T-cycles, once more because we read after the increment point)

### Final Register Population

The test pops the 6 saved DIV values from the stack into registers:
- Last read -> L
- 5th read -> H
- 4th read -> E
- 3rd read -> D
- 2nd read -> C
- 1st read -> B

## What The Test Expects

The test expects the following DIV values in the registers:

- **B (1st read after 6 NOPs)**: `0xAC`
- **C (2nd read after 57 NOPs)**: `0xAD` (B + 1)
- **D (3rd read after 56 NOPs)**: `0xAD` (same as C, phase shifted earlier)
- **E (4th read after 57 NOPs)**: `0xAE` (D + 1)
- **H (5th read after 57 NOPs)**: `0xAF` (E + 1)
- **L (6th read after 58 NOPs)**: `0xB1` (H + 2, phase shifted later)

### Key Expectations

1. **Initial DIV Value**: The first read (B) expects `0xAC`, indicating DIV should be at approximately this value when the boot ROM completes and the game ROM starts executing
2. **DIV Increment Rate**: DIV increments every 256 T-cycles (64 M-cycles)
3. **Timing Precision**: The test verifies sub-M-cycle timing precision by detecting when reads happen before vs after the exact increment moment
4. **Phase Relationships**: The test confirms that the timing phase can be shifted by adding/removing M-cycles, and that these shifts persist

## What The Test Is Testing

This test validates three critical aspects of the DIV register implementation:

### 1. Post-Boot DIV Initial Value
The DIV register must have a specific value (`0xAC`) when the boot ROM finishes and control transfers to the game ROM. This value depends on:
- The duration of the boot ROM execution (varies by hardware model)
- The continuous incrementing of the internal timer counter during boot

### 2. DIV Increment Timing
The test verifies that DIV increments at the correct rate:
- DIV is the upper 8 bits of a 16-bit internal counter
- The internal counter increments every T-cycle (4.194304 MHz)
- DIV effectively increments every 256 T-cycles

### 3. Sub-M-Cycle Timing Precision
The test verifies precise timing alignment:
- By varying NOPs by 1 (4 T-cycles), the test can shift reads to before or after an increment
- This tests that the emulator correctly implements T-cycle level timing, not just M-cycle timing
- The phase shifts demonstrate that the exact T-cycle when DIV increments is consistent and predictable

## Potential Failure Reasons

Based on the emulator code at `/Users/nathaniel.manley/vcs/personal/gameboy-emulator`, here are the potential reasons this test might fail:

### 1. Initial Timer Counter Value (LIKELY ISSUE)

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/CoreModule.java:55`

```java
@Provides
@Singleton
InternalTimerCounter provideInternalTimerCounter() {
    // Post-boot state for DMG/MGB (DIV = $AB, aligned for boot_sclk_align test)
    return new InternalTimerCounter(0xAAC8);
}
```

**Problem**: The initial counter value is set to `0xAAC8`, which gives DIV = `0xAA`. The test expects the first DIV read to be `0xAC`, not `0xAA`.

**Calculation**:
- Internal counter: `0xAAC8`
- DIV = upper 8 bits = `0xAAC8 >> 8` = `0xAA`

**Expected**:
- First read should be `0xAC`
- This means internal counter should be approximately `0xAC00` to `0xACFF`

**The Comment is Misleading**: The comment says "aligned for boot_sclk_align test" but this value might not be correct for `boot_div-dmgABCmgb`. Different DMG hardware revisions have different boot ROM durations and thus different post-boot DIV values.

### 2. Timing Alignment After Boot

**Issue**: The test starts with 6 NOPs and expects to read DIV "immediately after DIV has incremented". This suggests that:
- The internal counter should be at a specific sub-value within the `0xAC` range
- Specifically, it should be near `0xAC00` so that after 6 NOPs (24 T-cycles), the counter crosses from `0xABFF` to `0xAC00` or similar

**Current State**: With `0xAAC8`:
- This is 200 T-cycles into the `0xAA` period
- After 6 NOPs (24 T-cycles), counter = `0xAAE0` (still in the `0xAA` period)
- This is not aligned correctly for the test's expectations

### 3. Correct Initial Counter for DMG ABC/MGB

To determine the correct initial value:

1. **First read expects `0xAC`**: The counter's upper byte must be `0xAC`
2. **Read happens "immediately after DIV has incremented"**: This means the counter should have just crossed from `0xABFF` to `0xAC00` (or be very close to `0xAC00`)
3. **6 NOPs before first read**: These take 24 T-cycles

**Calculation**:
- If we want DIV to read as `0xAC` immediately after incrementing
- And 6 NOPs (24 T-cycles) need to align us to just after the `0xABFF -> 0xAC00` transition
- The counter should start at approximately: `0xAC00 - 24 = 0xABE8`

However, this is approximate. The exact value depends on the boot ROM execution time and when it transfers control.

### 4. Hardware Model Differences

Different Game Boy models have different boot ROM execution times:
- **DMG 0**: Shorter boot time, DIV starts around `0x19` (test boot_div-dmg0.s)
- **DMG ABC/MGB**: Longer boot time, DIV starts around `0xAC` (test boot_div-dmgABCmgb.s)
- **SGB/SGB2**: Different boot time, DIV starts around `0xD9` (test boot_div-S.s)

The emulator is currently configured for DMG/MGB but with an incorrect initial value that produces `0xAA` instead of `0xAC`.

### 5. T-Cycle vs M-Cycle Timing

**Observation**: The DividerRegister correctly reads the upper 8 bits of the 16-bit internal counter, and the InternalTimerCounter correctly increments every T-cycle. This part appears correctly implemented.

**Potential Issue**: If the CPU emulator runs instructions at M-cycle granularity but the timer runs at T-cycle granularity, there could be synchronization issues. However, the Timer.mCycle() method correctly calls internalCounter.tCycle() four times per M-cycle, so this should be working correctly.

## Recommended Fix

To fix this test failure, update the initial internal timer counter value in CoreModule:

```java
@Provides
@Singleton
InternalTimerCounter provideInternalTimerCounter() {
    // Post-boot state for DMG ABC/MGB (DIV = $AC)
    // Value chosen to align with boot_div-dmgABCmgb test expectations:
    // - First read after 6 NOPs should be $AC
    // - Read should happen immediately after DIV increments
    return new InternalTimerCounter(0xABE8);
}
```

**Note**: The exact value may need fine-tuning based on actual test execution. The value `0xABE8` is calculated to:
- Start at `0xABE8`
- After 6 NOPs (24 T-cycles): `0xABE8 + 24 = 0xAC00`
- This should read as `0xAC` immediately after the increment

Alternatively, if the test needs to be run just after boot, you might need to consider where in the boot sequence control is transferred and adjust accordingly.

## Additional Notes

1. **Boot ROM Duration**: The actual boot ROM on DMG ABC/MGB hardware takes a specific number of cycles to execute. The emulator should either:
   - Emulate the boot ROM to naturally arrive at the correct DIV value
   - Skip the boot ROM but initialize memory/registers (including the internal timer) to post-boot state

2. **Test Variants**: There are multiple boot_div tests for different hardware:
   - `boot_div-dmg0.s`: Expects DIV = `0x19` (DMG revision 0)
   - `boot_div-dmgABCmgb.s`: Expects DIV = `0xAC` (DMG revisions A/B/C and MGB)
   - `boot_div-S.s`: Expects DIV = `0xD9` (Super Game Boy)

   The emulator should be configurable to match the target hardware model.

3. **Serial Clock Alignment**: The current comment mentions "aligned for boot_sclk_align test", suggesting that the `0xAAC8` value was chosen for a different test. You may need different initial values for different tests, or a way to configure the hardware model being emulated.
