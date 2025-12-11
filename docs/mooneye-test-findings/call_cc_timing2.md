# Mooneye Test Analysis: call_cc_timing2

## Test Overview

The `call_cc_timing2` test verifies the exact M-cycle timing of the conditional CALL instruction (specifically CALL C, nn - opcode 0xDC) by exploiting OAM DMA memory blocking behavior. The test executes three CALL C instructions at precisely timed moments during OAM DMA transfers to verify that the memory writes for pushing the return address happen at the correct M-cycles when the condition is true.

## What The Test Does

### Setup Phase
1. **Disable interrupts**: Calls `di` to prevent interrupts from interfering with timing
2. **Initialize VRAM**: Fills the first 0x20 bytes of VRAM (starting at 0x8000) with the value 0x81. This provides a known value that will be copied by OAM DMA to OAM memory.
3. **Copy to HIRAM**: Uses the `run_hiram_test` macro to copy the test procedure to HIRAM (0xFF80+) so it can execute during OAM DMA (when most memory is inaccessible).
4. **Execute from HIRAM**: Jumps to the test code in HIRAM.

### Test Execution - Three Rounds

Each round follows the same pattern but with different timing:

#### Round 1: Test when OAM is accessible at M=6 (lines 58-70)
```assembly
1. Set SP to OAM+0x20 (0xFE20)
2. Start OAM DMA from 0x8000 (copies 0x81 values)
3. Wait precisely: ld a,38; loop: dec a; jr nz,loop; nops 1
   - This delay positions the CALL C to start at the right moment
4. Set carry flag: scf
5. Execute: call c, $FF80+(finish_round1-hiram_test)
6. At finish_round1: pop bc to retrieve what was pushed to the stack
```

**Timing intention**: The test carefully times the CALL C instruction so that OAM becomes accessible (DMA completes) at M=6 of the CALL instruction. According to the test comments, both the high byte (pushed at M=4) and low byte (pushed at M=5) writes should be blocked by DMA and read back as 0x81.

**Expected result after Round 1**:
- B = 0x81 (high byte blocked by DMA)
- C = 0x81 (low byte blocked by DMA)

#### Round 2: Test when OAM is accessible at M=5 (lines 72-83)
```assembly
1. Start OAM DMA from 0x8000 again
2. Wait precisely: ld a,38; loop; nops 2
3. Set carry flag: scf
4. Execute: call c, $FF80+(finish_round2-hiram_test)
5. At finish_round2: pop de to retrieve what was pushed
```

**Timing intention**: OAM becomes accessible at M=5 (one cycle later than Round 1). The high byte write at M=4 is still blocked by DMA, but the low byte write at M=5 should succeed.

**Expected result after Round 2**:
- D = 0x81 (high byte blocked by DMA)
- E = 0xB9 (low byte written correctly - this is the actual low byte of the return address)

#### Round 3: Test when OAM is accessible at M=4 (lines 85-97)
```assembly
1. Start OAM DMA from 0x8000 again
2. Wait precisely: ld a,38; loop; nops 3
3. Set carry flag: scf
4. Execute: call c, $FF80+(finish_round3-hiram_test)
5. At finish_round3: pop hl to retrieve what was pushed
```

**Timing intention**: OAM becomes accessible at M=4 (one cycle later than Round 2). Both the high byte and low byte writes should succeed as OAM is no longer blocked by DMA.

**Expected result after Round 3**:
- H = 0xFF (high byte written correctly - high byte of return address)
- L = 0xD6 (low byte written correctly - low byte of return address)

### Verification Phase
The test uses the assertion framework to check register values:
```assembly
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

This test validates the **precise M-cycle timing of the conditional CALL instruction's memory accesses**, specifically CALL cc, nn when the condition is true. According to the test header comments, the expected timing is:

### CALL cc, nn Instruction Timing (when condition is TRUE)
```
M = 0: instruction decoding (fetch and decode the CALL opcode)
M = 1: nn read: memory access for low byte of target address
M = 2: nn read: memory access for high byte of target address
M = 3: internal delay
M = 4: PC push: memory access for high byte of PC
M = 5: PC push: memory access for low byte of PC
```

The test verifies this by:
1. **OAM DMA Blocking Behavior**: During OAM DMA, only HIRAM (0xFF80-0xFFFE) is accessible. Attempts to read/write OAM during DMA return 0xFF (or read the value being transferred, which is 0x81 in this test).
2. **Precise Timing Control**: Using carefully counted delays to make OAM accessible at specific M-cycles.
3. **Stack Memory Inspection**: By pushing to OAM memory during DMA at different phases, the test can determine exactly when the memory writes occur.

Note: When the condition is false, CALL cc, nn takes only 3 M-cycles (M=0, M=1, M=2) as it doesn't execute the push or jump.

## Potential Failure Reasons

### 1. Missing Internal Delay Cycle at M=3

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Call.java`

