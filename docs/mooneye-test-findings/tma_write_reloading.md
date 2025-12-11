# Mooneye Test Analysis: tma_write_reloading

## Test Overview

The `tma_write_reloading` test verifies the precise timing of when writes to the TMA (Timer Modulo) register are picked up during the timer reload process. When TIMA overflows, the Game Boy reloads TIMA with the value from TMA after a 1 machine cycle delay. This test checks whether writes to TMA during different phases of the reload process affect the value that gets reloaded into TIMA.

## What The Test Does

The test performs four separate timing experiments, each testing when a write to TMA occurs relative to the TIMA overflow and reload cycle. Each experiment follows the same pattern:

### Common Setup for Each Experiment

1. **Register Initialization**:
   - `b = $FE` (254) - Value that will be written to TIMA/TMA initially
   - `h = $7F` (127) - Value that will be written to TMA during reload
   - `a = 0` - Used to clear interrupts and DIV

2. **Timer Configuration**:
   - Disable interrupts (`di`)
   - Clear IE (Interrupt Enable) register
   - Clear IF (Interrupt Flag) register
   - Reset DIV (Divider) to 0
   - Set TIMA to `$FE` (254) - one increment away from overflow
   - Set TMA to `$FE` (254) - initial reload value
   - Set TAC to `0b00000110` (6):
     - Bit 2 = 1: Timer enabled
     - Bits 1-0 = 10: Frequency select = 65536 Hz (monitored bit = bit 5 of internal counter)
     - At 65536 Hz, TIMA increments every 64 T-cycles (16 M-cycles)

3. **Trigger Overflow**:
   - Write `b` ($FE) to DIV (resets internal counter to 0)
   - Write `b` ($FE) to TIMA (sets TIMA back to 254)
   - Write `a` (0) to DIV (resets internal counter again, causing falling edge detection)
   - Load `a` with `h` ($7F) - prepares the new TMA value

### Experiment 1: Write TMA at T-cycle 112 (28 NOPs after DIV reset)

**Timing Calculation**:
- After DIV reset: internal counter starts at 0
- Each instruction takes 4 T-cycles for `ldh` and 1 M-cycle (4 T-cycles) for `nop`
- `ld a,h`: 1 M-cycle = 4 T-cycles
- 16 NOPs: 16 M-cycles = 64 T-cycles (total: 68 T-cycles)
- 12 NOPs: 12 M-cycles = 48 T-cycles (total: 116 T-cycles)
- At T-cycle ~112-116, the timer bit 5 goes from 0→1→0 triggering TIMA increment from $FE→$FF→$00
- Overflow is detected, reload phase begins
- `ldh (<TMA),a`: Writes $7F to TMA at approximately T-cycle 112-116
- This is during the RELOAD_PENDING phase (between overflow detection and actual reload)

**Expected Result**: `d = $7F`
- The write to TMA happens during RELOAD_PENDING, so it should update the value before it's captured

### Experiment 2: Write TMA at T-cycle 116 (29 NOPs after DIV reset)

**Timing**: One NOP (4 T-cycles) later than Experiment 1
- 16 NOPs + 13 NOPs = 29 NOPs = 116 T-cycles
- `ldh (<TMA),a`: Writes $7F to TMA at approximately T-cycle 116-120

**Expected Result**: `e = $7F`
- Still during or just at the transition to RELOADING phase, write should be captured

### Experiment 3: Write TMA at T-cycle 120 (30 NOPs after DIV reset)

**Timing**: Two NOPs (8 T-cycles) later than Experiment 1
- 16 NOPs + 14 NOPs = 30 NOPs = 120 T-cycles
- `ldh (<TMA),a`: Writes $7F to TMA at approximately T-cycle 120-124

**Expected Result**: `c = $FE`
- The TMA value has already been captured ($FE) and reload is in progress
- Writing to TMA now doesn't affect the reload
- TIMA should have $FE (the original TMA value)

### Experiment 4: Write TMA at T-cycle 124 (31 NOPs after DIV reset)

**Timing**: Three NOPs (12 T-cycles) later than Experiment 1
- 16 NOPs + 15 NOPs = 31 NOPs = 124 T-cycles
- `ldh (<TMA),a`: Writes $7F to TMA at approximately T-cycle 124-128

**Expected Result**: `l = $FE`
- The reload has completed, TIMA already has $FE
- Writing to TMA after reload doesn't affect TIMA
- TIMA should still have $FE

## What The Test Expects

The test sets up assertions using the `setup_assertions` macro and then verifies:

```assembly
assert_c $fe    ; Experiment 3: TIMA should be $FE (late write, after capture)
assert_d $7f    ; Experiment 1: TIMA should be $7F (early write, before capture)
assert_e $7f    ; Experiment 2: TIMA should be $7F (write during capture window)
assert_l $fe    ; Experiment 4: TIMA should be $FE (write after reload complete)
```

**Summary of Expected Values**:
- **Register d (Experiment 1)**: `$7F` - Write to TMA is picked up because it happens before TMA is captured
- **Register e (Experiment 2)**: `$7F` - Write to TMA is still picked up at the boundary
- **Register c (Experiment 3)**: `$FE` - Write to TMA is too late; original TMA value was already captured
- **Register l (Experiment 4)**: `$FE` - Write to TMA is way too late; reload already completed

## What The Test Is Testing

This test validates the Game Boy's timer reload mechanism, specifically:

1. **TMA Capture Timing**: When TIMA overflows, there's a specific window when the TMA value is captured for reloading
2. **Reload Phases**: The reload process has distinct phases:
   - NORMAL: Timer operating normally
   - RELOAD_PENDING: Overflow detected, waiting 1 M-cycle before reload
   - RELOADING: Actually writing TMA value to TIMA
