# Mooneye Test Analysis: call_timing2

## Test Overview

The `call_timing2` test verifies the exact M-cycle timing of the unconditional CALL instruction (opcode 0xCD) by exploiting OAM DMA memory blocking behavior. The test executes three CALL instructions at precisely timed moments during OAM DMA transfers to verify that the memory writes for pushing the return address happen at the correct M-cycles.

## What The Test Does

### Setup Phase
1. **Initialize VRAM**: Fills the first 0x20 bytes of VRAM (starting at 0x8000) with the value 0x81. This provides a known value that will be copied by OAM DMA to OAM memory.
2. **Copy to HIRAM**: Copies the test procedure to HIRAM (0xFF80+) so it can execute during OAM DMA (when most memory is inaccessible).
3. **Execute from HIRAM**: Jumps to the test code in HIRAM.

### Test Execution - Three Rounds

Each round follows the same pattern but with different timing:

#### Round 1: Test M=5 and M=6 (lines 58-69)
```
1. Set SP to OAM+0x20 (0xFE20)
2. Start OAM DMA from 0x8000
3. Wait precisely: ld a,38; loop: dec a; jr nz,loop; nop; nop
   - This delay positions the CALL to start at the right moment
4. Execute: call $FF80+(finish_round1-hiram_test)
5. POP BC to retrieve what was pushed to the stack
```

**Timing intention**: OAM becomes accessible during M=5 and M=6 of the CALL instruction. The test expects both the high byte (pushed at M=4) and low byte (pushed at M=5) to be incorrect (0x81), indicating they were blocked by DMA.

**Expected result after Round 1**:
- BC = 0x8181 (both bytes read back as 0x81 from OAM due to DMA blocking)

#### Round 2: Test M=5 only (lines 71-81)
```
1. Start OAM DMA from 0x8000 again
2. Wait precisely: ld a,38; loop; nops 3
3. Execute: call $FF80+(finish_round2-hiram_test)
4. POP DE to retrieve what was pushed
```

**Timing intention**: OAM becomes accessible only during M=5. The high byte write at M=4 is still blocked, but the low byte write at M=5 succeeds.

**Expected result after Round 2**:
- D = 0x81 (high byte blocked by DMA)
- E = 0xB9 (low byte written correctly - this is the low byte of the return address)

#### Round 3: Test M=4 only (lines 83-93)
```
1. Start OAM DMA from 0x8000 again
2. Wait precisely: ld a,38; loop; nops 4
3. Execute: call $FF80+(finish_round3-hiram_test)
4. POP HL to retrieve what was pushed
```

**Timing intention**: OAM becomes accessible during M=4. Both memory writes should succeed as OAM is no longer blocked by DMA.

**Expected result after Round 3**:
- HL = actual return address (both bytes written correctly)

### Verification Phase
The test uses the assertion framework to check register values:
```
assert_b $81    ; High byte from Round 1 - blocked by DMA
assert_c $81    ; Low byte from Round 1 - blocked by DMA
assert_d $81    ; High byte from Round 2 - blocked by DMA
assert_e $B9    ; Low byte from Round 2 - written successfully
assert_h $FF    ; High byte from Round 3 - written successfully
assert_l $D6    ; Low byte from Round 3 - written successfully
```

## What The Test Expects

The test expects the following register values at the end:
- **B = 0x81**: High byte pushed during Round 1 should be 0x81 (blocked by DMA)
- **C = 0x81**: Low byte pushed during Round 1 should be 0x81 (blocked by DMA)
- **D = 0x81**: High byte pushed during Round 2 should be 0x81 (blocked by DMA)
- **E = 0xB9**: Low byte pushed during Round 2 should be the actual return address low byte
- **H = 0xFF**: High byte pushed during Round 3 should be the actual return address high byte
- **L = 0xD6**: Low byte pushed during Round 3 should be the actual return address low byte

These values demonstrate that:
1. When OAM is accessible at M=6, both stack writes (M=4 and M=5) are blocked
2. When OAM is accessible at M=5, only M=4 is blocked; M=5 succeeds
3. When OAM is accessible at M=4, both M=4 and M=5 succeed

## What The Test Is Testing

This test validates the **precise M-cycle timing of the CALL instruction's memory accesses**, specifically:

### CALL Instruction Timing (from test comments)
```
M = 0: instruction decoding (fetch and decode the CALL opcode)
M = 1: nn read - memory access for low byte of target address
M = 2: nn read - memory access for high byte of target address
M = 3: internal delay
M = 4: PC push - memory access for high byte of PC
M = 5: PC push - memory access for low byte of PC
```

