# halt_ime0_nointr_timing Test Analysis

## Test Overview

This test validates that the HALT instruction with IME=0 (interrupts disabled) does not add any timing delay when no interrupts are pending. It ensures that HALT simply stops the CPU until an interrupt is requested, with the same timing as if NOP instructions were used to wait.

## What The Test Does

The test performs two rounds of timing measurements to verify HALT behavior:

### Setup (Lines 43-45)
1. **Disable interrupts**: `di` sets IME=0
2. **Wait for scanline 10**: `wait_ly 10` ensures a predictable starting state
3. **Enable VBLANK interrupt**: Sets IE register to enable VBLANK interrupts (bit 0)

### Round 1: HALT with IME=1 (Baseline) (Lines 47-68)

1. **Clear IF**: Sets interrupt flags register to 0
2. **Set interrupt handler**: `ld hl, test_round1` - sets HL to point to the next test section
3. **Enable interrupts and HALT**: `ei` then `halt` then `nop`
   - The `ei` enables interrupts (IME=1)
   - The `halt` stops the CPU
   - When VBLANK interrupt occurs, the CPU jumps to address $0040 (VBLANK handler at line 103-104)
   - The handler executes `jp hl`, jumping to `test_round1`
   - The `nop` after halt should never execute (if it does, the test jumps to `fail_halt`)

4. **In test_round1** (lines 55-68):
   - Set fail handler: `ld hl, fail_intr` (should not be needed)
   - Clear IF register
   - Execute 13 NOPs for timing alignment
   - Reset DIV timer: `xor a; ldh (<DIV), a` - clears DIV to 0
   - **HALT with IME=0**: Execute `halt` instruction
   - Execute 6 NOPs - these represent the timing equivalent to "interrupt dispatch + JP HL" in IME=1 case
   - Read DIV register into D register: This captures the time elapsed

### Round 2: HALT with IME=1 again (Lines 70-95)

5. **Repeat similar process** (lines 70-76):
   - Clear IF
   - Set interrupt handler to `test_round2`
   - Enable interrupts and HALT
   - Wait for VBLANK interrupt

6. **In test_round2** (lines 78-95):
   - Set fail handler: `ld hl, fail_intr`
   - Clear IF register
   - Execute **12 NOPs** (one less than round 1) for timing alignment
   - Reset DIV timer: `xor a; ldh (<DIV), a`
   - **HALT with IME=0**: Execute `halt`
   - Execute 6 NOPs
   - Read DIV register into E register

7. **Assertions**:
   - `assert_d $11` - D register should be 0x11
   - `assert_e $12` - E register should be 0x12

## What The Test Expects

The test expects specific DIV values after each round:
- **Round 1**: DIV should equal `0x11` (17 decimal)
- **Round 2**: DIV should equal `0x12` (18 decimal)

The difference of 1 between rounds comes from executing one fewer NOP instruction before resetting DIV in round 2 (12 vs 13 NOPs).

### Why these values?

The DIV register increments at 16384 Hz (every 256 T-cycles or 64 M-cycles). The test carefully counts the number of M-cycles between clearing DIV and reading it:

**Round 1 cycle count**:
- `xor a`: 1 M-cycle
- `ldh (<DIV), a`: 3 M-cycles (writes happen on cycle 3)
- `halt`: 1 M-cycle (when IME=0 and interrupt pending, HALT exits immediately)
- 6 × `nop`: 6 M-cycles
- `ldh a, (<DIV)`: 3 M-cycles (reads happen on cycle 3)
- **Total**: ~17 M-cycles of DIV increment before read

**Round 2 cycle count**:
- Same as round 1, but starts 1 M-cycle later (one fewer NOP before clearing DIV)
- **Total**: ~18 M-cycles of DIV increment before read

The key insight: **HALT with IME=0 and a pending interrupt should execute in exactly 1 M-cycle**, with no extra delay. The 6 NOPs after HALT are meant to simulate the interrupt dispatch overhead that would occur with IME=1.

