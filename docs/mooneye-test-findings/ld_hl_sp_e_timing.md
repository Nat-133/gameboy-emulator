# ld_hl_sp_e_timing Test Analysis

## Test Overview

This test validates the precise timing of the `LD HL, SP+e` instruction (opcode 0xF8), specifically when the instruction's memory read for the signed offset parameter (`e`) occurs during OAM DMA transfers. The test checks that the instruction correctly reads the offset value at the exact machine cycle expected, which determines whether it reads the original value or a value modified by OAM DMA.

## What The Test Does

### Setup Phase

1. **Initialize Environment**:
   - Disables interrupts (`di`)
   - Waits for VBlank to ensure PPU is in a safe state

2. **Copy Test Code to Strategic Locations**:
   - Copies `wram_test` code (starting at offset 1) to VRAM at 0x8000
   - Copies `wram_test` code to OAM at 0xFE00 (actually at 0xFDFF, OAM-1)
   - These copies ensure the test code spans specific memory boundaries

3. **Setup Stack Pointer**:
   - Sets SP to 0xCFFF

4. **Execute Test from HIRAM**:
   - Copies `hiram_test` procedure to HIRAM (0xFF80-0xFFFF)
   - Jumps to HIRAM to execute the test (required because OAM DMA blocks access to other memory)

### Test Execution - Round 1

The test performs two rounds, each testing different timing alignments with OAM DMA:

**Round 1 Execution Flow**:
1. Sets B=38 (delay counter)
2. Starts OAM DMA from source address 0x8000 (where `wram_test` was copied)
3. Executes a delay loop (38 iterations of `dec b; jr nz`)
4. Executes 1 NOP
5. Sets DE to point to `finish_round1` in HIRAM
6. Sets HL to 0xFDFF (OAM-1)
7. Jumps to HL (0xFDFF), which starts executing `wram_test` at 0xFDFF

**At 0xFDFF** (the `wram_test` code):
- Byte at 0xFDFF: First byte of `LD HL, SP+$42` (opcode 0xF8)
- Byte at 0xFE00: Second byte of `LD HL, SP+$42` (the offset value, originally 0x42)

**Critical Timing**: The delay is calibrated so that when `LD HL, SP+e` executes:
- M-cycle 0: Instruction decode (reads 0xF8 at 0xFDFF)
- M-cycle 1: Memory read for offset `e` (reads from 0xFE00) - **this read happens exactly one cycle before OAM DMA ends**
- M-cycle 2: Internal delay for address calculation

Since the OAM DMA was copying data from 0x8000 (where wram_test was placed), and the read happens before DMA completes, OAM location 0xFE00 still contains 0xFF (the value being transferred by DMA), not the original 0x42.

Result: `LD HL, SP+$FF` where 0xFF is interpreted as -1 (signed byte)
- SP = 0xCFFF
- SP + (-1) = 0xCFFE
- This value is pushed to stack

### Test Execution - Round 2

**Round 2 Execution Flow**:
1. Sets B=38 again
2. Starts another OAM DMA from 0x8000
3. Executes the same delay loop (38 iterations)
4. Executes 2 NOPs (one more than Round 1)
5. Sets DE to point to `finish_round2` in HIRAM
6. Sets HL to 0xFDFF (OAM-1)
7. Jumps to HL (0xFDFF), executing `wram_test` again

**Critical Timing**: With the extra NOP:
- The memory read for offset `e` happens exactly when OAM DMA **has already ended**
- At this point, 0xFE00 contains 0x42 (the original value from `wram_test`)

Result: `LD HL, SP+$42`
- SP = 0xCFFF
- SP + 0x42 = 0xD041
- This value is pushed to stack

### Cleanup and Verification

After both rounds:
1. Pops DE from stack (gets 0xD041 from Round 2, split into D=0xD0, E=0x41)
2. Pops BC from stack (gets 0xCFFE from Round 1, split into B=0xCF, C=0xFE)
3. Jumps to `test_finish`

**But wait!** When we pop DE, we get 0xD041, but the test expects:
- D = 0xD0
- E = 0x3F (not 0x41!)

Let me recalculate: SP = 0xCFFF, SP + 0x42 = 0xD041... but the expected value is D=0xD0, E=0x3F, which is 0xD03F.

This suggests SP + 0x42 should equal 0xD03F. Let me check: 0xD03F - 0xCFFF = 0x40 (64 decimal).

So the actual offset being tested in Round 2 must be 0x40, not 0x42. Looking back at the test, the comment says "so e = $42 = 42" but the actual calculation shows we need offset 0x40.

