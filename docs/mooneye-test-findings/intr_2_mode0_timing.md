# Mooneye Test Analysis: intr_2_mode0_timing

## Test Overview

This test validates the precise timing of STAT mode 2 (OAM scan) interrupts and when the PPU transitions to mode 0 (HBlank). The test measures how many cycles it takes from receiving a mode 2 interrupt to reaching mode 0, testing that the timing is accurate to a single cycle.

Test file: `/Users/nathaniel.manley/vcs/personal/mooneye-test-suite/acceptance/ppu/intr_2_mode0_timing.s`

## What The Test Does

### Setup Phase
1. Sets stack pointer to DEFAULT_SP (0xE000)
2. Waits for VBlank
3. Loads HL register with STAT address (0xFF41)
4. Enables STAT interrupts only (writes INTR_STAT = 0x02 to IE register at 0xFFFF)

### Test Iteration 1 (46 NOPs delay)
1. Calls `setup_and_wait_mode2`:
   - Waits for LY to reach 0x42 (scanline 66)
   - Waits for mode 0 (HBlank)
   - Waits for mode 3 (Drawing)
   - Writes 0b00100000 to STAT (enables mode 2/OAM interrupt, bit 5)
   - Clears interrupt flags (IF = 0)
   - Enables interrupts with EI
   - Executes HALT
   - HALT waits for interrupt, then resumes with NOP after HALT
   - If execution continues past NOP, jumps to fail_halt

2. When STAT interrupt fires (mode 2 detected):
   - Interrupt handler at 0x0048 (INTR_VEC_STAT) executes
   - Handler does: `add sp,+2` (removes return address from stack)
   - Then `ret` (returns to modified address)
   - This causes execution to continue after the `jp fail_halt` instruction

3. After interrupt returns, test executes 46 NOPs
4. Then enters tight loop counting cycles until mode becomes 0:
   ```
   ld b, $00        ; Initialize counter
   - inc b          ; Increment counter
   ld a, (hl)       ; Read STAT register (HL points to STAT)
   and $03          ; Mask lower 2 bits (mode bits)
   jr nz, -         ; Loop if not mode 0
   ```
5. Stores counter result in register D

### Test Iteration 2 (45 NOPs delay)
1. Same process as iteration 1, but with 45 NOPs instead of 46
2. Stores counter result in register E

### Assertions
1. Saves all registers to HRAM
2. Asserts D == 0x01 (with 46 NOPs, should loop once before mode 0)
3. Asserts E == 0x02 (with 45 NOPs, should loop twice before mode 0)
4. Exits with pass/fail based on assertions

## What The Test Expects

- **Register D = 0x01**: After 46 NOPs from the STAT mode 2 interrupt handler return, the test should loop exactly once before mode 0 is reached
- **Register E = 0x02**: After 45 NOPs from the STAT mode 2 interrupt handler return, the test should loop exactly twice before mode 0 is reached

This means that the difference of 1 NOP (4 CPU cycles) causes exactly 1 additional loop iteration, demonstrating cycle-accurate timing.

## What The Test Is Testing

This test validates:

1. **STAT Mode 2 Interrupt Timing**: The interrupt must fire at the exact cycle when mode 2 begins
2. **Mode 2 to Mode 0 Transition Timing**: The precise number of cycles from mode 2 start to mode 0 start
3. **Interrupt Handler Execution Timing**: How long it takes to enter and exit the interrupt handler
4. **PPU Mode Duration**: Mode 2 (OAM scan) takes 80 dots (80 PPU cycles = 20 CPU cycles), and mode 3 (Drawing) takes a variable amount depending on the scanline content

### Expected Timeline

For a scanline without sprites, scroll, or window (as specified in the test):

1. **Mode 2 (OAM Scan)**: 80 dots / 4 = 20 CPU cycles
2. **Mode 3 (Drawing)**: Minimum ~172 dots / 4 = 43 CPU cycles (variable, but minimal for no sprites/scroll/window)
3. **Total from Mode 2 start to Mode 0 start**: ~252 dots / 4 = 63 CPU cycles

The test uses HALT which waits in low power mode and then resumes immediately after the interrupt handler completes. The interrupt handler overhead and return adds additional cycles.

The critical measurement is: from when the interrupt handler returns to when mode 0 is detected, with 46 vs 45 NOPs causing a 1-loop difference in the polling loop.

## Potential Failure Reasons

### 1. Mode 2 Interrupt Timing Issues

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java` (lines 72-77)

```java
private Step setupOamScan() {
    displayInterruptController.sendOamScan();  // Interrupt sent here
    oamScanController.setupOamScan(uint(registers.read(LY)));
    count = 0;
    return oamScan();
}
```

**Potential Issue**: The interrupt is sent at the beginning of `setupOamScan()`, but the mode transition might not be occurring at the exact same cycle. The `sendOamScan()` method sets the mode and checks for interrupt:

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/DisplayInterruptController.java` (lines 41-50)

