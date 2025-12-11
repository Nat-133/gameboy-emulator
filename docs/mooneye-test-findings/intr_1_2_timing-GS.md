# Mooneye Test Analysis: intr_1_2_timing-GS

## Test Overview
This test measures the precise timing between a Mode 1 (VBlank) STAT interrupt and a Mode 2 (OAM scan) STAT interrupt on the Game Boy PPU. It verifies the number of cycles that can be executed between receiving the Mode 1 interrupt and receiving the Mode 2 interrupt.

**Test Compatibility:**
- Pass: DMG, MGB, SGB, SGB2
- Fail: CGB, AGB, AGS

## What The Test Does

### Step-by-Step Execution Flow

1. **Setup Phase:**
   - Set stack pointer to DEFAULT_SP (0xE000)
   - Wait for VBlank
   - Store STAT register address in HL
   - Enable STAT interrupts globally by writing INTR_STAT (0x02) to IE register

2. **First Test Iteration (with 5 NOPs delay):**
   - Call `setup_and_wait_mode1`:
     - Wait until LY reaches line 66 (0x42) - this is during normal rendering
     - Write 0x10 to STAT register (enables Mode 1/VBlank STAT interrupt)
     - Clear interrupt flags (IF register)
     - Execute `ei` to enable interrupts
     - Execute `halt` - CPU stops and waits for interrupt
     - When Mode 1 interrupt fires (at start of VBlank, line 144):
       - CPU wakes from HALT
       - Interrupt handler is called (INTR_VEC_STAT at 0x48)
       - Handler executes `add sp,+2` (removes interrupt return address)
       - Handler executes `ret` (returns to caller of setup_and_wait_mode1)
   - Execute 5 NOPs (delay)
   - Call `setup_and_wait_mode2`:
     - Write 0x20 to STAT register (enables Mode 2/OAM scan STAT interrupt)
     - Clear interrupt flags
     - Execute `ei` to enable interrupts
     - Set B register to 0
     - Loop: increment B repeatedly (`inc b; jr -`)
     - When Mode 2 interrupt fires (at start of next scanline's OAM scan):
       - Interrupt handler is called
       - Handler returns to caller
   - Store B register value in D register

3. **Second Test Iteration (with 4 NOPs delay):**
   - Same process as above but with only 4 NOPs delay
   - Store B register value in E register

4. **Assertion Phase:**
   - Call `setup_assertions`
   - Assert that D register equals 0x14 (20 decimal)
   - Assert that E register equals 0x15 (21 decimal)
   - Exit test with `quit_check_asserts`

### Key Timing Details

The test is measuring how many times the B register can be incremented in the loop between:
- The point where Mode 2 interrupt is enabled (after the NOPs)
- The point where Mode 2 interrupt actually fires

The difference of 1 between the two expected values (0x14 vs 0x15) shows that removing one NOP (4 cycles) allows exactly one more `inc b` instruction (1 M-cycle = 4 T-cycles) to execute before the interrupt.

## What The Test Expects

**Expected Register Values:**
- **D register = 0x14 (20 decimal)** - Result from iteration with 5 NOPs delay
- **E register = 0x15 (21 decimal)** - Result from iteration with 4 NOPs delay

These values represent the exact number of times the counter can increment between enabling the Mode 2 STAT interrupt and receiving it.

## What The Test Is Testing

This test validates the **precise PPU timing** for STAT interrupts, specifically:

1. **Mode Transition Timing:** The exact duration of Mode 1 (VBlank) before transitioning to Mode 2 (OAM scan)
2. **STAT Interrupt Latency:** How quickly STAT interrupts are triggered when a mode transition occurs
3. **Cycle-Accurate Behavior:** The test is extremely sensitive to timing - a difference of 4 T-cycles (1 M-cycle) changes the result by 1

### What Must Work Correctly

1. **VBlank Duration:** VBlank (Mode 1) must last exactly the right number of scanlines (10 scanlines, from LY=144 to LY=153)
2. **Scanline Timing:** Each scanline must take exactly 456 dots (114 M-cycles)
3. **Mode 2 Start Timing:** Mode 2 (OAM scan) must begin at precisely the right cycle within the scanline
4. **STAT Interrupt Timing:** When STAT mode bits change, the interrupt must be triggered with correct latency
5. **LY Increment Timing:** LY register must increment at the exact right moment in the scanline cycle

## Potential Failure Reasons

### 1. VBlank Scanline Count or Duration

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java` (lines 124-142)

**Current Implementation:**
```java
private Step vblank() {
    clock.tick();
    count++;

    // Increment LY every scanline during VBLANK
    if (count % SCANLINE_TICK_COUNT == 0) {
        int currentLy = uint(registers.read(LY));
        if (currentLy < 153) {
            updateLY((byte) (currentLy + 1));
        }
    }

    if (count < SCANLINE_TICK_COUNT * 10) {
        return Step.VBLANK;
    }

    updateLY((byte) 0);
    scanlineController.resetForNewFrame();
    return Step.OAM_SETUP;
}
```

**Issue:** The VBlank duration is `SCANLINE_TICK_COUNT * 10` = 456 * 10 = 4560 dots. This represents 10 complete scanlines of VBlank.

**Analysis:** VBlank spans lines 144-153 (10 lines), which is correct. However, the timing of when line 0 (the first line with Mode 2) actually starts may be off.

### 2. Mode Transition to OAM Scan Timing

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java` (lines 140-142 and 72-77)

**Current Implementation:**
When VBlank ends:
```java
updateLY((byte) 0);           // Set LY to 0
scanlineController.resetForNewFrame();
return Step.OAM_SETUP;        // Return to OAM setup
```

Then on next tCycle:
```java
private Step setupOamScan() {
    displayInterruptController.sendOamScan();  // Triggers Mode 2 interrupt here
    oamScanController.setupOamScan(uint(registers.read(LY)));
    count = 0;
    return oamScan();
}
```

**Issue:** The Mode 2 interrupt is sent via `sendOamScan()` which happens in the `setupOamScan()` step. The timing depends on when this happens relative to LY being set to 0.

### 3. STAT Interrupt Trigger Timing

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/DisplayInterruptController.java` (lines 41-50, 130-138)

**Current Implementation:**
```java
public void sendOamScan() {
    registers.setStatMode(StatParser.PpuMode.OAM_SCANNING);

    byte stat = registers.read(PpuRegister.STAT);
    activeModeLine = StatParser.oamInterruptEnabled(stat)
            ? Optional.of(ActiveModeLine.OAM)
            : Optional.empty();

    checkAndTriggerStatInterrupt();
}

private void checkAndTriggerStatInterrupt() {
    boolean newStatLine = activeModeLine.isPresent() || lycLine;

    if (newStatLine && !statLine) {  // this is for stat blocking, only trigger on rising edge
        interruptController.setInterrupt(Interrupt.STAT);
    }

    statLine = newStatLine;
}
```

**Analysis:** The interrupt is triggered via `interruptController.setInterrupt()` which sets a bit in the IF register. The CPU checks for interrupts during the fetch cycle after executing an instruction.

### 4. Interrupt Servicing Timing

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/Cpu.java` (lines 40-45)

**Current Implementation:**
```java
private void handlePotentialInterrupt() {
    if (cpuStructure.registers().IME() && cpuStructure.interruptBus().hasInterrupts()) {
        Interrupt highestPriorityInterrupt = cpuStructure.interruptBus().activeInterrupts().getFirst();
        HardwareInterrupt.callInterruptHandler(cpuStructure, highestPriorityInterrupt);
    }
}
```

This is called during fetch cycle (line 28 in cycle() method), after the instruction executes and a clock tick.

**Issue:** The timing of when an interrupt is detected and serviced affects the test results. If there's any delay in interrupt servicing, it could change the B register count.

### 5. PPU to CPU Clock Synchronization

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/ClockWithParallelProcess.java`

**Current Implementation:**
```java
@Override
public void tick() {
    parallelProcess.run();  // This runs PPU tCycle
    time ++;
}
```

**Analysis:** The PPU runs at 4x CPU clock speed. Every CPU M-cycle (4 T-cycles), the clock ticks 4 times, each time calling the PPU's tCycle(). This means for every `inc b` instruction that executes (1 M-cycle), the PPU advances by 4 dots.

### 6. LY Update and Mode Transition Synchronization

**Key Issue:** The test is extremely timing-sensitive. The exact cycle when:
- LY increments from 153 to 0
- Mode transitions from Mode 1 to Mode 2
- STAT interrupt is triggered
- CPU detects and services the interrupt

All of these must occur at precisely the right moments to match hardware behavior.

### 7. Possible Off-by-One Errors in Scanline Counting

Looking at line 140-142 in PictureProcessingUnit.java:
```java
updateLY((byte) 0);
scanlineController.resetForNewFrame();
return Step.OAM_SETUP;
```

The LY is set to 0, then on the NEXT tCycle call, `setupOamScan()` is called which sends the Mode 2 interrupt. This means there's a delay between LY=0 being set and Mode 2 actually starting.

**Potential Issue:** On real hardware, the Mode 2 interrupt might be triggered at a different relative position to when LY=0 is set. The test might be failing because the emulator triggers the Mode 2 interrupt one or more cycles too early or too late.

### 8. VBlank to Line 0 Transition Timing

The transition from the last line of VBlank (LY=153) to the first line (LY=0) might not be happening at exactly the right cycle. The real hardware might have specific behavior for:
- When LY changes from 153 to 0
- How many cycles of "line 153" there are
- When Mode 1 ends and the PPU prepares for Mode 2

**Critical Timing:** Line 153 on real hardware is shorter than other lines (only 456 dots before wrapping to line 0), but the exact timing of when OAM scan starts on line 0 is crucial.

### Summary of Most Likely Issues

1. **Mode 2 interrupt trigger timing relative to LY=0** - The interrupt might be firing too early or too late
2. **VBlank line 153 duration** - Line 153 might have incorrect duration
3. **Scanline transition timing** - The exact cycle when transitioning from one line to the next
4. **STAT mode change and interrupt latency** - Delay between mode bit change and IF flag being set

The test expects exactly 0x14 or 0x15 loop iterations depending on a 4-cycle delay difference, which means the emulator's timing is likely off by a small but measurable amount - possibly as little as 1-4 cycles at the critical Mode 1 to Mode 2 transition point.
