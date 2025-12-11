# Mooneye Test Analysis: intr_2_0_timing

## Test Overview

The `intr_2_0_timing` test validates the precise timing between STAT mode 2 (OAM scan) and STAT mode 0 (H-blank) interrupts on the Game Boy PPU. The test measures how many M-cycles elapse from the moment a mode 2 STAT interrupt is triggered until a mode 0 STAT interrupt is triggered on the same scanline. According to the test comments, this test has been verified to pass on all official Game Boy hardware: DMG, MGB, SGB, SGB2, CGB, AGB, and AGS.

## What The Test Does

### Step-by-Step Execution Flow

The test performs two measurement iterations with slightly different delays to verify timing precision:

#### First Iteration (with 4 NOPs delay):
1. **Setup for Mode 2 interrupt:**
   - Wait for scanline LY=$42
   - Wait for mode 0 (H-blank) to complete
   - Wait for mode 3 (drawing) to complete
   - Write $20 (0b00100000) to STAT register - enables mode 2 (OAM scan) interrupt only
   - Clear interrupt flags (IF register = 0)
   - Enable interrupts (EI instruction)

2. **Wait for Mode 2 interrupt:**
   - Execute HALT instruction
   - CPU waits in halt mode until STAT interrupt fires
   - When mode 2 interrupt triggers, CPU exits halt and executes NOP
   - Then jumps to STAT interrupt handler at $0048
   - Interrupt handler executes: `add sp,+2` then `ret`
   - This skips the return address, causing execution to continue after the `jp fail_halt` instruction

3. **Delay 4 NOPs:**
   - Execute 4 NOP instructions (4 M-cycles delay)

4. **Setup for Mode 0 interrupt and count cycles:**
   - Write $08 (0b00001000) to STAT register - enables mode 0 (H-blank) interrupt only
   - Clear interrupt flags
   - Enable interrupts
   - XOR A (clear A register, set B = 0)
   - Enter tight loop: `inc b; jr -` (increment B continuously)
   - This loop counts M-cycles until the mode 0 interrupt fires
   - When mode 0 interrupt triggers, interrupt handler skips the return, breaking the loop
   - Result stored in register D

#### Second Iteration (with 3 NOPs delay):
5. Repeat the same process with only 3 NOP instructions as delay
6. Result stored in register E

#### Final Assertions:
7. **setup_assertions:** Save all CPU registers to high RAM for comparison
8. **assert_d $07:** Expect register D to contain value $07
9. **assert_e $08:** Expect register E to contain value $08
10. **quit_check_asserts:** Print results and check if assertions passed

## What The Test Expects

The test expects very specific timing values:
- **Register D = $07:** With a 4 M-cycle delay between mode 2 and setting up for mode 0, the counter should reach 7 before the mode 0 interrupt fires
- **Register E = $08:** With a 3 M-cycle delay, the counter should reach 8 before the mode 0 interrupt fires

This 1-cycle difference confirms that the test is measuring the exact timing window between mode 2 and mode 0 on the same scanline.

### Timing Analysis:

The test effectively measures: **Time from Mode 2 start to Mode 0 start minus setup overhead**

Breaking down the cycle counts:
- Mode 2 (OAM scan) duration: 80 dots (40 M-cycles)
- Mode 3 (drawing) duration: varies, but minimum ~172 dots (86 M-cycles) for no sprites/scroll
- Total from mode 2 start to mode 0 start: ~252 dots (126 M-cycles)

However, the test has setup overhead after the mode 2 interrupt:
- Interrupt handling: ~5 M-cycles (includes the add sp,+2 and ret)
- Writing to STAT ($08): ~3 M-cycles (LD A, immediate + LDH)
- Clear interrupts: ~2 M-cycles (XOR A + LDH)
- Enable interrupts: ~1 M-cycle (EI)
- XOR A: ~1 M-cycle
- LD B,A: ~1 M-cycle

Total setup: ~13 M-cycles

With 4 NOPs (4 M-cycles): 13 + 4 = 17 M-cycles setup, leaving 126 - 17 = 109 M-cycles... but the test expects 7.

This suggests the measurement starts from a different reference point. Let me reconsider:

**Alternative interpretation:** The test measures from when the mode 0 interrupt setup completes (after EI) to when the mode 0 interrupt actually fires. The `inc b; jr -` loop takes 2 M-cycles per iteration:
- inc b: 1 M-cycle
- jr -: 2 M-cycles (when taken)
Total: 3 M-cycles per loop iteration, but the last one may be interrupted mid-execution.

Actually, looking more carefully:
- `inc b`: 1 M-cycle
- `jr -`: 3 M-cycles when taken

So each complete loop is 4 M-cycles. If B=7, that's 7 increments, which could represent different timing based on when the interrupt fires during the loop.

The actual expectation is: **The time remaining from the setup completion to mode 0 interrupt varies by exactly 1 M-cycle based on the initial delay**, confirming cycle-accurate timing.

## What The Test Is Testing

This test validates:

1. **PPU Mode Transition Timing:** The exact number of dots/cycles for mode 2 (OAM scan) and mode 3 (drawing) phases
2. **STAT Interrupt Timing:** STAT interrupts fire at the exact cycle when the mode changes, not delayed
3. **Mode 2 Duration:** OAM scan should take exactly 80 dots (40 M-cycles)
4. **Mode 3 Duration:** Drawing phase duration (varies based on sprite/scroll, but must be consistent)
5. **Interrupt Latency:** The delay from when the interrupt condition becomes true to when the CPU enters the interrupt handler

The test specifically validates the Game Boy's PPU scanline timing with no sprites, no scroll, and no window - the simplest case that should have predictable, fixed timing.

## Potential Failure Reasons

### 1. PPU Mode Transition Timing Issues

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java`

**Issue:** The PPU uses fixed cycle counts for OAM scan but variable timing for drawing.

```java
private Step oamScan() {
    oamScanController.performOneClockCycle();
    count++;
    return count < 40*2 ? Step.OAM_SCAN : Step.SCANLINE_SETUP;
}
```

The OAM scan is correctly set to 80 t-cycles (40 M-cycles). However, the transition to mode 3 happens immediately via `setupScanline()` which calls `displayInterruptController.sendDrawing()`. This is correct.

**Potential Issue:** The drawing phase duration may not be accurate. The test expects no sprites, no scroll (SCX=0), and no window. In this case, mode 3 should have a predictable duration.

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

The drawing completes when `scanlineController.drawingComplete()` returns true. The duration depends on the background fetcher and FIFO pipeline.

### 2. STAT Interrupt Timing

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/DisplayInterruptController.java`

**Issue:** STAT interrupts are triggered via the "STAT blocking" mechanism with rising edge detection:

```java
private void checkAndTriggerStatInterrupt() {
    boolean newStatLine = activeModeLine.isPresent() || lycLine;

    if (newStatLine && !statLine) {  // only trigger on rising edge
        interruptController.setInterrupt(Interrupt.STAT);
    }

    statLine = newStatLine;
}
```

**Mode 2 Interrupt:**
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

**Mode 0 Interrupt:**
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

These look correct. The interrupts fire immediately when the mode changes.

### 3. Mode 3 (Drawing) Clears Active Mode Line

**Critical Observation:**
```java
public void sendDrawing() {
    registers.setStatMode(StatParser.PpuMode.DRAWING);

    // Mode 3 (Drawing) has no STAT interrupt, so clear the active mode line.
    // This allows the internal STAT IRQ signal to go low (if LYC line is also low),
    // enabling new STAT interrupts when the next mode with an enabled interrupt begins.
    activeModeLine = Optional.empty();

    // Update the internal stat line state (important for STAT blocking)
    updateStatLine();
}
```

This correctly implements STAT blocking by allowing the STAT line to go low during mode 3, enabling a rising edge when mode 0 begins.