```java
public void sendOamScan() {
    registers.setStatMode(StatParser.PpuMode.OAM_SCANNING);

    byte stat = registers.read(PpuRegister.STAT);
    activeModeLine = StatParser.oamInterruptEnabled(stat)
            ? Optional.of(ActiveModeLine.OAM)
            : Optional.empty();

    checkAndTriggerStatInterrupt();
}
```

The mode is set, then the interrupt is checked. This should be correct, but timing must be exact.

### 2. Mode 3 (Drawing) Duration

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java` (lines 92-102)

```java
private Step drawScanline() {
    scanlineController.performSingleClockCycle();
    count++;

    if (!scanlineController.drawingComplete()) {
        return Step.SCANLINE_DRAWING;
    }

    displayInterruptController.sendHblank();
    return Step.HBLANK;
}
```

**Potential Issue**: The mode 3 duration might not be accurate for a minimal scanline (no sprites, no scroll, no window). The test expects very specific timing, so if mode 3 is even slightly longer or shorter than expected, the test will fail.

The `ScanlineController` handles the pixel fetching and drawing. For a scanline with SCX=0 (no scroll), no sprites, and no window, the mode 3 duration should be minimal but there may be implementation-specific delays.

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/ScanlineController.java` (lines 70-74)

```java
public void setupScanline() {
    LX = 0;
    backgroundFetcher.reset();
    state = shouldDiscardPixel() ? State.DISCARD_PIXELS : State.PIXEL_FETCHING;
}
```

The `shouldDiscardPixel()` check might add unexpected cycles if SCX is not 0.

### 3. Clock Synchronization

**Location**: Multiple locations where `clock.tick()` and `ppuClock.tick()` are called

The emulator uses a `ClockWithParallelProcess` to run the PPU at 4x the CPU clock speed (PPU runs at 4MHz while CPU runs at 1MHz). Each PPU cycle (dot) is 1/4 of a CPU cycle.

**Potential Issue**: If the clock synchronization between CPU and PPU is not exact, the timing measurements in the test will be off. The test is measuring in CPU cycles, but the PPU modes are defined in PPU dots.

### 4. Interrupt Handler Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/Cpu.java` (lines 40-45)

```java
private void handlePotentialInterrupt() {
    if (cpuStructure.registers().IME() && cpuStructure.interruptBus().hasInterrupts()) {
        Interrupt highestPriorityInterrupt = cpuStructure.interruptBus().activeInterrupts().getFirst();
        HardwareInterrupt.callInterruptHandler(cpuStructure, highestPriorityInterrupt);
    }
}
```

**Potential Issue**: The interrupt is checked after a fetch cycle, which adds a specific delay. The test expects the interrupt to be handled with precise timing. If the interrupt handling adds unexpected cycles or if the interrupt is delayed by instruction boundaries, the timing will be off.

### 5. HALT Instruction Behavior

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Halt.java` (lines 14-26)

```java
@Override
public void execute(CpuStructure cpuStructure) {
    boolean IME = cpuStructure.registers().IME();
    boolean interruptsPending = cpuStructure.interruptBus().hasInterrupts();

    if (!IME && interruptsPending) {
        cpuStructure.idu().disableNextIncrement();
    }

    cpuStructure.clock().stop();
    cpuStructure.interruptBus().waitForInterrupt();
    cpuStructure.clock().start();
}
```

**Potential Issue**: The `waitForInterrupt()` method ticks the clock until an interrupt is pending. This should be correct, but if there's any timing discrepancy in how HALT resumes after an interrupt, it could affect the test. The HALT should resume with the NOP instruction immediately after the interrupt handler returns.

### 6. Mode 0 Detection Timing

The test reads STAT register repeatedly to detect mode 0. The mode is set by:

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/DisplayInterruptController.java` (lines 30-39)

```java
public void sendHblank() {
    registers.setStatMode(StatParser.PpuMode.H_BLANK);

    byte stat = registers.read(PpuRegister.STAT);
    activeModeLine = StatParser.hblankInterruptEnabled(stat)
            ? Optional.of(ActiveModeLine.HBLANK)
            : Optional.empty();

    checkAndTriggerStatInterrupt();
}
```

**Potential Issue**: The mode is written immediately when `sendHblank()` is called. However, this happens at the end of mode 3 drawing. If the drawing cycle count is off, the mode 0 will appear at the wrong time relative to the mode 2 interrupt.

## Summary

The most likely causes of failure are:

1. **Incorrect Mode 3 duration**: The drawing phase may be taking too many or too few cycles for a minimal scanline
2. **Clock synchronization issues**: The 4:1 PPU-to-CPU clock ratio may not be perfectly synchronized
3. **Interrupt handler timing**: The cycles between interrupt flag being set and interrupt handler executing may not match hardware
4. **HALT resume timing**: The cycle at which execution resumes after HALT may be off by a cycle or two

To fix this test, the emulator needs to ensure that:
- Mode 2 lasts exactly 80 PPU dots (20 CPU cycles)
- Mode 3 for a minimal scanline (no sprites, SCX=0, no window) lasts the correct number of dots
- The interrupt fires on the exact cycle that mode 2 begins
- The HALT instruction resumes on the exact cycle after the interrupt handler completes
