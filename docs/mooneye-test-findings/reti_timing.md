# RETI Timing Test Analysis

## Test Overview

The `reti_timing` test validates the precise cycle-by-cycle timing of the RETI (Return from Interrupt) instruction. It uses OAM DMA memory blocking to create precise timing windows, testing whether RETI executes in exactly 4 M-cycles with the correct memory access pattern for each cycle. The test verifies that memory accesses happen only during M-cycles 1-2, and that M-cycle 3 is an internal delay with no memory access.

## What The Test Does

### Setup Phase

1. **Copy callback to HIRAM ($FF80-$FF81)**:
   ```assembly
   hiram_cb:
     xor a      ; Clear A register (A = 0)
     jp hl      ; Jump to address in HL
   ```
   - This 2-byte sequence can execute during OAM DMA since HIRAM ($FF80-$FFFE) remains accessible
   - Located at $FF80: opcode $AF (xor a), then jp hl

2. **Prepare ROM code at $2080**:
   ```assembly
   .org $2080
   ld a, $01   ; Set A = 1
   jp hl       ; Jump to address in HL
   ```

### Test Round 1: Verify memory access during M-cycles 1-2

**Goal**: Confirm that RETI's M-cycles 1-2 access memory (and can be blocked by DMA), while M-cycle 3 does not.

**Setup**:
```assembly
wait_vblank
ld hl, OAM - 1      ; HL = $FDFF
ld a, $80
ld (hl), a           ; Memory[$FDFF] = $80

ld hl, VRAM         ; HL = $8000
ld a, $20
ld (hl), a           ; Memory[$8000] = $20

ld SP, OAM-1        ; SP = $FDFF (stack points here)
ld hl, finish_round1 ; HL = address of finish_round1 label
```

After this setup:
- Stack pointer SP = $FDFF
- Memory[$FDFF] = $80 (will be popped as low byte of PC)
- Memory[$8000] = $20 (DMA source data)
- HL = finish_round1 (return address for both code paths)

**Timing synchronization**:
```assembly
ld b, 39
start_oam_dma $80   ; Start DMA from $8000 to $FE00
- dec b             ; Loop: 39 iterations
  jr nz, -          ; Each iteration: 3 M-cycles (dec=1, jr=2)
nops 3              ; 3 more M-cycles

reti                ; Execute RETI
```

**DMA timing**: `start_oam_dma $80` expands to:
```assembly
wait_vblank
ld a, $80
ldh (<DMA), a       ; Write to $FF46 register
```

The write to DMA register happens, then the delay loop executes:
- DMA start: M-cycle 0
- Delay loop: 39 × 3 = 117 M-cycles
- 3 NOPs: 3 M-cycles
- Total delay: 120 M-cycles from DMA start
- RETI begins at M-cycle 121

**DMA phases** (based on typical Game Boy DMA behavior):
- M-cycle 0: DMA write to $FF46 (REQUESTED)
- M-cycle 1: PENDING (preparation phase)
- M-cycles 2-161: TRANSFERRING (160 bytes transferred, 1 byte per M-cycle)
- M-cycle 162+: INACTIVE

During TRANSFERRING phase (M-cycles 2-161):
- Memory regions blocked: All except HIRAM ($FF80-$FFFE) and I/O registers ($FF00-$FF7F)
- Reads from blocked regions return $FF
- The DMA copies byte 0 from $8000 to $FE00 during M-cycle 2
- So Memory[$FE00] = $20 after M-cycle 2

**RETI execution timing**:
- M-cycle 121 (relative to DMA): RETI M=0 (instruction decode)
- M-cycle 122: RETI M=1 (pop low byte from [$FDFF])
- M-cycle 123: RETI M=2 (pop high byte from [$FE00])
- M-cycle 124: RETI M=3 (internal delay)

At M-cycles 122-123, DMA is in TRANSFERRING phase (transferring bytes 120-121 of 160), so memory is blocked.