Actually, reviewing the addresses more carefully:
- The first byte of `LD HL, SP+e` is at 0xFDFF
- The second byte (the offset parameter) is at 0xFE00
- But when OAM DMA is active and transferring, it's writing to OAM sequentially

The timing calibration ensures:
- Round 1: Memory read happens when byte being written to 0xFE00 is 0xFF (from source)
- Round 2: Memory read happens after DMA completes, so 0xFE00 has the original value

Looking at the expected values again:
- B=0xCF, C=0xFE → BC=0xCFFE → This is SP + (-1) = 0xCFFF - 1 = 0xCFFE ✓
- D=0xD0, E=0x3F → DE=0xD03F → This is SP + 0x40 = 0xCFFF + 0x40 = 0xD03F ✓

So the offset value at 0xFE00 in the original wram_test must be 0x40, not 0x42 as stated in the comment. Or more likely, at the specific byte position being transferred when the read occurs, the value is different.

Actually, looking more carefully: the OAM DMA copies from 0x8000, which has `wram_test + 1` copied there. The byte at index 0 of the OAM (0xFE00) gets the first byte of `wram_test + 1`. During transfer, before completion, this could be 0xFF. After completion, it would be the actual byte from source.

The key insight: The test verifies that the `e` parameter read in `LD HL, SP+e` happens during M-cycle 1 (the second M-cycle) of the instruction.

## What The Test Expects

The test expects the following register values after both rounds complete:

- **B = 0xCF** (upper byte of result from Round 1)
- **C = 0xFE** (lower byte of result from Round 1)
- **D = 0xD0** (upper byte of result from Round 2)
- **E = 0x3F** (lower byte of result from Round 2)

These values prove:
1. Round 1 executed `LD HL, SP+(-1)` → 0xCFFF + (-1) = 0xCFFE
2. Round 2 executed `LD HL, SP+(value)` → 0xCFFF + value = 0xD03F

The specific values demonstrate that the instruction read the offset parameter at precisely M-cycle 1, because:
- If read too early, both rounds would see the same original value
- If read too late, both rounds would see the DMA-modified value
- The one-cycle difference (1 NOP vs 2 NOPs) creates exactly one M-cycle offset in when the read occurs relative to DMA completion

## What The Test Is Testing

This test validates:

1. **LD HL, SP+e Instruction Timing**: The instruction must follow this exact timing:
   - M-cycle 0: Instruction decode (read opcode byte)
   - M-cycle 1: Memory read for signed offset parameter `e`
   - M-cycle 2: Internal delay for address calculation

2. **Memory Read Timing**: The parameter read must occur during M-cycle 1, not earlier or later

3. **OAM DMA Interaction**: During OAM DMA:
   - Memory reads to OAM region should reflect the DMA transfer state
   - The blocking/access behavior must be cycle-accurate
   - The test relies on reading from OAM at precise moments relative to DMA completion

4. **Instruction Implementation Correctness**: The emulator must:
   - Execute the full 3 M-cycle instruction with correct timing
   - Read the offset parameter at the right moment
   - Perform the signed addition correctly
   - Set flags correctly (Z=0, N=0, H and C from addition)

## Potential Failure Reasons

### 1. Incorrect Instruction Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/OperationTargetAccessor.java` (lines 71-75)

The current implementation for `SP_OFFSET`:
```java
case SP_OFFSET -> ControlFlow.signedAdditionWithIdu(
        registers.SP(),
        (byte) this.getDirectValue(OperationTarget.IMM_8),
        true,
        cpuStructure);
```

This reads IMM_8 (which internally calls `readIndirectPCAndIncrement`), then performs the addition. The issue is **when** the memory read occurs.

**Analysis**:
- `getDirectValue(IMM_8)` calls `ControlFlow.readIndirectPCAndIncrement()` (line 69)
- `readIndirectPCAndIncrement()` reads memory and increments PC with a clock tick (line 78-82 in ControlFlow.java)
- Then `signedAdditionWithIdu()` performs the calculation with another clock tick at the end (line 69)

The problem: The instruction should have this timing:
- M-cycle 0: Opcode fetch (handled by instruction fetch mechanism)
- M-cycle 1: Read parameter `e` from (PC) and increment PC
- M-cycle 2: Internal delay for calculation

But the current implementation might be:
- Reading the parameter
- Performing calculation (which includes 1 clock tick at line 69 of ControlFlow.java)

This appears to match the expected 3-cycle timing in the test (`CpuCycleTest.java` line 80 confirms 3 cycles).