The current implementation:
```java
@Override
public void execute(CpuStructure cpuStructure) {
    short functionPointer = ControlFlow.readImm16(cpuStructure);  // M=1 and M=2

    // technically, the if statement should be in the previous clock tick.
    if (ControlFlow.evaluateCondition(condition, cpuStructure.registers())) {
        ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());  // M=3,4,5?
        cpuStructure.registers().setPC(functionPointer);
    }
}
```

**Problem 1 - Missing M=3 internal delay**: According to the test specification, there should be an **internal delay cycle** at M=3 between reading the address and starting the push operation. The current implementation goes directly from `readImm16` (M=1, M=2) to `pushToStack` without an intermediate delay cycle.

**Problem 2 - Condition evaluation timing**: The comment on line 27 indicates awareness that "the if statement should be in the previous clock tick", but it's currently evaluated AFTER reading the immediate value. The condition should ideally be evaluated during the internal delay at M=3, but the hardware doesn't allow "rewinding" if the condition is false after already reading the address.

**Expected behavior**:
```java
short functionPointer = ControlFlow.readImm16(cpuStructure);  // M=1, M=2

if (ControlFlow.evaluateCondition(condition, cpuStructure.registers())) {
    cpuStructure.clock().tick();  // M=3: internal delay - MISSING!
    ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());  // M=4, M=5
    cpuStructure.registers().setPC(functionPointer);
}
```

### 2. Incorrect Stack Push Timing

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
1. SP--, tick (M-cycle consumed, no memory access)
2. Write high byte, SP--, tick (M-cycle consumed, memory write happens)
3. Write low byte, tick (M-cycle consumed, memory write happens)

This results in 3 M-cycles total for the push operation. However, according to the test specification:
- M=4: Write high byte to memory
- M=5: Write low byte to memory

**Critical timing issue**: The question is whether the memory write in step 2 happens during the first or second M-cycle of that step. If it happens during the second M-cycle, then:
- Step 1 (SP--, tick) = M=3 (matches the "internal delay" in the CALL timing)
- Step 2 (write high, SP--, tick) = M=4 (write happens here)
- Step 3 (write low, tick) = M=5 (write happens here)

However, if the missing explicit M=3 delay from the Call.java execute() method is the issue, then pushToStack is being called one cycle early, causing:
- pushToStack step 1 = M=2 (wrong!)
- pushToStack step 2 = M=3 (wrong!)
- pushToStack step 3 = M=4 (wrong!)

This would shift all memory writes one cycle early and cause all test rounds to fail.

### 3. Order of Clock Ticks vs Memory Operations

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/ControlFlow.java`

Looking at the pushToStack implementation more carefully:

```java
cpuStructure.clock().tick();    // tick BEFORE write
cpuStructure.memory().write(...);  // write happens
```

**Potential issue**: The clock tick happens BEFORE the memory write. This means the DMA state machine (which is called via clock tick) transitions to the next state before the memory write is attempted. This could cause off-by-one timing errors.

For example, if the DMA is supposed to block at M=4:
1. Clock ticks (M=4 starts, DMA might transition)
2. Memory write happens (but is M=4 still blocking, or has DMA already moved to next state?)

The test requires very precise timing of when OAM becomes accessible vs. when memory writes occur. The order of operations within an M-cycle matters:
- If DMA transitions first, then memory writes in that same M-cycle might succeed
- If memory writes first, then DMA blocking should apply

### 4. DMA State Machine Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java` (lines 89-105)

The DMA controller's `mCycle()` method:
```java
@Override
public void mCycle() {
    switch (dmaPhase) {
        case INACTIVE -> {}
        case REQUESTED -> dmaPhase = DmaPhase.PENDING;
        case PENDING -> dmaPhase = DmaPhase.TRANSFERRING;
        case TRANSFERRING -> {
            // Transfer one byte
            dmaByteIndex++;
            if (dmaByteIndex >= OAM_SIZE) {
                dmaPhase = DmaPhase.INACTIVE;
            }
        }
    }
}
```

**Analysis**:
- OAM DMA takes 160 M-cycles to transfer 160 bytes (0xA0 = 160)
- The DMA starts blocking at the TRANSFERRING phase
- The DMA completes and becomes INACTIVE after transferring all bytes

