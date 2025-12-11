# di_timing-GS Test Analysis

## Test Overview

The `di_timing-GS` test verifies that the DI (Disable Interrupts) instruction takes immediate effect on DMG/MGB Game Boy models. The test is specifically designed to fail on CGB/GBA models where DI has a one-instruction delay before taking effect. The test validates precise interrupt timing behavior by testing whether interrupts can occur immediately after a DI instruction.

## What The Test Does

### Initial Setup
1. **Disable interrupts** with `DI`
2. **Enable VBLANK interrupt** by writing `INTR_VBLANK` (0x01) to IE register (0xFFFF)
3. **Set interrupt handler** to `test_round1` by loading address into HL

### Round 1: Testing Interrupt Arrival Before DI
1. **Wait for VBLANK** by polling LY register until it reaches 143, then 144
2. **Clear interrupt flags** by writing 0 to IF register (0xFF0F)
3. **Enable interrupts** with `EI`
4. **Enter HALT mode** which waits for an interrupt
5. After HALT, execute `NOP` then jump to `fail_halt` (should never execute)

**Interrupt Handler (test_round1):**
- Sets next interrupt handler to `finish_round1` (HL register)
- Enables interrupts with `EI`
- Delays for: `2505 * 7 + 5 = 17540` cycles (via `delay_long_time 2505`)
- Adds 6 more NOPs: `6 * 4 = 24` cycles
- **Total delay: 17564 cycles**
- Executes `DI` instruction (should NOT execute - if reached, test fails)
- Jumps to `fail_round1`

**Expected:** The VBLANK interrupt should fire BEFORE the DI instruction is reached, jumping to `finish_round1` instead.

### Round 2: Testing DI Takes Immediate Effect
1. **Wait for next VBLANK**
2. **Clear interrupt flags**
3. **Enable interrupts** with `EI`
4. **Enter HALT mode**
5. After HALT, execute `NOP` then jump to `fail_halt` (should never execute)

**Interrupt Handler (test_round2):**
- Sets next interrupt handler to `fail_round2` (HL register)
- Enables interrupts with `EI`
- Delays for: `2505 * 7 + 5 = 17540` cycles
- Adds **5 NOPs** (one less than round 1): `5 * 4 = 20` cycles
- **Total delay: 17560 cycles** (4 cycles less than round 1)
- **Executes DI instruction** (this time it should execute)
- **Critical test point:** Executes `NOP` immediately after DI
- If an interrupt occurs at this NOP, the test fails by jumping to `fail_round2`

### Success Condition
- Test succeeds with `quit_ok` if it reaches `test_finish` label
- This means DI disabled interrupts immediately, preventing the VBLANK interrupt during the NOP

### Interrupt Vector
- At address 0x0040 (VBLANK interrupt vector): `JP HL`
- This jumps to whatever address is currently in HL register

## What The Test Expects

### Round 1 Expectations
- VBLANK interrupt should fire during the delay period
- The interrupt should occur BEFORE the DI instruction at line 56
- Execution should jump to `finish_round1` via the interrupt handler
- The DI at line 56 should NEVER execute

### Round 2 Expectations
- VBLANK interrupt should fire during the delay period, causing execution to jump to `test_round2` handler
- The DI instruction at line 78 SHOULD execute (4 cycles earlier than round 1)
- **Critical:** DI must take immediate effect on the next instruction
- The NOP at line 81 should execute WITHOUT triggering an interrupt
- No interrupt should occur between DI and test completion
- Test should reach `test_finish` and pass

### Timing Precision
The test relies on precise timing:
- Round 1 uses 6 NOPs before DI
- Round 2 uses 5 NOPs before DI (exactly 4 cycles difference)
- This 4-cycle window is where the VBLANK interrupt is expected to arrive
- If DI has immediate effect, the interrupt is blocked at the NOP
- If DI has delayed effect (CGB behavior), the interrupt fires at the NOP and test fails

## What The Test Is Testing

### Primary Behavior: DI Immediate Effect (DMG/MGB)
The test validates that on DMG/MGB hardware:
- **DI instruction disables interrupts IMMEDIATELY**
- The very next instruction after DI cannot be interrupted
- IME (Interrupt Master Enable) flag is set to false during DI execution
- No delay exists between DI execution and interrupt masking

### Contrasted with CGB/GBA Behavior
On CGB/GBA hardware (which this test is NOT for):
- DI has a one-instruction delay
- The instruction immediately after DI can still be interrupted
- IME is not set to false until after the next instruction completes