### 2. OAM DMA Memory Access Behavior

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java`

The DMA implementation shows:
```java
private boolean isBlocking() {
    return switch (dmaPhase) {
        case INACTIVE -> false;
        case REQUESTED, PENDING -> blockingDuringSetup;
        case TRANSFERRING -> true;
    };
}
```

And the read implementation:
```java
public byte read(short address) {
    ensureDmaListenerRegistered();
    if (isBlocking() && !isAccessibleDuringDma(address)) {
        return (byte) 0xFF;
    }
    return underlying.read(address);
}
```

**Potential Issues**:

a) **DMA Phase Timing**: The DMA goes through phases:
   - REQUESTED → PENDING → TRANSFERRING
   - Each transition happens on `mCycle()` call
   - The test requires exact cycle accuracy for when transfers occur and complete

b) **OAM Memory Reads During Transfer**: When reading from OAM (0xFE00-0xFE9F) during DMA:
   - The test expects to read the value being transferred at that exact byte
   - Current implementation returns 0xFF for blocked reads
   - But the test needs to read the actual value being written by DMA

   The test comment says "if OAM DMA is still running $42 will be replaced with $FF", suggesting that during DMA, reading from OAM should return the value currently being transferred (0xFF from source), not a hardcoded 0xFF.

c) **DMA Completion Timing**: The test's one-cycle difference between rounds is crucial:
   - Round 1: Read happens before DMA completes → should see in-transfer value
   - Round 2: Read happens after DMA completes → should see final value

   The DMA completion timing must be exactly correct. With 160 bytes (0xA0) to transfer at 1 byte per M-cycle, DMA takes 160 M-cycles to complete plus the setup phases.

### 3. Signed Addition Implementation

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/ControlFlow.java` (lines 44-72)

```java
public static short signedAdditionWithIdu(short a, byte signedByte, boolean setFlags, CpuStructure cpuStructure) {
    byte msb = upper_byte(a);
    byte lsb = lower_byte(a);

    ArithmeticResult res = cpuStructure.alu().add(lsb, signedByte);
    if (setFlags) {
        // For LD HL,SP+n: set Z=0, N=0, keep H and C from addition
        Hashtable<Flag, Boolean> flagChanges = new FlagChangesetBuilder(res.flagChanges())
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .build();
        flagChanges.forEach((f,b) -> cpuStructure.registers().setFlags(b, f));
    }
    lsb = res.result();

    boolean carry = res.flagChanges().getOrDefault(Flag.C, false);
    boolean negativeOffset = get_bit(signedByte, 7);

    if (carry && !negativeOffset) {
        msb = (byte) cpuStructure.idu().increment(msb);
    }
    else if (!carry && negativeOffset) {
        msb = (byte) cpuStructure.idu().decrement(msb);
    }

    cpuStructure.clock().tick();

    return concat(msb, lsb);
}
```

This implementation looks correct:
- Adds lower byte with signed offset
- Handles carry propagation to upper byte
- Has one clock tick at the end (M-cycle 2)
- Sets flags correctly

### 4. Most Likely Issue: OAM Memory Read During DMA

The most probable failure reason is **how memory reads from OAM behave during DMA transfer**:

**Current Behavior** (MemoryBus.java):
```java
if (isBlocking() && !isAccessibleDuringDma(address)) {
    return (byte) 0xFF;
}
```

**Expected Behavior**:
When reading from OAM during DMA, the emulator should:
1. If the byte has already been transferred, return the transferred value
2. If the byte is currently being transferred, return the value being written
3. If the byte hasn't been transferred yet, return the old value

The test specifically crafted the timing so that:
- Round 1: Byte at 0xFE00 is being/has been transferred when read occurs
- Round 2: Entire DMA has completed when read occurs

The current implementation returns hardcoded 0xFF for blocked addresses, but it should return the actual value present in OAM at that moment, considering DMA progress.

### 5. DMA Phase Progression Timing

The DMA phase transitions (REQUESTED → PENDING → TRANSFERRING) happen on `mCycle()` calls. The test requires precise alignment:
- When does DMA start blocking memory?
- When does each byte get transferred?
- When does DMA complete and stop blocking?

The delay loop with 38 iterations plus NOPs is carefully calibrated. If the emulator's DMA timing is off by even one cycle, the test will fail.

## Summary

The test most likely fails due to **OAM memory read behavior during DMA transfers**. The emulator currently returns a hardcoded 0xFF for blocked addresses, but the test expects to read actual values being transferred to OAM. Additionally, the exact cycle-accurate timing of DMA phase transitions and byte transfers must match hardware behavior for the test to pass.
