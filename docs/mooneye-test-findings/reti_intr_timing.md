# reti_intr_timing Test Analysis

## Test Overview

The `reti_intr_timing` test validates the precise timing of interrupt re-enabling when the RETI (Return from Interrupt) instruction is executed. Specifically, it tests whether interrupts are enabled immediately after RETI completes, allowing a pending interrupt to be serviced right away.

Source: `/Users/nathaniel.manley/vcs/personal/mooneye-test-suite/acceptance/reti_intr_timing.s`

## What The Test Does

### Initial Setup (lines 29-36)
```assembly
di                              ; Disable interrupts
ld a, INTR_VBLANK | INTR_SERIAL ; A = 0x09 (bits 0 and 3 set)
ld (IF), a                      ; Set both VBLANK and SERIAL interrupt flags
ld (IE), a                      ; Enable both VBLANK and SERIAL interrupts
xor a                           ; A = 0
ld b, a                         ; B = 0
ld d, a                         ; D = 0
ld e, a                         ; E = 0
```

The test sets up with:
- Interrupts disabled (DI)
- Both VBLANK (bit 0) and SERIAL (bit 3) interrupt flags set in IF register
- Both VBLANK and SERIAL interrupts enabled in IE register
- Registers B, D, E initialized to 0

### Main Execution Flow (lines 40-45)
```assembly
ei                              ; Enable interrupts (takes effect after next instruction)
inc b                           ; B = 1, then VBLANK interrupt should trigger
; Handler $40 is supposed to be executed here
inc b                           ; This should NOT execute
```

**Step-by-step execution:**
1. `EI` instruction enables interrupts (but effect is delayed until after the next instruction)
2. `INC B` executes (B becomes 1)
3. After `INC B` completes, interrupts are checked and VBLANK interrupt fires (highest priority)
4. PC is pushed to stack, jump to VBLANK handler at address 0x40
5. The second `INC B` should NOT execute because control transfers to the interrupt handler

### VBLANK Interrupt Handler (lines 54-56)
```assembly
.org INTR_VEC_VBLANK    ; Address 0x40
  inc d                  ; D = 1
  reti                   ; Return from interrupt and enable interrupts
```

**Handler execution:**
1. `INC D` executes (D becomes 1)
2. `RETI` executes:
   - Pops return address from stack
   - Sets PC to return address
   - **Immediately enables interrupts (IME = 1)**

### Critical Timing Test
**The key question:** After RETI completes, are interrupts enabled immediately?

Since the SERIAL interrupt flag is still set (it was never cleared), and interrupts are now enabled:
- If RETI enables interrupts immediately: SERIAL interrupt should fire before the second `INC B` executes
- The SERIAL handler at 0x58 will execute instead

### SERIAL Interrupt Handler (lines 58-60)
```assembly
.org INTR_VEC_SERIAL    ; Address 0x58
  inc e                  ; E = 1
  jp test_finish         ; Jump to test_finish
```

**Handler execution:**
1. `INC E` executes (E becomes 1)
2. Jump to test_finish

### Test Verification (lines 47-52)
```assembly
test_finish:
  setup_assertions
  assert_b $01           ; B should be 1
  assert_d $01           ; D should be 1
  assert_e $01           ; E should be 1
  quit_check_asserts
```

## What The Test Expects

The test expects all three assertions to pass:
- **B = 0x01**: Only the first `INC B` executed (before VBLANK interrupt)
- **D = 0x01**: VBLANK handler executed (incremented D)
- **E = 0x01**: SERIAL handler executed (incremented E)

The critical expectation is **E = 0x01**, which proves that:
1. RETI enabled interrupts immediately
2. The pending SERIAL interrupt was serviced right after RETI completed
3. Control never returned to the second `INC B` instruction

## What The Test Is Testing

This test validates the **interrupt enable timing of the RETI instruction**. Specifically:

1. **RETI must enable interrupts immediately** upon completion, not after the next instruction
2. **No delay in interrupt checking** after RETI - if a pending interrupt exists, it should fire immediately
3. **Correct interrupt priority** - VBLANK (priority 0) fires before SERIAL (priority 3)

This is different from the EI instruction, which delays interrupt enabling until after the next instruction executes. RETI must enable interrupts with no such delay.

## Potential Failure Reasons

### Analysis of Emulator Code

#### 1. RETI Implementation (`ReturnFromInterruptHandler.java`)

**Current implementation (lines 15-20):**
```java
@Override
public void execute(CpuStructure cpuStructure) {
    short value = ControlFlow.popFromStack(cpuStructure);
    cpuStructure.registers().setPC(value);
    cpuStructure.registers().setIME(true);  // Sets IME immediately
    cpuStructure.clock().tick();
}
```

The RETI instruction correctly sets IME to true immediately in the execute phase.

#### 2. EI Implementation for Comparison (`EnableInterrupts.java`)