## What The Test Is Testing

This test validates the **HALT instruction timing behavior** in the IME=0 (interrupts disabled) case when interrupts are pending:

1. **HALT should not add delay**: When IME=0 and an interrupt is pending (IF & IE != 0), HALT should exit immediately on the next instruction, taking exactly 1 M-cycle
2. **DIV timer continues during HALT**: The DIV register must continue to increment normally during the HALT instruction
3. **Consistent timing**: The HALT instruction should have the same timing whether executed with IME=0 or IME=1 (both take 1 M-cycle to execute)

### Game Boy HALT Behavior

The Game Boy has two HALT behaviors:
- **IME=1**: HALT stops CPU until an interrupt occurs, then the interrupt handler is called
- **IME=0 with pending interrupt**: HALT exits immediately after 1 M-cycle (this test verifies this case)
- **IME=0 without pending interrupt**: HALT stops CPU until an interrupt is requested (IF & IE becomes non-zero), then continues at the next instruction

## Potential Failure Reasons

### 1. Timer Not Running During HALT (LIKELY ISSUE)

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Halt.java` (lines 23-25)

```java
cpuStructure.clock().stop();
cpuStructure.interruptBus().waitForInterrupt();
cpuStructure.clock().start();
```

**Problem**: The `clock().stop()` call appears to be unused (both `CpuClock.stop()` and `ClockWithParallelProcess.stop()` are empty methods), but the issue is that during `waitForInterrupt()`, the timer increments are tied to `clock.tick()`:

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/components/InterruptBus.java` (lines 55-59)

```java
public void waitForInterrupt() {
    while (!hasInterrupts()) {
        clock.tick();
    }
}
```

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/ClockWithParallelProcess.java` (lines 13-16)

```java
@Override
public void tick() {
    parallelProcess.run();  // This calls timer.mCycle()
    time ++;
}
```

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/CpuModule.java` (lines 60-67)

```java
return new ClockWithParallelProcess(() -> {
    timer.mCycle();
    serialController.mCycle();
    dmaController.mCycle();
    for (int i = 0; i < 4; i++) {
        ppu.tCycle();
    }
});
```

**Analysis**: The timer DOES run during HALT because `waitForInterrupt()` calls `clock.tick()` which calls `timer.mCycle()`. This is actually correct behavior - the DIV register should continue incrementing during HALT.

### 2. HALT Execution Timing (CONFIRMED CORRECT)

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Halt.java` (lines 15-26)

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

**Analysis**: When IME=0 and interrupts are pending, the code correctly calls `disableNextIncrement()`, which implements the HALT bug behavior. The HALT instruction itself executes and then `waitForInterrupt()` immediately returns (since interrupts are pending), so HALT takes 1 M-cycle as expected.

### 3. DIV Register Timing (POTENTIAL ISSUE)

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/components/InternalTimerCounter.java` (lines 15-28)

```java
public void tCycle() {
    int oldValue = counter;
    counter = (counter + 1) & 0xFFFF;

    // Detect falling edges: bits that were 1 and are now 0
    int fallingEdges = oldValue & ~counter;

    for (var entry : fallingEdgeListeners.entrySet()) {
        int bit = entry.getKey();
        if ((fallingEdges & (1 << bit)) != 0) {
            entry.getValue().run();
        }
    }
}
```

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/components/DividerRegister.java` (lines 12-20)

```java
@Override
public byte read() {
    return (byte) ((internalCounter.getValue() >> 8) & 0xFF);
}