**Memory access behavior during DMA**:
- Reading $FDFF: This is in WRAM region ($C000-$DFFF mirrors to $E000-$FDFF), normally accessible but blocked during DMA
- Reading $FE00: This is OAM region, blocked during DMA
- Both reads return $FF when blocked

**Critical insight**: The test is checking whether EXACTLY the right bytes are blocked:
- If M-cycle 1 happens during DMA blocking: low byte read = $FF
- If M-cycle 2 happens during DMA blocking: high byte read = $FF
- If M-cycle 3 tries to access memory (BUG): would also be blocked

**Expected PC value**:
- Both bytes blocked: PC = $FFFF
- Low byte succeeds, high byte blocked: PC = $FF80 ← **This is the key!**
- Both bytes succeed: PC = $2080
- Low byte blocked, high byte succeeds: PC = $20FF

**The test expects PC = $FF80** (low byte $80, high byte $FF), which means:
- Low byte read at M-cycle 1 (cycle 122) is NOT blocked yet, reads $80
- High byte read at M-cycle 2 (cycle 123) IS blocked, reads $FF
- This suggests DMA blocking starts BETWEEN M-cycles 1 and 2 of RETI

**Wait - that doesn't match the earlier analysis!** Let me recalculate:

At cycle 122 (RETI M=1):
- Relative to DMA start: cycle 122
- DMA started at cycle 0
- DMA phase at cycle 122: TRANSFERRING (started at cycle 2, so we're at byte 120)
- Memory SHOULD be blocked

This suggests another possibility: **Maybe the low byte read happens just before the blocking check, or there's a 1-cycle offset in when blocking takes effect.**

**Alternative theory**: What if RETI M=0 includes a memory read (instruction fetch), and the counting is different?

Actually, looking at the comment in the test:
```
; M = 0: instruction decoding
; M = 1: PC pop: memory access for low byte
; M = 2: PC pop: memory access for high byte
; M = 3: internal delay
```

This clearly states M=1 and M=2 do memory access. So if both happen during DMA blocking, both should return $FF.

**New theory**: What if the test expects the FETCH of the next instruction (after RETI completes) to be blocked?

After RETI completes at cycle 124:
- PC has been set to the popped value
- The CPU needs to fetch the next instruction from PC
- This fetch happens during the next M-cycle

If PC = $FF80:
- The fetch at cycle 125 reads from $FF80 (HIRAM, accessible during DMA)
- The instruction is $AF (xor a)
- Execution continues: xor a (A=0), then jp hl (jumps to finish_round1)
- At finish_round1: `or a; jr z, test_round2` - A=0, so continue to round 2 ✓

If PC = $2080:
- The fetch at cycle 125 reads from $2080 (ROM, blocked during DMA)
- Returns $FF, decoded as RST $38 instruction
- Would jump to $0038 interrupt vector
- Eventually reaches finish_round1 somehow with A=1
- At finish_round1: `or a; jr z, test_round2` - A≠0, so fail ✗

**But wait**: At cycle 125, DMA is still in TRANSFERRING (byte 123). ROM at $2080 would be blocked, returning $FF.

Actually, I need to reconsider what "blocked" means. During DMA:
- HIRAM ($FF80-$FFFE) is accessible
- I/O registers ($FF00-$FF7F) are accessible
- Everything else returns $FF

So:
- $FDFF (WRAM/echo RAM) returns $FF
- $FE00 (OAM) returns $FF
- $2080 (ROM) returns $FF
- $FF80 (HIRAM) returns actual value

**Revised understanding**:

If RETI reads both bytes during DMA blocking:
- Low byte from $FDFF: blocked, returns $FF
- High byte from $FE00: blocked, returns $FF
- PC = $FFFF

If RETI reads low byte before blocking starts:
- Low byte from $FDFF: succeeds, returns $80
- High byte from $FE00: blocked, returns $FF
- PC = $FF80

**The test expects PC = $FF80**, which means:
- The 120-cycle delay is calibrated so that RETI M=1 happens just before or at the edge of DMA blocking
- RETI M=2 happens definitely during DMA blocking

When PC = $FF80:
1. Next instruction fetch reads from $FF80 (accessible even during DMA)
2. Reads $AF (xor a)
3. Executes xor a → A = 0
4. Next instruction: jp hl → jumps to finish_round1
5. At finish_round1: A=0, test passes

### Test Round 2: Verify timing precision with +1 cycle offset

**Setup**:
```assembly
wait_vblank
ld hl, OAM          ; HL = $FE00
ld a, $FF
ld (hl), a           ; Memory[$FE00] = $FF

ld SP, OAM-1        ; SP = $FDFF
ld hl, finish_round2

ld b, 39
start_oam_dma $80
- dec b
  jr nz, -
nops 4              ; 4 NOPs instead of 3 - ONE extra M-cycle!

reti
```

**Timing**:
- DMA start: M-cycle 0
- Delay: 117 + 4 = 121 M-cycles
- RETI begins at M-cycle 122 (one cycle later than round 1)

**RETI execution**:
- M-cycle 123: RETI M=1 (pop low byte)
- M-cycle 124: RETI M=2 (pop high byte)
- M-cycle 125: RETI M=3 (internal delay)

With the extra 1-cycle delay:
- Now both M-cycles 1 and 2 should definitely be during DMA blocking
- Both bytes read as $FF
- PC = $FFFF

When PC = $FFFF:
1. Next instruction fetch reads from $FFFF (IE register, in accessible I/O region)
2. The value at $FFFF depends on interrupt enable settings (probably $00)
3. $00 = NOP instruction
4. PC wraps to $0000, continues execution through boot sequence/ROM
5. Eventually reaches finish_round2 with A=1 (from the ROM code at $2080 somehow)
6. At finish_round2: `or a; jr nz, test_success` - A≠0, test passes

Actually, that doesn't make sense either. Let me reconsider...

**Alternative for Round 2**: With extra delay, maybe now the low byte read succeeds and high byte is blocked:
- If this happens, PC = $20FF (high byte $20, low byte $FF)
- Or perhaps the timing shifts such that both succeed: PC = $2080

Wait, I'm confusing myself. Let me think about this more carefully.

The memory at $FE00 starts as $FF (explicitly written in round 2). But then DMA copies from $8000 (which contains $20 from setup). After DMA byte 0 transfer (at cycle 2), Memory[$FE00] = $20.

So at the time of RETI:
- Memory[$FDFF] = $80 (still there from before)
- Memory[$FE00] = $20 (overwritten by DMA)

Actually, wait_vblank happens between rounds, and the memory setup is fresh each round. Let me re-read round 2 setup.

Round 2 setup does NOT write to $FDFF! It only writes $FF to $FE00. So:
- Memory[$FDFF] = ??? (whatever is there, probably $80 from round 1 or garbage)
- Memory[$FE00] = $FF (explicitly set)

Then DMA starts from $8000, which still contains $20. After DMA transfer:
- Memory[$FE00] = $20 (copied from $8000)

Hmm, but the stack pointer is still at $FDFF. What's at $FDFF now?

Actually, WRAM is not persistent between rounds - each round does wait_vblank and fresh setup. But round 2 doesn't write to $FDFF, so it's probably $00 or garbage.

Let me assume $FDFF = $00 (or some default value). Then:
- Low byte from $FDFF = $00 (if not blocked) or $FF (if blocked)
- High byte from $FE00 = $20 (if not blocked) or $FF (if blocked)

If both blocked: PC = $FFFF
If only high byte blocked: PC = $FF00
If only low byte blocked: PC = $20FF
If neither blocked: PC = $2000

None of these are $2080 or $FF80...

I think I'm overcomplicating this. Let me just document what I know and provide educated guesses.

## What The Test Expects

Based on the test logic:

**Round 1** (`or a; jr z, test_round2`):
- Expects A = 0 to pass
- A = 0 means HIRAM callback at $FF80 was executed (`xor a`)
- This happens when PC = $FF80

**Round 2** (`or a; jr nz, test_success`):
- Expects A ≠ 0 to pass
- A ≠ 0 means ROM code at $2080 was executed (`ld a, $01`)
- This happens when PC = $2080

**Interpretation**:
- Round 1 (120-cycle delay): RETI should read low byte successfully but high byte blocked → PC = $FF80
- Round 2 (121-cycle delay): RETI should read both bytes successfully → PC = $2080

This suggests:
- At 121-122 cycles after DMA start: Low byte read succeeds, high byte blocked
- At 122-123 cycles after DMA start: Both bytes succeed (DMA blocking ends just in time)

But this contradicts the earlier calculation that DMA blocks until cycle 161!

**Alternative interpretation**: Maybe the DMA blocking has timing subtleties:
- Memory blocking might not start immediately at TRANSFERRING phase
- Or blocking might end earlier for certain addresses
- Or there's a 1-cycle offset in when memory reads check the blocking state

## What The Test Is Testing

The test validates:

1. **RETI instruction M-cycle breakdown**:
   - M-cycle 0: Instruction decode (fetch already happened)
   - M-cycle 1: Memory read of low byte from stack (SP)
   - M-cycle 2: Memory read of high byte from stack (SP+1)
   - M-cycle 3: Internal delay (no memory access)

2. **Memory access timing**: M-cycles 1-2 must access memory at specific moments that can be blocked by DMA

3. **M-cycle 3 is internal**: M-cycle 3 must NOT access memory (cannot be affected by DMA blocking)

4. **Precise timing**: The single-cycle difference between rounds must produce different PC values

## Potential Failure Reasons

### 1. Incorrect RETI M-Cycle Timing

**Current Implementation** (`ReturnFromInterruptHandler.java`):
```java
@Override
public void execute(CpuStructure cpuStructure) {
    short value = ControlFlow.popFromStack(cpuStructure);  // M-cycles 1-2
    cpuStructure.registers().setPC(value);
    cpuStructure.registers().setIME(true);
    cpuStructure.clock().tick();  // M-cycle 3
}
```

**ControlFlow.popFromStack**:
```java
public static short popFromStack(CpuStructure cpuStructure) {
    byte lsb = cpuStructure.memory().read(cpuStructure.registers().SP());  // Read low byte
    incrementSP(cpuStructure);
    cpuStructure.clock().tick();  // M-cycle 1 ends

    byte msb = cpuStructure.memory().read(cpuStructure.registers().SP());  // Read high byte
    incrementSP(cpuStructure);
    cpuStructure.clock().tick();  // M-cycle 2 ends

    return concat(msb, lsb);
}
```

**M-cycle count**:
- M-cycle 0: Instruction decode (handled by CPU.cycle() fetch)
- M-cycle 1: Read LSB + tick ✓
- M-cycle 2: Read MSB + tick ✓
- M-cycle 3: Final tick in execute() ✓
- Total: 4 M-cycles ✓

The M-cycle count is correct.

### 2. Memory Read Timing Within M-Cycle

**Potential issue**: The order of operations within an M-cycle matters:

Current order:
1. `memory.read(address)` - reads memory
2. `incrementSP()` - increments stack pointer
3. `clock.tick()` - advances clock

If DMA blocking state changes during `clock.tick()`, the memory read might happen before the state transition, causing incorrect blocking behavior.

**What should happen**: Memory reads should check DMA blocking state at the BEGINNING of the M-cycle, not after the read completes.

### 3. DMA Blocking Timing

The test relies on precise DMA blocking windows. The current emulator DMA implementation needs to:
- Block memory access starting at the correct M-cycle
- Maintain blocking for exactly 160 M-cycles of transfer
- End blocking at the correct M-cycle

**Potential issues**:
- DMA phases (REQUESTED, PENDING, TRANSFERRING) might transition at wrong times
- Blocking check might happen at wrong point in M-cycle
- Off-by-one errors in cycle counting

### 4. DMA Phase Transition Timing

Current DMA phases:
- REQUESTED (write to $FF46)
- PENDING (1 M-cycle delay)
- TRANSFERRING (160 M-cycles)

The test assumes specific timing for when blocking starts and ends. If the emulator's phase transitions don't match hardware:
- Memory might be blocked too early or too late
- The 120 vs 121 cycle delay wouldn't produce the expected different results

### 5. Instruction Fetch After RETI

After RETI completes and PC is updated, the CPU must fetch the next instruction from the new PC location:

```java
// In CPU.cycle(), after execute():
fetch_cycle(instruction);
```

This fetch happens during a separate M-cycle. If the fetch occurs during DMA blocking and reads from a blocked region, it would return $FF instead of the actual instruction.

**For the test**:
- If PC = $FF80 (HIRAM): Fetch succeeds during DMA, reads real instruction
- If PC = $2080 (ROM): Fetch during DMA returns $FF, causing wrong behavior

The timing of this fetch relative to DMA blocking is critical.

### 6. Stack Pointer Increment Timing

The `incrementSP` happens before `clock.tick()`:
```java
byte lsb = cpuStructure.memory().read(cpuStructure.registers().SP());
incrementSP(cpuStructure);  // SP changes before cycle ends
cpuStructure.clock().tick();
```

On real hardware, SP might increment at a different sub-cycle timing. If DMA blocking checks depend on SP value, this could cause issues.

### 7. IME Flag Setting Timing

The current implementation sets IME during M-cycle 3:
```java
cpuStructure.registers().setIME(true);
cpuStructure.clock().tick();
```

According to some documentation, IME should be set AFTER RETI completes, not during the final M-cycle. While this test doesn't involve interrupts being serviced, the timing could still matter for edge cases.

### 8. Clock Synchronization with DMA

The test requires perfect synchronization between:
- CPU cycle counter
- DMA cycle counter
- Memory blocking state

If these are not perfectly aligned (e.g., DMA ticks independently or at different times than CPU), the test will fail.

Current architecture uses `ClockWithParallelProcess` for PPU, but DMA timing must align with CPU M-cycles exactly.

## Key Questions for Debugging

To debug this test failure, check:

1. **What is the actual PC value after RETI in each round?**
   - Log PC after RETI execution
   - Compare to expected $FF80 (round 1) or $2080 (round 2)

2. **What values are read from stack during RETI?**
   - Log the actual bytes read at $FDFF and $FE00
   - Check if they're $80/$20 (success) or $FF/$FF (blocked)

3. **What is the DMA phase during RETI M-cycles 1-2?**
   - Log DMA phase at cycles 122-123 (round 1) and 123-124 (round 2)
   - Verify it's TRANSFERRING as expected

4. **When does DMA blocking actually start and end?**
   - Log the exact cycle when `isBlocking()` becomes true/false
   - Verify it matches expected start at cycle 2, end at cycle 161

5. **What instruction is fetched after RETI?**
   - Log the byte fetched from the PC location after RETI
   - Check if it matches expected opcode

## Summary

The `reti_timing` test is a sophisticated cycle-accurate timing test that verifies:
- RETI takes exactly 4 M-cycles
- Memory access happens only in M-cycles 1-2
- M-cycle 3 is an internal delay with no memory access
- Timing aligns perfectly with DMA blocking windows

The most likely failure causes:
1. **Memory read timing** - Reads might happen at wrong point relative to DMA state checks
2. **DMA blocking boundaries** - Blocking might start/end at wrong cycle
3. **Instruction fetch after PC update** - Fetch timing relative to DMA blocking
4. **Clock synchronization** - CPU and DMA cycle counters might be misaligned

To fix, the emulator needs cycle-perfect execution where:
- Memory reads check DMA state at the correct sub-cycle timing
- DMA phases transition at exactly the right M-cycles
- All operations within an M-cycle happen in the correct order
- CPU and DMA cycle counters are perfectly synchronized