### 4. Drawing Phase Duration - The Most Likely Issue

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/ScanlineController.java`

The drawing phase uses a FIFO pipeline with background fetcher. For the test case (SCX=0, no sprites, no window), the timing should be:
- Initial fetch to fill FIFO: ~8 t-cycles per tile fetch step (5 steps = ~40 t-cycles)
- Pixel output: 160 pixels at 1 t-cycle each = 160 t-cycles
- But the fetcher continues fetching while outputting...

The actual drawing duration depends on:
1. How many pixels need to be discarded based on SCX
2. The FIFO fetcher timing
3. Whether sprites are processed

```java
private boolean shouldDiscardPixel() {
    int pixelsToDiscard = mod(registers.read(SCX), 8);
    int discardedPixels = (8 - backgroundFifo.size());
    return discardedPixels % 8 != pixelsToDiscard;
}
```

If SCX=0, `pixelsToDiscard = 0`, so no pixels should be discarded. The test waits for mode 0 and mode 3 to complete before starting, ensuring we're at the beginning of a fresh scanline.

**Potential Problem:** The exact cycle count for mode 3 may not match hardware. On real hardware, mode 3 for a simple scanline (no sprites, SCX=0) typically takes:
- Minimum: ~172 dots (86 M-cycles)
- This varies based on implementation details

The emulator's FIFO implementation may take a different number of cycles.

### 5. Interrupt Handling Timing

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/HardwareInterrupt.java`

```java
public static void callInterruptHandler(CpuStructure cpuStructure, Interrupt interrupt) {
    cpuStructure.registers().setIME(false);
    cpuStructure.interruptBus().deactivateInterrupt(interrupt);

    cpuStructure.registers().setPC(cpuStructure.idu().decrement(cpuStructure.registers().PC()));

    cpuStructure.clock().tick();

    ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());

    cpuStructure.registers().setPC(interrupt.getInterruptHandlerAddress());
    byte nextInstruction = ControlFlow.readIndirectPCAndIncrement(cpuStructure);
    cpuStructure.registers().setInstructionRegister(nextInstruction);
}
```

The interrupt handling includes:
- 1 clock tick explicitly
- Push to stack (which takes 2 M-cycles internally via ControlFlow.pushToStack)
- Fetch next instruction

The total interrupt overhead should be 5 M-cycles, which matches hardware behavior.

### 6. HALT Instruction Timing

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Halt.java`

```java
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

**Issue:** The `waitForInterrupt()` method ticks the clock until an interrupt is pending:

```java
public void waitForInterrupt() {
    while (!hasInterrupts()) {
        clock.tick();
    }
}
```

This means HALT releases on the **same M-cycle** that the interrupt becomes pending. On real hardware, there's a specific timing relationship:
- HALT releases when an interrupt is pending (IF & IE != 0)
- The interrupt is handled 1 M-cycle after EI completes (if IME=1)
- There's a 1 M-cycle delay after HALT before the next instruction

**Potential Issue:** The HALT may not have the correct timing for when it exits relative to when the interrupt flag is set. On real hardware, HALT exits when the interrupt condition becomes true, but the interrupt handler is called 1 M-cycle later (after the NOP following HALT completes).

The test has:
```assembly
halt
nop
jp fail_halt
```

The expected flow is:
1. HALT executes and waits
2. Mode 2 interrupt flag is set
3. HALT releases
4. NOP executes (1 M-cycle)
5. Interrupt is serviced (before JP executes)

The emulator's implementation should match this, but the exact cycle of interrupt servicing relative to HALT release may be off.

## Summary of Most Likely Issues

1. **Mode 3 (Drawing) Duration:** The FIFO-based drawing implementation may not produce the exact cycle count that real hardware does for simple scanlines (no sprites, SCX=0, no window). The test expects a very specific duration.

2. **HALT Exit Timing:** The relationship between when the interrupt flag is set, when HALT releases, and when the interrupt is actually serviced may not precisely match hardware behavior.

3. **Instruction Fetch After Interrupt:** The exact cycle count for fetching and executing the first instruction after the interrupt handler returns may differ from hardware.

To diagnose further, the emulator would need to be run with cycle-level logging to see:
- Exactly when mode 2 starts (which cycle)
- Exactly when mode 3 starts
- Exactly when mode 0 starts
- When the STAT interrupts are set in the IF register
- When the CPU services each interrupt
- The actual values in registers D and E

The 1-cycle sensitivity of this test (D=7 vs E=8 with only 1 NOP difference) indicates that the emulator needs cycle-perfect PPU timing to pass.
