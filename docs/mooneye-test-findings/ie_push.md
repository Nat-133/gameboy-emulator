# Mooneye Test Analysis: ie_push

## Test Overview

The `ie_push` test validates a very specific edge case in the Game Boy's interrupt dispatch mechanism: what happens when the IE (Interrupt Enable) register at address 0xFFFF is modified during the interrupt dispatch process itself, specifically during the PC push to the stack. This is a critical timing test that verifies whether writes to IE during interrupt dispatch can cancel or modify the interrupt that's currently being dispatched.

## What The Test Does

The test consists of 4 rounds, each testing different scenarios:

### Round 1: IE Written During Upper Byte Push (Interrupt Cancellation)
**Location:** 0x0200
**Setup:**
1. Set SP = 0x0000 (so writing the upper byte of PC will write to address 0xFFFF, which is IE)
2. Enable TIMER interrupt in IE register (bit 2)
3. Enable interrupts with `ei`
4. Execute `nop` (to allow interrupt dispatch to begin)
5. Request TIMER interrupt by setting IF

**Expected Behavior:**
- During interrupt dispatch, the CPU pushes PC to the stack
- SP=0x0000, so it first decrements SP to 0xFFFF
- Writing the upper byte of PC (0x02) to address 0xFFFF overwrites the IE register
- Since the new IE value (0x02) has TIMER bit cleared, the interrupt should be cancelled
- PC should be set to 0x0000 (where a `jp hl` instruction jumps to finish_round1)
- IF should still have TIMER bit set (not cleared during cancellation)
- IME should be 0 after cancellation

**Success Criteria:**
- Execution reaches finish_round1 (via 0x0000)
- IF register still has INTR_TIMER set
- IME is disabled

### Round 2: Verify IME Remains Disabled
**Location:** Immediately after round 1
**Setup:**
1. Enable JOYPAD interrupt in IE
2. Request JOYPAD interrupt in IF
3. Execute `nop`

**Expected Behavior:**
- Since IME should be 0 after round 1's cancellation, no interrupt should occur
- Execution continues to round 3

