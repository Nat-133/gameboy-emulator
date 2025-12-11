# Mooneye Test Analysis: stat_lyc_onoff

## Test Overview

The `stat_lyc_onoff` test validates the behavior of the STAT register's LY=LYC coincidence flag (bit 2) and the corresponding STAT interrupt when the PPU is turned off and on. This test checks whether the coincidence flag is retained when the LCD is disabled, whether changing LYC while LCD is disabled has any effect, and whether interrupts fire correctly when the comparison result changes upon re-enabling the LCD.

Test location: `/Users/nathaniel.manley/vcs/personal/mooneye-test-suite/acceptance/ppu/stat_lyc_onoff.s`

## What The Test Does

The test has 4 rounds, each testing different scenarios of LCD on/off transitions:

### Round 1: Turn off PPU while coincidence=true, then test bit changes

1. **Setup**: Enable STAT LYC interrupt (bit 6 of STAT = 1), enable STAT interrupts in IE
2. **Wait for VBlank**: LY reaches 144 (0x90)
3. **Set LYC=0x90**: This makes LY=LYC, so coincidence bit should be set
4. **Disable LCD**: Write 0 to LCDC (turning off bit 7)
5. **Clear IF**: Clear any pending interrupts
6. **Enable interrupts**: Execute `ei` followed by `nop`
7. **Check step 1**: Read STAT, expect 0xC4
   - Bit 7: 1 (always reads as 1)
   - Bit 6: 1 (LYC interrupt enabled)
   - Bit 2: 1 (coincidence flag - should be retained)
   - Bits 1-0: 00 (mode 0, H-BLANK set when LCD disabled)
8. **Change LYC=0x01**: While LCD is off
9. **Check step 2**: Read STAT, expect 0xC4 (coincidence bit should still be 1)
   - The comparison clock is not running, so changing LYC should have no effect
10. **Enable LCD**: Write 0x80 to LCDC
11. **Check step 3**: Read STAT, expect 0xC0 (coincidence bit should now be 0)
    - LY resets to 0 when LCD is enabled, so LY=0 vs LYC=1 is false
    - This is a falling edge of the coincidence flag
12. **Expected interrupt behavior**: NO interrupt should fire because:
    - The coincidence bit goes from 1→0 (falling edge), which doesn't trigger an interrupt
    - Interrupts only fire on rising edges of the internal STAT line

### Round 2: Turn off PPU while coincidence=true, then test no bit change

1. **Setup**: Same as Round 1, but set interrupt handler to fail with "r2 intr"
2. **Wait for VBlank**: LY reaches 144 (0x90)
3. **Set LYC=0x90**: Coincidence is true
4. **Disable LCD**: Write 0 to LCDC
5. **Clear IF and enable interrupts**
6. **Check step 1**: Read STAT, expect 0xC4 (coincidence=1)
7. **Change LYC=0x00**: While LCD is off
8. **Check step 2**: Read STAT, expect 0xC4 (coincidence still 1)
9. **Enable LCD**: Write 0x80 to LCDC
10. **Check step 3**: Read STAT, expect 0xC4 (coincidence still 1)
    - LY resets to 0 when LCD enabled, so LY=0 vs LYC=0 is true
    - The coincidence flag stays high (no edge)
11. **Expected interrupt behavior**: NO interrupt should fire because:
    - The coincidence bit stays at 1 (no rising edge)
    - Even though the interrupt is enabled, the STAT line must transition from low→high to trigger

### Round 3: Turn off PPU while coincidence=false, then test no bit change

1. **Setup**: Same interrupt handler setup
2. **Wait for VBlank**: LY reaches 144
3. **Set LYC=0x00**: While at VBlank (LY=144), so LY≠LYC, coincidence=false
4. **Clear IF**
5. **Disable LCD**: Write 0 to LCDC
6. **Enable interrupts**
7. **Check step 1**: Read STAT, expect 0xC0 (coincidence=0)
8. **Change LYC=0x01**: While LCD is off
9. **Check step 2**: Read STAT, expect 0xC0 (coincidence still 0)
10. **Enable LCD**: Write 0x80 to LCDC
11. **Check step 3**: Read STAT, expect 0xC0 (coincidence still 0)
    - LY resets to 0, so LY=0 vs LYC=1 is false
    - Coincidence stays at 0