### Interrupt Timing Precision
The test also validates:
1. **EI timing**: EI enables interrupts with a one-instruction delay (the HALT after EI demonstrates this)
2. **HALT behavior**: HALT waits for interrupts and resumes execution after handling
3. **Interrupt handling**: Interrupts are properly triggered, handled, and can return via JP HL
4. **VBLANK timing**: VBLANK interrupts fire at the expected time during frame rendering

## Potential Failure Reasons

### 1. DI Has Delayed Effect (Most Likely Issue)

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/DisableInterrupts.java`

**Current Implementation:**
```java
@Override
public void execute(CpuStructure cpuStructure) {
    byte ir = cpuStructure.memory().read(cpuStructure.registers().PC());
    cpuStructure.registers().setInstructionRegister(ir);
    ControlFlow.incrementPC(cpuStructure);
    cpuStructure.registers().setIME(false);  // IME set to false during execution

    cpuStructure.clock().tick();
}
```

**Analysis:**
The implementation sets IME to false during the execute phase. Looking at the CPU cycle in `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/Cpu.java`:

```java
public void cycle() {
    Instruction instruction = decode(cpuStructure.registers().instructionRegister());
    instruction.execute(cpuStructure);
    fetch_cycle(instruction);
}

private void fetch_cycle(Instruction instruction) {
    if (!instruction.handlesFetch()) {
        fetch();
        cpuStructure.clock().tick();
        handlePotentialInterrupt();  // This checks IME
        instruction.postFetch(cpuStructure);
    }
}
```

The DI instruction's `handlesFetch()` returns `true`, so it skips the fetch_cycle entirely. This means interrupts are NOT checked after DI completes, which is correct for immediate effect.

**Potential Problem:** None detected in DI implementation - it appears correct for DMG behavior.

### 2. EI Has Incorrect Timing

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/EnableInterrupts.java`

**Current Implementation:**
```java
@Override
public void execute(CpuStructure cpuStructure) {
    // Empty - does nothing during execute
}

@Override
public void postFetch(CpuStructure cpuStructure) {
    cpuStructure.registers().setIME(true);  // IME set in postFetch
}
```

**Analysis:**
EI sets IME in the `postFetch` phase, which happens AFTER the interrupt check. Looking at the CPU cycle:

```java
private void fetch_cycle(Instruction instruction) {
    if (!instruction.handlesFetch()) {
        fetch();
        cpuStructure.clock().tick();
        handlePotentialInterrupt();  // Interrupt check happens BEFORE postFetch
        instruction.postFetch(cpuStructure);  // IME set here
    }
}
```

This means:
1. EI executes
2. Next instruction is fetched
3. Interrupt check occurs (IME still false)
4. IME is set to true in postFetch
5. Next cycle begins

**Result:** This is CORRECT! EI should have a one-instruction delay. The instruction immediately after EI cannot be interrupted.

### 3. HALT Behavior After Interrupts

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Halt.java`

**Current Implementation:**
```java
@Override
public void execute(CpuStructure cpuStructure) {
    boolean IME = cpuStructure.registers().IME();
    boolean interruptsPending = cpuStructure.interruptBus().hasInterrupts();

    if (!IME && interruptsPending) {
        cpuStructure.idu().disableNextIncrement();  // HALT bug
    }

    cpuStructure.clock().stop();
    cpuStructure.interruptBus().waitForInterrupt();
    cpuStructure.clock().start();
}
```

**Analysis:**
The HALT implementation handles the HALT bug correctly (when IME is false but interrupts are pending). The test uses HALT with IME enabled, so this shouldn't affect the test.

**Potential Problem:** None detected.

### 4. Interrupt Handling Timing

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/Cpu.java`

**Current Implementation:**
```java
private void handlePotentialInterrupt() {
    if (cpuStructure.registers().IME() && cpuStructure.interruptBus().hasInterrupts()) {
        Interrupt highestPriorityInterrupt = cpuStructure.interruptBus().activeInterrupts().getFirst();
        HardwareInterrupt.callInterruptHandler(cpuStructure, highestPriorityInterrupt);
    }
}
```

**Analysis:**
Interrupts are checked during the fetch cycle, between fetch and postFetch. This happens for every instruction that doesn't handle its own fetch (most instructions). The check correctly verifies both IME and pending interrupts.

**Potential Problem:** None detected in the check itself.