The test verifies this by:
1. **OAM DMA Blocking Behavior**: During OAM DMA, only HIRAM (0xFF80-0xFFFE) is accessible. Attempts to read/write OAM return 0x81 (the value being transferred).
2. **Precise Timing Control**: Using carefully counted delays to make OAM accessible at specific M-cycles.
3. **Stack Memory Inspection**: By pushing to OAM memory during DMA at different phases, the test can determine exactly when the memory writes occur.

## Potential Failure Reasons

### 1. Incorrect CALL Instruction Timing Implementation

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Call.java`

The current implementation:
```java
@Override
public void execute(CpuStructure cpuStructure) {
    short functionPointer = ControlFlow.readImm16(cpuStructure);  // M=1 and M=2

    if (ControlFlow.evaluateCondition(condition, cpuStructure.registers())) {
        ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());  // M=3,4,5
        cpuStructure.registers().setPC(functionPointer);
    }
}
```

**Problem**: The comment on line 27 indicates awareness that the conditional check should happen earlier, but it's currently evaluated AFTER reading the immediate value. According to the test specification, the timing should be:
- M=0: Decode
- M=1: Read low byte of address
- M=2: Read high byte of address
- M=3: Internal delay (where condition would be checked)
- M=4: Write high byte of PC to stack
- M=5: Write low byte of PC to stack

### 2. Missing Internal Delay Cycle

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Call.java`

The current implementation calls `readImm16` (which takes 2 M-cycles) and then immediately calls `pushToStack`. According to the test specification, there should be an **internal delay** at M=3 before the push operations begin.

**Expected behavior**:
```java
short functionPointer = ControlFlow.readImm16(cpuStructure);  // M=1, M=2
cpuStructure.clock().tick();  // M=3: internal delay - MISSING!
ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());  // M=4, M=5
```

### 3. Incorrect Stack Push Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/ControlFlow.java` (lines 114-127)

The `pushToStack` implementation:
```java
public static void pushToStack(CpuStructure cpuStructure, short value) {
    decrementSP(cpuStructure);      // SP--
    cpuStructure.clock().tick();    // tick after SP--

    cpuStructure.memory().write(cpuStructure.registers().SP(), upper_byte(value));  // Write high byte
    decrementSP(cpuStructure);      // SP--
    cpuStructure.clock().tick();    // tick after SP--

    cpuStructure.memory().write(cpuStructure.registers().SP(), lower_byte(value));  // Write low byte
    cpuStructure.clock().tick();    // tick after write
}
```

**Analysis**: This implementation performs:
1. SP--, tick (M-cycle consumed)
2. Write high byte, SP--, tick (M-cycle consumed)
3. Write low byte, tick (M-cycle consumed)

This results in 3 M-cycles total for the push operation. However, the test expects:
- M=4: Write high byte to memory
- M=5: Write low byte to memory

**Potential issue**: The timing of when memory writes happen relative to clock ticks may not align with hardware behavior. The memory write should happen DURING the M-cycle, not after incrementing the clock.

### 4. DMA Timing Relative to Memory Operations

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java`

The DMA controller's `mCycle()` is called once per M-cycle in the clock callback. The DMA state machine transitions:
```
REQUESTED -> PENDING -> TRANSFERRING
```

**Potential issue**: The test requires very precise timing of when OAM becomes accessible vs. when memory writes occur. If the DMA state transitions or memory blocking happens at slightly different points within an M-cycle than on real hardware, the test results will differ.

The implementation needs to ensure that:
1. DMA blocking is checked at the right moment within each M-cycle
2. Memory operations happen at the correct sub-cycle timing
3. The transition from "blocked" to "accessible" aligns with hardware behavior

### 5. M-Cycle Counting and Synchronization

**Location**: Multiple files in CPU execution path

The test relies on extremely precise M-cycle counting. Any off-by-one error in:
- The delay loops before CALL
- The CALL instruction timing
- The DMA state machine transitions
- The POP instruction timing

...will cause the test to read back incorrect values.

**Key verification needed**:
1. Does `cpuStructure.clock().tick()` correctly represent one M-cycle?
2. Are all instructions' M-cycle counts accurate?
3. Does the DMA state machine transition at exactly the right M-cycle boundaries?
4. Do memory reads/writes happen at the correct point within an M-cycle?

## Summary

The test is failing likely due to one or more of these issues:
1. **Missing internal delay cycle** at M=3 in the CALL instruction
2. **Incorrect timing** of when memory writes happen relative to clock ticks in `pushToStack`
3. **DMA blocking/unblocking timing** not precisely aligned with hardware behavior
4. **Off-by-one errors** in M-cycle counting somewhere in the execution path

The most probable issue is the missing internal delay cycle (M=3) in the CALL instruction, which would shift all subsequent memory operations by one M-cycle and cause all three test rounds to read back incorrect values.