**Potential timing issues**:
1. The test starts DMA during VBLANK (wait_vblank macro)
2. Then waits with a precise delay (ld a,38; dec a; jr nz,- loop + nops)
3. The CALL instruction starts at a precise moment

The test relies on OAM becoming accessible at specific M-cycles. With:
- REQUESTED -> PENDING (1 M-cycle)
- PENDING -> TRANSFERRING (1 M-cycle)
- TRANSFERRING for 160 bytes (160 M-cycles)
- Total: 162 M-cycles from write to $FF46 until DMA completes

The test's timing must account for:
- When exactly during the TRANSFERRING phase does each byte transfer?
- When does blocking start and end?
- Does the last byte transfer happen in the same M-cycle that DMA becomes INACTIVE?

### 5. Clock Callbacks and DMA Integration

**Location**: Clock system integration

The clock system calls `mCycle()` on the DMA controller once per M-cycle. The critical question is: **in what order do things happen within a single M-cycle?**

Possible orders:
1. Clock tick -> DMA mCycle() -> CPU memory operation
2. Clock tick -> CPU memory operation -> DMA mCycle()
3. CPU memory operation -> Clock tick -> DMA mCycle()

The test expects:
- Round 1: CALL starts, OAM accessible at M=6 (after both writes at M=4 and M=5)
- Round 2: CALL starts, OAM accessible at M=5 (after write at M=4, but at same time as write at M=5)
- Round 3: CALL starts, OAM accessible at M=4 (at same time as both writes)

This suggests the memory write and DMA state check must happen at the exact same moment within the M-cycle, with different relative timing in each round.

### 6. Comparison with Unconditional CALL

**Reference**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/mooneye-test-findings/call_timing2.md`

The unconditional CALL test (call_timing2.md) has similar issues documented. The key difference is:
- Unconditional CALL always takes 6 M-cycles
- Conditional CALL takes 6 M-cycles if condition is true, 3 M-cycles if false

Both should have the same timing when the condition is true:
```
M=0: Decode
M=1: Read address low byte
M=2: Read address high byte
M=3: Internal delay
M=4: Write PC high byte to stack
M=5: Write PC low byte to stack
```

The conditional version likely has the same issues as the unconditional version, particularly:
1. Missing internal delay at M=3
2. Potentially incorrect timing of when memory writes happen relative to clock ticks

### 7. Specific Timing Calculation

Let's trace through what should happen in Round 2:

**Test code**:
```assembly
start_oam_dma $80        ; Starts DMA (REQUESTED phase)
ld a, 38                 ; 2 M-cycles
- dec a                  ; 38 * 4 M-cycles = 152 M-cycles (dec=1, jr nz=3)
  jr nz, -
nops 2                   ; 2 M-cycles
scf                      ; 1 M-cycle
call c, target           ; 6 M-cycles (if taken)
```

DMA timing:
- M=0: Write to $FF46 (REQUESTED)
- M=1: PENDING
- M=2: TRANSFERRING byte 0
- M=3: TRANSFERRING byte 1
- ...
- M=161: TRANSFERRING byte 159 (last byte)
- M=162: INACTIVE (OAM accessible)

Test delay calculation:
- start_oam_dma includes wait_vblank, which takes variable time
- The test must carefully calculate when the CALL instruction starts relative to when DMA completes

**Critical observation**: The test comment says "OAM is accessible at M=5", meaning:
- At M=4 of the CALL, OAM is still blocked (DMA still TRANSFERRING)
- At M=5 of the CALL, OAM is accessible (DMA became INACTIVE)

This requires knowing the exact cycle when the last DMA byte transfers and DMA becomes INACTIVE.

## Summary

The test is failing likely due to one or more of these issues:

1. **Missing internal delay cycle** at M=3 in the CALL instruction implementation
   - This is the most likely cause
   - Would shift all memory writes one cycle early
   - All three test rounds would read back incorrect values

2. **Order of operations within M-cycle**
   - Clock tick happens before memory write in pushToStack
   - DMA state might transition before memory write is attempted
   - Could cause off-by-one errors in which memory accesses are blocked

3. **DMA completion timing**
   - When exactly does OAM become accessible after the last byte transfer?
   - Does the transition from TRANSFERRING to INACTIVE happen before or after memory operations in that M-cycle?

4. **Condition evaluation timing**
   - The comment in Call.java acknowledges the condition should be evaluated earlier
   - However, the test might be forgiving of this as long as the memory writes happen at the right M-cycles

The most straightforward fix would be to add an explicit `cpuStructure.clock().tick()` in the Call.java execute() method after reading the address and before calling pushToStack, but only when the condition is true. However, this requires careful analysis of the overall timing to ensure it matches hardware behavior.