@Override
public void write(byte value) {
    internalCounter.reset();
}
```

**Analysis**: The DIV register reads the upper 8 bits of the internal 16-bit counter. The internal counter increments once per T-cycle (4 T-cycles = 1 M-cycle). This means DIV increments once every 256 T-cycles = 64 M-cycles.

**Timing calculation**:
- If the test expects DIV to be 0x11 after ~17 M-cycles, but DIV only increments every 64 M-cycles, this doesn't match!
- Actually, looking more carefully: the internal counter starts at 0xAAC8 (from CoreModule line 55)
- After 17 M-cycles = 68 T-cycles, the counter would be at 0xAAC8 + 68 = 0xAB30
- DIV = 0xAB30 >> 8 = 0xAB (not 0x11!)

**Wait, the test resets DIV!**
- The test does `xor a; ldh (<DIV), a` which calls `write()` on DividerRegister
- This calls `internalCounter.reset()` which sets counter to 0
- So after reset, the counter starts at 0x0000

**Re-calculating after DIV reset**:
- After reset, counter = 0x0000
- After 17 M-cycles = 68 T-cycles, counter = 0x0044 (68 decimal)
- DIV = 0x0044 >> 8 = 0x00 (not 0x11!)

### 4. The Core Timing Issue (MOST LIKELY)

The test expects DIV = 0x11 after approximately 17 M-cycles, but:
- DIV increments once every 64 M-cycles (256 T-cycles)
- After 17 M-cycles, DIV should still be 0x00 (or whatever it was at reset)
- To reach DIV = 0x11, you need: 0x11 × 64 = 1088 M-cycles

**This suggests the test is actually measuring MORE time than just the HALT + 6 NOPs!**

Looking back at the test code more carefully:

```assembly
halt
nops 6 ; Equivalent to interrupt + JP HL in the IME=1 case
```

The comment says "equivalent to interrupt + JP HL in the IME=1 case". But in THIS round, IME=0, so no interrupt occurs! The 6 NOPs are meant to compensate for the fact that in the IME=1 baseline case, there's overhead from interrupt dispatch.

**Re-reading the test flow**:

The test is comparing two scenarios:
1. **IME=1 case** (in the outer loop before reaching test_round1/test_round2): HALT → VBLANK interrupt → handler → JP HL → test code
2. **IME=0 case** (within test_round1/test_round2): HALT (exits immediately) → 6 NOPs → continue

The test is verifying that the IME=0 HALT path takes the same amount of time as just executing NOPs, while the system is waiting for the VBLANK interrupt to be triggered naturally.

**Actually, looking at the test again**: The test is waiting for the VBLANK interrupt to occur BEFORE entering the test rounds. Once in test_round1, the IF register is cleared, so there are NO pending interrupts when the second HALT executes!

Let me re-read:
- Line 57: `clear_IF` - clears interrupt flags
- Line 63: `halt` - executes HALT with **no pending interrupts**

**AH! The test is measuring the time it takes for a VBLANK interrupt to occur naturally!**

The test is:
1. Clear IF (no pending interrupts)
2. Reset DIV to 0
3. Execute HALT (IME=0, no pending interrupts) - CPU stops and waits
4. VBLANK interrupt is requested (sets IF bit 0)
5. HALT exits, continues with 6 NOPs
6. Read DIV

The DIV value of 0x11 represents how long it takes for the VBLANK interrupt to naturally occur from the PPU, PLUS the 6 NOPs, PLUS the overhead of reading DIV.

### 5. The Real Issue: IME=0 HALT Without Pending Interrupts

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Halt.java` (lines 19-21)

```java
if (!IME && interruptsPending) {
    cpuStructure.idu().disableNextIncrement();
}
```

The code only handles the case where `IME=0 AND interruptsPending`. But in this test, we have `IME=0 AND NO pending interrupts` (IF was just cleared). In this case, the code should:
1. Not trigger the HALT bug (no `disableNextIncrement()`)
2. Wait for an interrupt to be requested
3. Exit HALT immediately when an interrupt is requested (without servicing it)

**Current behavior**:
```java
cpuStructure.clock().stop();
cpuStructure.interruptBus().waitForInterrupt();
cpuStructure.clock().start();
```

This waits until `hasInterrupts()` returns true, which is correct. The timer continues to run during this wait (via `clock.tick()` in `waitForInterrupt()`), which is also correct.

### 6. Instruction Timing in HALT

The HALT instruction should take **1 M-cycle** to execute. After the `execute()` method returns, the CPU's fetch cycle runs (in `Cpu.java` lines 23-32):