3. **Write Window**: Writes to TMA during RELOAD_PENDING should affect the reload, but writes during RELOADING should not
4. **Precise Timing**: The test verifies that the transition between these phases happens at exact T-cycle boundaries

The test is checking that:
- TMA is captured at the START of the RELOADING phase (not at overflow detection)
- Writes to TMA during RELOAD_PENDING affect the reload
- Writes to TMA during or after RELOADING do not affect the current reload

## Potential Failure Reasons

### Issue 1: TMA Capture Timing in Timer.java

Looking at `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/components/Timer.java`:

```java
private void transitionReloadPhase() {
    switch (reloadPhase) {
        case RELOAD_PENDING -> {
            if (wasOverflowCancelled) {
                reloadPhase = ReloadPhase.NORMAL;
                wasOverflowCancelled = false;
            } else {
                startReloading();  // Line 87
            }
        }
        case RELOADING -> reloadPhase = ReloadPhase.NORMAL;
        case NORMAL -> {}
    }
}

private void startReloading() {
    reloadPhase = ReloadPhase.RELOADING;
    capturedTmaValue = timerModulo.read();  // Line 97 - TMA is captured HERE
    interruptController.setInterrupt(Interrupt.TIMER);
}
```

The current implementation captures TMA at the beginning of `startReloading()`, which is called during `transitionReloadPhase()` at the start of an M-cycle.

### Issue 2: Timing of TMA Capture vs. Actual Hardware

The critical question is: **When exactly does the hardware capture the TMA value?**

According to the test expectations:
- At ~112 T-cycles (28 NOPs): Write should be captured → expects `$7F`
- At ~116 T-cycles (29 NOPs): Write should be captured → expects `$7F`
- At ~120 T-cycles (30 NOPs): Write should NOT be captured → expects `$FE`

This suggests that the TMA capture happens somewhere between 116-120 T-cycles after the DIV reset.

**Current Implementation Behavior**:

The Timer's `mCycle()` method:
```java
public void mCycle() {
    transitionReloadPhase();    // Line 71 - Captures TMA here if transitioning to RELOADING
    applyReloadIfActive();      // Line 72 - Writes captured value to TIMA

    for (int i = 0; i < 4; i++) {
        internalCounter.tCycle();  // Line 75
        checkForFallingEdge();     // Line 76
    }
}
```

The sequence is:
1. M-cycle N: Overflow detected (TIMA goes $FF→$00), sets `reloadPhase = RELOAD_PENDING`
2. M-cycle N+1:
   - `transitionReloadPhase()` called FIRST → captures TMA value
   - `applyReloadIfActive()` writes captured value to TIMA
   - Then the 4 T-cycles of the M-cycle execute

### Issue 3: T-Cycle Granularity Problem

The current implementation operates at M-cycle granularity, but the test is checking T-cycle level timing. The `mCycle()` method:
1. Captures TMA at the START of the M-cycle (before any T-cycles)
2. Applies reload at the START of the M-cycle (before any T-cycles)
3. Then runs the 4 T-cycles

**Problem**: If a CPU instruction writes to TMA during the same M-cycle that the timer is reloading, the current implementation might capture the TMA value before the CPU instruction has written it.

Example scenario for Experiment 2 (29 NOPs):
- M-cycle 29 begins
- Timer calls `transitionReloadPhase()` → captures TMA (still $FE)
- Timer calls `applyReloadIfActive()` → writes $FE to TIMA
- Then T-cycles 0-3 execute
- During these T-cycles, the CPU's `ldh (<TMA),a` instruction executes, writing $7F to TMA
- But it's too late! TMA was already captured at the start of the M-cycle

### Issue 4: Lack of Sub-M-Cycle Ordering

The Game Boy's hardware has specific ordering within an M-cycle:
1. CPU executes instructions (memory reads/writes happen at specific T-cycles)
2. Timer checks for falling edges at specific T-cycles
3. Reload process happens at specific T-cycles

The current implementation doesn't model this sub-M-cycle ordering. It processes timer state transitions before any CPU operations in that M-cycle, which could lead to incorrect capture timing.

### Recommended Fix Strategy

To fix this issue, the timer implementation needs to:

1. **Model T-cycle level timing for TMA capture**: The capture of TMA should happen at a specific T-cycle within the RELOADING M-cycle, not at the start of the M-cycle.

2. **Delay TMA capture**: Instead of capturing TMA in `transitionReloadPhase()`, capture it in `applyReloadIfActive()` or at a specific T-cycle during the reload M-cycle.

3. **Consider instruction timing**: The `ldh (<TMA),a)` instruction takes 3 M-cycles (12 T-cycles):
   - M-cycle 0: Fetch opcode
   - M-cycle 1: Fetch address
   - M-cycle 2: Write to memory

   The write happens in the 3rd M-cycle, and the timer needs to check if TMA has been written before capturing it.

4. **Test the specific timing window**: The test shows that:
   - 28-29 NOPs: TMA write is captured (expects $7F)
   - 30-31 NOPs: TMA write is not captured (expects $FE)

   This is only a 4-8 T-cycle difference, suggesting the capture happens very early in the RELOADING phase, possibly at T-cycle 0-1 of that M-cycle.

5. **Possible solution**: Capture TMA at the END of the RELOAD_PENDING M-cycle (or very beginning of RELOADING M-cycle), after CPU instructions have had a chance to execute. This would require restructuring when `transitionReloadPhase()` captures vs applies the TMA value.