12. **Expected interrupt behavior**: NO interrupt because coincidence stays low

### Round 4: Turn off PPU while coincidence=false, then create rising edge

1. **Setup**: Disable interrupts initially
2. **Wait for VBlank**: LY reaches 144
3. **Set LYC=0x00**: While at VBlank, so coincidence=false
4. **Clear IF**
5. **Disable LCD**: Write 0 to LCDC
6. **Enable interrupts**
7. **Check**: Read STAT, expect 0xC0 (coincidence=0)
8. **Set interrupt handler to "finish" (success path)**
9. **Enable LCD**: Write 0x80 to LCDC
   - LY resets to 0, so LY=0 vs LYC=0 becomes true
   - This is a rising edge: coincidence goes from 0→1
10. **Disable interrupts**: `di` instruction
11. **Expected interrupt behavior**: MUST fire an interrupt before the `di` executes
    - The interrupt handler should jump to "finish" via `jp hl`
    - If no interrupt fires, execution continues and fails with "Fail: r4 no intr"

## What The Test Expects

The test expects the following behavior:

1. **Coincidence flag retention when LCD disabled**: When the LCD is turned off, the coincidence flag (STAT bit 2) must be retained at its current value. It should NOT be reset to 0.

2. **No comparison updates while LCD disabled**: When the LCD is off, changing LYC should not update the coincidence flag. The LY/LYC comparison clock is not running.

3. **Comparison resumes when LCD enabled**: When the LCD is re-enabled:
   - LY is reset to 0
   - The comparison clock starts running again
   - The coincidence flag is immediately updated based on the new LY=0 vs current LYC value

4. **Interrupt timing on LCD enable**: When enabling the LCD creates a rising edge of the coincidence flag (0→1), a STAT interrupt must fire immediately, before the next instruction executes.

5. **No interrupts on non-rising edges**: Interrupts should NOT fire when:
   - The coincidence flag falls (1→0)
   - The coincidence flag stays the same (no edge)

6. **Specific STAT values**: Throughout the test, specific STAT register values are checked:
   - 0xC4 = 0b1100_0100 (bit 7=1 always, bit 6=interrupt enabled, bit 2=coincidence, mode 0)
   - 0xC0 = 0b1100_0000 (bit 7=1 always, bit 6=interrupt enabled, bit 2=0 no coincidence, mode 0)

## What The Test Is Testing

This test validates several critical aspects of the PPU's STAT/LYC comparison system:

1. **STAT register state preservation**: The coincidence flag must be preserved when the LCD is disabled, not automatically cleared.

2. **Comparison clock control**: The LY/LYC comparison logic should only run when the LCD is enabled (LCDC bit 7 = 1).

3. **LCD enable behavior**: When the LCD is re-enabled:
   - LY should reset to 0
   - The comparison should execute immediately
   - The coincidence flag should be updated based on the current comparison

4. **STAT interrupt edge detection**: The STAT interrupt system must implement proper edge detection:
   - Only rising edges (0→1 transitions) of the internal STAT line should trigger interrupts
   - This requires tracking the previous state of the STAT line
   - Multiple simultaneous conditions (mode + LYC) are OR'd together into a single internal line

5. **Interrupt timing precision**: When a rising edge occurs during LCD enable, the interrupt must be triggered before the next instruction after the LCDC write completes.

## Potential Failure Reasons

Based on the emulator code analysis, here are potential reasons the test might fail:

### 1. Coincidence Flag Not Updated When LCD Enabled (Most Likely Issue)

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java` lines 52-59

```java
// LCD just got enabled - reset PPU state
if (!wasLcdEnabled) {
    wasLcdEnabled = true;
    registers.write(LY, (byte) 0);
    count = 0;
    step = Step.OAM_SETUP;
    // When LCD is enabled, check LY/LYC coincidence immediately
    displayInterruptController.checkAndSendLyCoincidence();
    return;  // Start fresh on next tCycle
}
```

**Analysis**: The code DOES call `checkAndSendLyCoincidence()` when the LCD is enabled, which looks correct. However, the timing might be off. The test expects the comparison to happen immediately when LCDC is written, but this code only executes on the next `tCycle()` call after the LCDC write.

**Problem**: The LYC comparison might not be updated at the exact moment the LCDC register is written. The test writes to LCDC and immediately expects the coincidence flag to be updated, but there may be a delay of one or more CPU cycles.

### 2. No Memory Listener for LCDC Writes

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/DisplayModule.java` lines 43-49

```java
DisplayInterruptController controller = new DisplayInterruptController(interruptController, ppuRegisters);
// Register listener for LYC writes (0xFF45) to handle mid-scanline LYC changes
memory.registerMemoryListener((short) 0xFF45, controller::checkAndSendLyCoincidence);
// Register listener for STAT writes (0xFF41) to re-evaluate STAT interrupt condition
memory.registerMemoryListener((short) 0xFF41, controller::checkStatCondition);
return controller;
```

**Analysis**: There is NO memory listener registered for LCDC writes (0xFF40). This means:
- When the game code writes to LCDC to enable/disable the LCD, nothing happens immediately
- The PPU only detects the change on its next `tCycle()` call
- The LYC comparison update is delayed until that `tCycle()` executes

**Problem**: The test expects immediate updates when LCDC is written, but the emulator delays processing until the next PPU tick. This could cause:
- Round 1 step 3: Coincidence flag might not update immediately when LCD is enabled
- Round 4: The interrupt might not fire at the right time, or might not fire before the `di` instruction

### 3. Coincidence Flag May Be Cleared When LCD Disabled

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java` lines 41-48

```java
// Handle LCD disabled state
if (!lcdEnabled) {
    if (wasLcdEnabled) {
        // Just got disabled - set mode to HBlank but don't reset LY
        // The coincidence flag is retained
        registers.setStatMode(StatParser.PpuMode.H_BLANK);
        wasLcdEnabled = false;
    }
    return;  // PPU does not run when LCD is disabled
}
```

**Analysis**: The comment says "The coincidence flag is retained", which is correct. The code only sets the mode to H_BLANK and doesn't touch the coincidence flag.

**Verification**: Looking at `registers.setStatMode()` in `PpuRegisters.java` line 56-59:
```java
public void setStatMode(StatParser.PpuMode mode) {
    if (statRegister != null) {
        statRegister.setMode(mode);
    }
}
```

And in `StatRegister.java` lines 38-46:
```java
public void setMode(StatParser.PpuMode mode) {
    int modeValue = switch (mode) {
        case H_BLANK -> 0;
        case V_BLANK -> 1;
        case OAM_SCANNING -> 2;
        case DRAWING -> 3;
    };
    this.value = (byte) ((value & ~MODE_MASK) | modeValue);
}
```

Where `MODE_MASK = 0b0000_0011` (line 12). This correctly preserves bit 2 (the coincidence flag).

**Conclusion**: This appears correct - the coincidence flag should be retained.

### 4. checkAndSendLyCoincidence May Not Work When LCD Disabled

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/DisplayInterruptController.java` lines 52-68

```java
public void checkAndSendLyCoincidence() {
    // When LCD is disabled, the LY/LYC comparison clock is not running.
    // The coincidence flag should be retained as-is and no interrupt should fire.
    if (!LcdcParser.lcdEnabled(registers.read(PpuRegister.LCDC))) {
        return;
    }

    boolean lyIsLyc = registers.read(PpuRegister.LY) == registers.read(PpuRegister.LYC);

    // Set coincidence flag using internal PPU method
    registers.setStatCoincidenceFlag(lyIsLyc);

    byte stat = registers.read(PpuRegister.STAT);
    lycLine = lyIsLyc && StatParser.lyCompareInterruptEnabled(stat);

    checkAndTriggerStatInterrupt();
}
```