```java
private void fetch_cycle(Instruction instruction) {
    if (!instruction.handlesFetch()) {
        fetch();
        cpuStructure.clock().tick();  // This is 1 M-cycle

        handlePotentialInterrupt();

        instruction.postFetch(cpuStructure);
    }
}
```

So the total timing for HALT is:
- `execute()`: runs `waitForInterrupt()` which ticks the clock until interrupt is requested
- `fetch_cycle()`: ticks the clock once more (1 M-cycle)

**WAIT! This might be wrong!**

During `execute()`, the `waitForInterrupt()` method is ticking the clock in a loop. Then after `execute()` returns, `fetch_cycle()` ticks the clock again. This might be double-counting!

**Looking at normal instruction flow**:
- Normal instruction `execute()` doesn't tick the clock
- After `execute()`, `fetch_cycle()` ticks the clock once

**HALT instruction flow**:
- HALT `execute()` ticks the clock multiple times (in the loop)
- After `execute()`, `fetch_cycle()` ticks the clock once more

This suggests HALT takes N+1 M-cycles, where N is the number of ticks inside `waitForInterrupt()`. For the test case:
- `waitForInterrupt()` runs until interrupt is requested
- Then one extra tick happens in `fetch_cycle()`

**Actually, this is fine!** The HALT instruction is special - it's supposed to stop the CPU and let time pass while waiting. The test is measuring the total time from clearing DIV to reading DIV, which includes:
1. The time waiting for VBLANK (during HALT)
2. The 6 NOPs after HALT
3. The overhead of the LDH instruction to read DIV

The test expects this total time to be consistent between two measurements, with a 1 M-cycle difference due to executing one fewer NOP before the measurement.

### 7. PPU VBLANK Timing

The test depends on the PPU generating VBLANK interrupts at the correct time. The VBLANK interrupt should be requested when LY transitions from 143 to 144 (start of VBLANK period).

**If the emulator's PPU VBLANK timing is off, the DIV values would be incorrect.**

The test waits until LY=10, then performs measurements. The timing between line 10 and the next VBLANK depends on:
- Scanline timing (456 dots per scanline)
- Number of scanlines (144 visible + 10 VBlank = 154 total)

From line 10 to line 144 (next VBLANK):
- 144 - 10 = 134 scanlines
- 134 × 456 dots = 61,104 dots = 15,276 M-cycles

After clearing DIV at a specific point within this period, the test measures how long until VBLANK occurs.

### Summary of Potential Issues

The most likely reason for test failure:

**Issue**: The emulator's HALT timing might be off by 1 M-cycle, OR the PPU's VBLANK interrupt timing is slightly off, OR the test is executing at a different point in the frame than expected.

The test is extremely sensitive to timing - a difference of just 64 M-cycles would change the DIV value by 1. The test expects:
- Round 1: DIV = 0x11 (1088 M-cycles after reset)
- Round 2: DIV = 0x12 (1152 M-cycles after reset)
- Difference: 64 M-cycles (= 1 DIV increment)

This 64 M-cycle difference comes from executing one fewer NOP instruction (1 M-cycle) before clearing DIV. But wait, 1 M-cycle difference shouldn't cause a 64 M-cycle difference in the DIV reading...

**Unless**: The test is specifically designed so that the timing sits right on the boundary of a DIV increment. The 1 M-cycle difference shifts the DIV reset point relative to the internal counter's increment, causing the subsequent DIV reading to capture one more increment.

The emulator could fail this test if:
1. **HALT exits at the wrong time**: Adding or removing even 1 M-cycle from HALT execution
2. **PPU VBLANK timing is off**: The VBLANK interrupt occurs slightly earlier or later than expected
3. **Internal counter phase is wrong**: The starting value of the internal counter (0xAAC8) might need to be adjusted based on when in the frame the test starts
4. **DIV reset timing**: The DIV reset might not happen at exactly the right T-cycle within an M-cycle