**Success Criteria:**
- No interrupt occurs (doesn't jump to fail_round2_intr at INTR_VEC_JOYPAD)

### Round 3: IE Written During Lower Byte Push (Too Late to Cancel)
**Location:** After round 2
**Setup:**
1. Set SP = 0x0001 (so writing the lower byte of PC will write to address 0x0000)
2. Set HL = fail_round3_cancel (this is where cancelled interrupts would jump)
3. Enable SERIAL interrupt in IE register (bit 3)
4. Enable interrupts with `ei`
5. Execute `nop`
6. Request SERIAL interrupt by setting IF

**Expected Behavior:**
- During interrupt dispatch, the CPU pushes PC to the stack
- SP=0x0001, first decrement gives SP=0x0000, writes upper byte to 0x0000
- Second decrement gives SP=0xFFFF, writes lower byte (0x35) to 0xFFFF (IE register)
- The IE write happens during the lower byte push, which is too late to cancel the interrupt
- Interrupt should complete normally, jumping to INTR_VEC_SERIAL (0x58)
- IF should be cleared for SERIAL interrupt

**Success Criteria:**
- Interrupt completes (jumps to INTR_VEC_SERIAL, then finish_round3)
- IF register is cleared (no interrupts pending)

### Round 4: Two Interrupts, IE Written During Upper Byte Push (Partial Cancellation)
**Location:** After round 3
**Setup:**
1. Set SP = 0x0000 (writing upper byte will write to IE at 0xFFFF)
2. Set HL = fail_round4_cancel
3. Enable both STAT and VBLANK interrupts in IE (bits 1 and 0)
4. Enable interrupts with `ei`
5. Execute `nop`
6. Request both STAT and VBLANK interrupts in IF

**Expected Behavior:**
- Two interrupts are pending: VBLANK (highest priority) and STAT
- During dispatch, upper byte (0x02) is written to IE at 0xFFFF
- New IE value (0x02) has STAT bit set but VBLANK bit clear
- VBLANK interrupt should be cancelled, but STAT interrupt should proceed
- CPU should dispatch STAT interrupt instead
- IF should only clear VBLANK bit (not STAT, since STAT was dispatched)

**Success Criteria:**
- STAT interrupt is dispatched (jumps to INTR_VEC_STAT, then finish_round4)
- IF has VBLANK bit set (only VBLANK should remain)

## What The Test Expects

### Final State Requirements:
1. **Round 1 Success:** Execution reaches finish_round1, IF has TIMER bit set
2. **Round 2 Success:** No JOYPAD interrupt occurs
3. **Round 3 Success:** SERIAL interrupt completes, IF is cleared
4. **Round 4 Success:** STAT interrupt completes, IF has VBLANK bit set
5. **Test Passes:** Execution reaches quit_ok at end of round 4

### Memory Layout:
- **0x0000:** `jp hl` - Used by round 1 to jump to finish_round1
- **0x0040 (INTR_VEC_VBLANK):** `jp fail_round4_vblank`
- **0x0048 (INTR_VEC_STAT):** `jp finish_round4`
- **0x0050 (INTR_VEC_TIMER):** `jp fail_round1_nocancel`
- **0x0058 (INTR_VEC_SERIAL):** `jp finish_round3`
- **0x0060 (INTR_VEC_JOYPAD):** `jp fail_round2_intr`
- **0x0200:** Round 1 code
- **0x1000+:** Failure handlers

## What The Test Is Testing

This test validates critical timing behavior of the Game Boy's interrupt dispatch mechanism:

1. **Interrupt Dispatch Timing:** The test verifies the exact sequence and timing of operations during interrupt dispatch:
   - IME is cleared
   - IF bit is cleared
   - PC is pushed to stack (2 bytes, upper then lower)
   - PC is set to interrupt vector
   - Next instruction is fetched

2. **IE Register Live Monitoring:** The interrupt controller must continuously monitor the IE register, even during the interrupt dispatch process. Changes to IE during dispatch should affect which interrupt is actually dispatched.

3. **Cancellation Window:** There's a specific window during dispatch where writing to IE can cancel the interrupt:
   - Writing IE during the **upper byte** push can cancel the interrupt
   - Writing IE during the **lower byte** push is too late to cancel

4. **State Consistency:** When an interrupt is cancelled:
   - IME should be set to 0
   - IF should NOT be cleared (the interrupt remains pending but not serviced)
   - PC should end up at address 0x0000 (where the pushed upper byte was written)

5. **Multi-Interrupt Priority:** When multiple interrupts are pending and IE is modified during dispatch, the system should re-evaluate which interrupt to dispatch based on the new IE value.

## Potential Failure Reasons

### Issue 1: Interrupt Dispatch Does Not Monitor IE During Dispatch

**Current Implementation Analysis:**

In `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/HardwareInterrupt.java`:

```java
public static void callInterruptHandler(CpuStructure cpuStructure, Interrupt interrupt) {
    cpuStructure.registers().setIME(false);                                      // Step 1
    cpuStructure.interruptBus().deactivateInterrupt(interrupt);                 // Step 2

    cpuStructure.registers().setPC(cpuStructure.idu().decrement(cpuStructure.registers().PC()));  // Step 3
    cpuStructure.clock().tick();                                                 // Step 4

    ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());       // Step 5

    cpuStructure.registers().setPC(interrupt.getInterruptHandlerAddress());      // Step 6
    byte nextInstruction = ControlFlow.readIndirectPCAndIncrement(cpuStructure);
    cpuStructure.registers().setInstructionRegister(nextInstruction);
}
```

**The Problem:**

The current implementation commits to dispatching the interrupt immediately at the start of `callInterruptHandler`:
1. It clears IME
2. It clears the IF bit for the interrupt
3. Then it pushes PC to the stack

**This means:**
- By the time the PC is being pushed to the stack (Step 5), the interrupt has already been "deactivated" (IF bit cleared)
- If the stack push writes to IE register at 0xFFFF, there's no check to see if the interrupt should still be dispatched
- The interrupt continues to completion regardless of IE changes during the push

**What Should Happen:**

According to the test's expectations:
1. IME should be cleared first
2. PC should be pushed to the stack (2 memory writes with clock ticks)
3. **After the upper byte push**, the system should check if the interrupt is still enabled in IE
4. If the interrupt bit in IE has been cleared, the dispatch should be cancelled
5. If cancelled, IF should NOT be cleared, and PC should remain at the location where it was written

### Issue 2: No Re-evaluation of Active Interrupts

In `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/Cpu.java`:

```java
private void handlePotentialInterrupt() {
    if (cpuStructure.registers().IME() && cpuStructure.interruptBus().hasInterrupts()) {
        Interrupt highestPriorityInterrupt = cpuStructure.interruptBus().activeInterrupts().getFirst();
        HardwareInterrupt.callInterruptHandler(cpuStructure, highestPriorityInterrupt);
    }
}
```

The interrupt is determined once at the start, and then `callInterruptHandler` executes with that specific interrupt. There's no mechanism to:
- Re-evaluate which interrupt should be dispatched after IE changes
- Cancel and restart with a different interrupt if priorities change

### Issue 3: Push Operation Timing

In `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/ControlFlow.java`:

```java
public static void pushToStack(CpuStructure cpuStructure, short value) {
    decrementSP(cpuStructure);              // SP--
    cpuStructure.clock().tick();            // 1 m-cycle

    cpuStructure.memory().write(cpuStructure.registers().SP(), upper_byte(value));  // Write upper byte
    decrementSP(cpuStructure);              // SP--
    cpuStructure.clock().tick();            // 1 m-cycle

    cpuStructure.memory().write(cpuStructure.registers().SP(), lower_byte(value));  // Write lower byte
    cpuStructure.clock().tick();            // 1 m-cycle
}
```

The push operation is correct in terms of timing and order, but there's no hook point where:
- After writing the upper byte, the system checks if IE has changed
- The interrupt dispatch process can be cancelled mid-push

### Issue 4: Stack Pointer Wrapping

The test relies on SP wrapping behavior:
- Round 1: SP = 0x0000, after decrement becomes 0xFFFF
- Round 3: SP = 0x0001, after two decrements becomes 0xFFFF

The emulator's IDU (Increment/Decrement Unit) must handle 16-bit wrapping correctly for addresses 0x0000 and 0xFFFF.

### Issue 5: Memory Write to 0xFFFF Must Update IE Register

The test assumes that writing to address 0xFFFF updates the IE register immediately and that this change is visible to the interrupt dispatch logic.

In `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MappedMemory.java`:

```java
memoryMap[0xFFFF] = new ByteRegisterMapping(interruptEnableRegister);
```

This mapping looks correct - writes to 0xFFFF should update the IE register. However, the interrupt bus reads IE through:

```java
private byte calculateActiveInterruptByte() {
    return and(interruptFlagsRegister.read(), interruptEnableRegister.read());
}
```

This should see the updated value, but the issue is that this check only happens:
1. Before interrupt dispatch begins (in `handlePotentialInterrupt`)
2. Never during the dispatch process itself

### Required Implementation Changes

To pass this test, the emulator needs:

1. **Split Interrupt Dispatch into Phases:**
   - Phase 1: Clear IME
   - Phase 2: Push upper byte of PC, then CHECK if interrupt still enabled in IE
   - Phase 3a: If cancelled, restore PC to 0x0000, don't clear IF
   - Phase 3b: If not cancelled, push lower byte of PC, clear IF, jump to handler

2. **IE Monitoring During Dispatch:**
   - After each memory write during PC push, re-check `InterruptBus.activeInterrupts()`
   - If the interrupt being dispatched is no longer in the active list, cancel dispatch

3. **Cancellation Handling:**
   - If cancelled after upper byte push: PC should be set to 0x0000
   - If cancelled after lower byte push: PC should be set to 0x0001
   - IF should NOT be cleared for cancelled interrupts
   - IME should remain 0

4. **Multi-Interrupt Re-evaluation:**
   - If IE changes during dispatch and multiple interrupts are pending
   - Re-evaluate priority and dispatch the highest priority interrupt that's still enabled

## Summary

The `ie_push` test is a sophisticated timing test that verifies the Game Boy's interrupt dispatch mechanism correctly handles modifications to the IE register during the interrupt dispatch process itself. The test validates that:

1. Writing to IE during the upper byte push of PC can cancel the interrupt being dispatched
2. Writing to IE during the lower byte push is too late to cancel
3. Cancelled interrupts leave IF set but IME cleared
4. When multiple interrupts are pending, modifying IE during dispatch can change which interrupt gets serviced

The current emulator implementation likely fails this test because it commits to dispatching an interrupt too early (clearing IF before the PC push), and doesn't monitor IE register changes during the multi-cycle dispatch process. To pass this test, the interrupt dispatch logic needs to be refactored to check IE after each memory operation during the PC push, with proper cancellation handling.