**Analysis**: This is correct - when LCD is disabled, the method returns early without updating the coincidence flag or firing interrupts. This matches the test's expectations for rounds 1-3.

**Verification**: This method is called:
1. When LYC is written (memory listener at 0xFF45) - will return early if LCD disabled ✓
2. When LCD is enabled in `PictureProcessingUnit.java` line 58 - LCD is already enabled at this point ✓
3. When LY is updated via `updateLY()` - only called when PPU is running ✓

**Conclusion**: This appears correct for the test requirements.

### 5. Critical Issue: LCDC Write Does Not Trigger Immediate LYC Check

**Root Cause Analysis**:

The test writes to LCDC at 0xFF40 to enable the LCD:
```assembly
ld a, $80
ldh (<LCDC), a
```

At this moment:
1. The LCDC register is updated in memory
2. NO memory listener is triggered (none registered for 0xFF40)
3. The CPU continues executing the next instruction
4. Eventually, the PPU's `tCycle()` method runs (4 PPU cycles per 1 CPU cycle)
5. The PPU detects the LCD was just enabled
6. It resets LY to 0 and calls `checkAndSendLyCoincidence()`

**The Problem**: There's a gap between when LCDC is written and when the coincidence flag is updated. In Round 4, the test does:
```assembly
ld a, $80        ; Load 0x80 into A
ld hl, finish    ; Set interrupt handler to finish label
ldh (<LCDC), a   ; Enable LCD - should trigger interrupt here
di               ; Disable interrupts - should NOT reach here if interrupt fired
quit_failure_string "Fail: r4 no intr"
```

The test expects the STAT interrupt to fire between the `ldh (<LCDC), a)` and `di` instructions. But in the emulator:
1. `ldh (<LCDC), a)` writes to memory, no listener fires
2. `di` executes, disabling interrupts
3. Later, the PPU ticks and notices LCD is enabled
4. The coincidence flag is updated and interrupt is set
5. But interrupts are now disabled, so the interrupt never executes

**This is the most likely failure mode for Round 4.**

### 6. Potential Fix Required

To fix this, the emulator needs to:

1. **Add an LCDC memory listener** at address 0xFF40 that:
   - Detects when LCDC bit 7 (LCD enable) changes from 0→1
   - When enabled: immediately resets LY to 0 and calls `checkAndSendLyCoincidence()`
   - When disabled: immediately sets mode to H_BLANK

2. **Alternative approach**: The memory listener could trigger an immediate PPU tick or directly update the relevant state without waiting for the next `tCycle()`.

### 7. Minor Issue: LY Write Behavior When LCD Disabled

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java` line 54

```java
registers.write(LY, (byte) 0);
```

**Analysis**: When LCD is enabled, LY is reset to 0. This is correct according to the test expectations. However, the test also seems to assume that:
- When LCD is disabled, LY should retain its current value (0x90 in the test)
- The comment on line 43 says "don't reset LY" when disabling

Looking at the disable code (lines 41-48), LY is NOT modified when the LCD is disabled, which is correct.

**Conclusion**: This appears correct.

## Summary of Likely Failures

The test will most likely fail on **Round 4** with the message "Fail: r4 no intr" because:

1. The emulator does not have a memory listener for LCDC (0xFF40) writes
2. When the test writes to LCDC to enable the LCD, the change is not processed until the next PPU tick
3. By the time the PPU processes the LCD enable and updates the coincidence flag, the `di` instruction has already executed
4. The STAT interrupt is set but never fires because interrupts are disabled

Secondary failure modes that could occur:

1. **Round 1 step 3**: Could fail with "Fail: r1 step 3" if the coincidence flag is not updated quickly enough when LCD is enabled
2. **Round 2 or 3**: Could fail with "Fail: r2 intr" or "Fail: r3 intr" if spurious interrupts fire when they shouldn't (unlikely given the current code)

The fix requires adding immediate LCDC write handling, either via a memory listener or by making LCDC writes trigger immediate PPU state updates.