### 5. Interrupt Priority Order

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/components/InterruptBus.java`

**Current Implementation:**
```java
private static final List<Interrupt> INTERRUPT_PRIORITY = List.of(
    Interrupt.JOYPAD,
    Interrupt.SERIAL,
    Interrupt.TIMER,
    Interrupt.STAT,
    Interrupt.VBLANK
);
```

**Analysis:**
The interrupt priority list shows VBLANK as the LOWEST priority. This is **INCORRECT**!

**Game Boy Interrupt Priority (highest to lowest):**
1. VBLANK (bit 0) - highest priority
2. STAT (bit 1)
3. TIMER (bit 2)
4. SERIAL (bit 3)
5. JOYPAD (bit 4) - lowest priority

**Potential Problem:** The priority order is completely reversed! However, this test only enables VBLANK interrupts, so priority shouldn't matter for this specific test.

### 6. Critical Issue: Fetch Cycle After DI

**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/DisableInterrupts.java`

**Current Implementation:**
```java
@Override
public boolean handlesFetch() {
    return true;  // DI handles its own fetch
}

@Override
public void execute(CpuStructure cpuStructure) {
    byte ir = cpuStructure.memory().read(cpuStructure.registers().PC());
    cpuStructure.registers().setInstructionRegister(ir);
    ControlFlow.incrementPC(cpuStructure);
    cpuStructure.registers().setIME(false);

    cpuStructure.clock().tick();
}
```

**Analysis:**
DI returns `handlesFetch() = true`, which means:
1. DI executes completely in its execute() method
2. It fetches the next instruction itself
3. It increments PC
4. It sets IME to false
5. The fetch_cycle() is SKIPPED entirely

This means the next instruction is already fetched and loaded into the instruction register BEFORE IME is set to false. Let's trace the sequence:

**DI Execution:**
1. Current IR contains DI opcode (0xF3)
2. DI.execute() is called
3. Inside execute(): Reads next instruction from PC
4. Inside execute(): Loads it into IR
5. Inside execute(): Increments PC
6. Inside execute(): Sets IME to false
7. Inside execute(): Ticks clock
8. Returns to Cpu.cycle()

**Next CPU Cycle:**
1. Cpu.cycle() is called again
2. Decodes the instruction already in IR (the one after DI)
3. Executes that instruction
4. If that instruction doesn't handle fetch, goes to fetch_cycle()
5. In fetch_cycle(): handlePotentialInterrupt() is called
6. This is where IME is checked - it's now false, so no interrupt

**The Problem:**
The instruction immediately after DI does NOT get an interrupt check before it executes! The interrupt check happens DURING the fetch cycle of the instruction AFTER the one following DI.

**Example Sequence:**
```
DI      <- Sets IME=false, fetches NOP
NOP     <- Executes, no interrupt check, fetches ADD
ADD     <- Executes, interrupt check happens here (but IME already false)
```

In the test, after DI at line 78:
```assembly
di      <- Sets IME=false, fetches NOP
nop     <- Executes without interrupt check!
```

**This is actually CORRECT for DMG!** On DMG, DI takes immediate effect, so the NOP should not be interruptible. The test should pass if VBLANK timing is correct.

### 7. Most Likely Failure: VBLANK Interrupt Timing

**Root Cause:** The test is extremely timing-sensitive. The failure is most likely because:

1. **VBLANK interrupt is not firing at the precise cycle expected**
   - The test expects the interrupt to fire in a 4-cycle window
   - Round 1: interrupt should fire BEFORE cycle 17564
   - Round 2: interrupt should fire AT cycle 17560 (during the DI or just before)

2. **PPU timing may be off by a few cycles**
   - If VBLANK fires too early, Round 1 might fail
   - If VBLANK fires too late, Round 2 might get interrupted after DI
   - The test has only a 4-cycle margin of error

3. **Clock synchronization between CPU and PPU**
   - Located in: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/`
   - The PPU runs at 4x CPU speed via ClockWithParallelProcess
   - Any timing drift could cause this test to fail

**Recommendation:** Check the VBLANK interrupt timing in the PPU implementation. The interrupt should fire at LY=144, Mode 1 (VBLANK mode), at precisely the right M-cycle.

## Summary

The test validates that DI takes immediate effect on DMG hardware by:
1. Setting up a scenario where a VBLANK interrupt is expected in a narrow timing window
2. Executing DI just before the interrupt should arrive
3. Verifying that the instruction after DI is NOT interrupted

The emulator's DI implementation appears correct for DMG behavior. The most likely failure reason is **VBLANK interrupt timing precision** - the interrupt may be firing slightly too early or too late, outside the 4-cycle window the test depends on. The test requires frame-perfect timing accuracy to pass.