**Current implementation (lines 13-19):**
```java
@Override
public void execute(CpuStructure cpuStructure) {
}

@Override
public void postFetch(CpuStructure cpuStructure) {
    cpuStructure.registers().setIME(true);  // Delayed to postFetch
}
```

EI correctly delays interrupt enabling to the postFetch phase (after next instruction).

#### 3. CPU Cycle and Interrupt Checking (`Cpu.java`)

**The fetch_cycle method (lines 23-32):**
```java
private void fetch_cycle(Instruction instruction) {
    if (!instruction.handlesFetch()) {
        fetch();
        cpuStructure.clock().tick();

        handlePotentialInterrupt();  // Interrupts checked here

        instruction.postFetch(cpuStructure);
    }
}
```

**Main cycle method (lines 15-21):**
```java
public void cycle() {
    Instruction instruction = decode(cpuStructure.registers().instructionRegister());

    instruction.execute(cpuStructure);  // RETI sets IME=true here

    fetch_cycle(instruction);  // Then this executes
}
```

### THE PROBLEM

The issue is in the execution flow after RETI:

1. **RETI.execute()** runs:
   - Pops PC from stack
   - Sets PC to return address (pointing to second `INC B`)
   - **Sets IME = true**
   - Ticks clock

2. **fetch_cycle()** runs:
   - Fetches the next instruction (the second `INC B`) at the return address
   - Ticks clock
   - **Checks for interrupts** via `handlePotentialInterrupt()`
   - Runs postFetch

**The problem:** After RETI sets IME=true and PC to the return address, the emulator fetches the instruction at the return address (second `INC B`) BEFORE checking for interrupts.

However, looking at `handlePotentialInterrupt()` (lines 40-45), it SHOULD catch the pending SERIAL interrupt:

```java
private void handlePotentialInterrupt() {
    if (cpuStructure.registers().IME() && cpuStructure.interruptBus().hasInterrupts()) {
        Interrupt highestPriorityInterrupt = cpuStructure.interruptBus().activeInterrupts().getFirst();
        HardwareInterrupt.callInterruptHandler(cpuStructure, highestPriorityInterrupt);
    }
}
```

At this point:
- IME = true (set by RETI)
- SERIAL interrupt flag is still set
- So the interrupt should fire

**Wait - potential issue with interrupt priority!**

Looking at `InterruptBus.java` (lines 12-18):
```java
private static final List<Interrupt> INTERRUPT_PRIORITY = List.of(
        Interrupt.JOYPAD,
        Interrupt.SERIAL,
        Interrupt.TIMER,
        Interrupt.STAT,
        Interrupt.VBLANK
);
```

**This is BACKWARDS!** The interrupt priority is defined with JOYPAD as highest priority, but according to Game Boy hardware documentation and the test's behavior, **VBLANK should be highest priority (bit 0), and JOYPAD should be lowest (bit 4)**.

Correct priority order should be:
1. VBLANK (bit 0)
2. STAT (bit 1)
3. TIMER (bit 2)
4. SERIAL (bit 3)
5. JOYPAD (bit 4)

**However**, this wouldn't affect THIS specific test, because:
- After VBLANK handler completes (via RETI), only SERIAL interrupt is pending
- There's no priority conflict at that point

### Actual Root Cause

Looking more carefully at the flow:

After RETI completes:
1. RETI sets PC to return address (where second `INC B` is)
2. RETI sets IME = true
3. fetch_cycle() is called
4. fetch() reads the instruction at PC (second `INC B`) and increments PC
5. handlePotentialInterrupt() checks for interrupts

**The fetch has already happened!** The instruction register now contains the second `INC B` opcode.

On the NEXT call to cycle():
1. The second `INC B` will be decoded and executed

So the emulator will execute the second `INC B` before servicing the SERIAL interrupt, leading to:
- B = 2 (WRONG - should be 1)
- D = 1 (correct)
- E = 1 (correct, but happens later than it should)

### Summary of Issues

1. **Primary Issue**: After RETI enables interrupts, the emulator fetches the next instruction before checking for interrupts. The interrupt check happens between fetch and execution, but the instruction is already loaded and will execute on the next cycle.

2. **Secondary Issue**: Interrupt priority order is inverted in `InterruptBus.java` - JOYPAD should be lowest priority, VBLANK should be highest.

### Expected vs Actual Behavior

**Expected (correct hardware):**
- After RETI completes, interrupts are checked immediately
- SERIAL interrupt fires before any instruction at the return address executes
- Second `INC B` never executes
- B = 1, D = 1, E = 1

**Actual (emulator):**
- After RETI completes, next instruction is fetched
- Interrupts are checked, but instruction is already in pipeline
- Second `INC B` executes
- Then SERIAL interrupt fires
- B = 2, D = 1, E = 1

The test will fail because B = 2 instead of B = 1.
