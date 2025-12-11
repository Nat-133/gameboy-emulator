# Mooneye Test Analysis: rapid_toggle

## Test Overview

The `rapid_toggle` test validates that the Game Boy timer correctly handles rapid starting and stopping of the timer. This test checks two critical behaviors:
1. Starting/stopping the timer does not reset its internal counter state
2. The timer circuit design causes "glitchy" timer increments when the enable bit is toggled

The test runs a tight loop that repeatedly enables and disables the 4096 Hz timer (writing to TAC register), expecting a timer interrupt to fire after approximately $FFF8-$FFD9 = $1F (31 decimal) loop iterations, which is significantly earlier than would occur if the timer was reset on each enable/disable.

## What The Test Does

### Initial Setup (Lines 43-55)
1. **Enable Timer Interrupt**: `ld a, INTR_TIMER` / `ldh (<IE), a` - Enable timer interrupts in IE register
2. **Clear Interrupt Flag**: `xor a` / `ldh (<IF), a` - Clear any pending interrupts
3. **Reset DIV**: `ldh (<DIV), a` - Reset DIV register (also resets internal timer counter to 0)
4. **Set TIMA to $F0**: `ld a, $F0` / `ldh (<TIMA), a` - Start TIMA at $F0 (needs 16 increments to overflow)
5. **Start Timer**: `ld a, %00000100` / `ldh (<TAC), a` - Enable 4096 Hz timer (bit 2 set, bits 1-0 = 00 for 4096 Hz)
6. **Initialize Counter**: `ld bc, $FFFF` - BC will count down to track how many loop iterations occur
7. **Enable Interrupts**: `ei` - Enable interrupt handling

### Main Loop (Lines 57-64)
The loop repeatedly toggles the timer on and off:
1. **Enable Timer**: `ld a, %00000100` / `ldh (<TAC), a` - Write 0x04 to TAC (enable timer, 4096 Hz)
2. **Disable Timer**: `ld a, %00000000` / `ldh (<TAC), a` - Write 0x00 to TAC (disable timer)
3. **Decrement Counter**: `dec bc` - Decrement the loop counter
4. **Check Counter**: `ld a, c` / `or b` / `jr nz, -` - Continue loop if BC != 0

Each iteration takes several CPU cycles:
- `ld a, imm8`: 2 M-cycles (8 T-cycles)
- `ldh (n8), a`: 3 M-cycles (12 T-cycles)
- Total per loop: approximately 13-14 M-cycles

### Failure Path (Line 66)
If the loop completes without a timer interrupt (BC reaches 0), the test fails with "FAIL: NO INTR"

### Success Path (Lines 68-72)
When the timer interrupt fires (from TIMA overflow):
1. **Jump to Interrupt Handler**: Execution jumps to address $50 (INTR_VEC_TIMER)
2. **Jump to test_finish**: `jp test_finish` redirects to validation code
3. **Validate BC Value**: Expects B=$FF and C=$D9
   - This means BC should be $FFD9 when interrupt fires
   - Loop ran $FFFF - $FFD9 = $0026 (38 decimal) iterations

## What The Test Expects

### Expected Behavior
The test expects a timer interrupt to fire when BC = $FFD9 (after 38 iterations of the loop).

### Expected Values at Completion
- **Register B**: $FF
- **Register C**: $D9
- **BC Combined**: $FFD9

### Timing Analysis
With TIMA starting at $F0 and needing to reach $00 (wrap around):
- Need 16 increments: $F0 -> $F1 -> ... -> $FF -> $00 (overflow triggers interrupt)
- At 4096 Hz, timer increments every 1024 T-cycles (256 M-cycles)
- Each loop iteration is ~13-14 M-cycles
- If timer ran continuously: would need ~256 M-cycles per increment = ~18-19 loop iterations per increment
- For 16 increments: ~300+ iterations expected if timer behaved normally

However, the test expects only ~38 iterations, suggesting the "glitchy" behavior causes many additional timer increments.

## What The Test Is Testing

### Core Timer Behavior: State Preservation
The timer's internal counter (part of the DIV register system) continues running even when the timer is disabled. When you:
1. Disable the timer (TAC bit 2 = 0)
2. Re-enable the timer (TAC bit 2 = 1)

The internal counter does NOT reset - it maintains its value. This is critical because the timer uses a falling edge detector on a specific bit of the internal counter.

### "Glitchy" Timer Increments
The Game Boy timer uses a circuit design where:
- The timer increments on a **falling edge** of a specific bit in the internal counter
- The monitored bit is determined by TAC bits 0-1 (frequency select)
- For 4096 Hz mode (TAC & 0x03 = 0), bit 9 is monitored
- The falling edge is detected as: `(timer_enabled) AND (bit_value)`

When you toggle the timer enable bit rapidly:
- **Scenario 1**: If bit 9 is high (1) when timer is enabled
  - `timer_bit = 1 AND 1 = 1` (high)
  - When timer is disabled: `timer_bit = 0 AND 1 = 0` (low)
  - This creates a **falling edge** (1 -> 0), causing TIMA to increment!

- **Scenario 2**: Even without bit 9 transitioning naturally, the enable/disable creates artificial edges

This means each enable/disable cycle can potentially cause a timer increment if bit 9 happens to be set, making the timer increment much faster than normal.

### Why BC = $FFD9?
The specific value $FFD9 depends on:
1. The initial state of the internal counter (reset to 0 via DIV write)
2. How many T-cycles each loop iteration takes
3. When bit 9 of the internal counter becomes 1 (after 512 T-cycles)
4. How many "glitchy" increments occur from toggling

The test validates that the emulator correctly implements this hardware quirk.

## Potential Failure Reasons

### Analysis of Emulator Timer Implementation

Looking at `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/components/Timer.java`:

#### Current Implementation Structure

The Timer class uses a falling edge detection mechanism:

```java
private void checkForFallingEdge() {
    boolean isTimerBitHigh = isTimerBitHigh();

    if (wasTimerBitHigh && !isTimerBitHigh) {
        incrementTima();
    }

    wasTimerBitHigh = isTimerBitHigh;
}

private boolean isTimerBitHigh() {
    if (!isTimerEnabled()) {
        return false;  // <-- KEY: Returns false when timer disabled
    }

    int monitoredBit = getMonitoredBit();
    int counterValue = internalCounter.getValue();
    return ((counterValue >> monitoredBit) & 1) == 1;
}
```

#### How TAC Writes Trigger Edge Checks

The TacRegister has a write listener:
```java
// In TacRegister.write():
public void write(byte newValue) {
    value.set(uint(newValue));
    writeListener.onWrite();  // <-- Calls Timer.checkForFallingEdge()
}

// In Timer constructor:
tac.setWriteListener(this::checkForFallingEdge);
```

#### Correct Behavior Analysis

**The implementation appears CORRECT for the "glitchy" behavior:**

1. **When timer is enabled and bit 9 is high**:
   - `isTimerEnabled() = true`
   - `isTimerBitHigh() = true` (if bit 9 of counter is 1)
   - `wasTimerBitHigh = true` after first check

2. **When timer is disabled** (writing 0 to TAC):
   - TAC write triggers `checkForFallingEdge()`
   - `isTimerEnabled() = false` now
   - `isTimerBitHigh() = false` (returns false when disabled)
   - Since `wasTimerBitHigh == true && isTimerBitHigh == false`: **falling edge detected!**
   - TIMA increments
   - `wasTimerBitHigh = false`

3. **When timer is re-enabled** (writing 4 to TAC):
   - TAC write triggers `checkForFallingEdge()`
   - `isTimerEnabled() = true` now
   - If bit 9 is still high: `isTimerBitHigh() = true`
   - Since `wasTimerBitHigh == false && isTimerBitHigh == true`: **rising edge** (no increment)
   - `wasTimerBitHigh = true`

This correctly implements the glitchy behavior where disabling the timer (when bit 9 is high) causes an increment.

### Potential Issues

#### Issue 1: Timing of checkForFallingEdge() Calls

The `checkForFallingEdge()` is called:
1. After each T-cycle in the internal counter
2. When TAC is written
3. When DIV is reset (via reset listener)

In the `mCycle()` method:
```java
public void mCycle() {
    transitionReloadPhase();
    applyReloadIfActive();

    for (int i = 0; i < 4; i++) {
        internalCounter.tCycle();
        checkForFallingEdge();  // Called 4 times per M-cycle
    }
}
```

**Potential Problem**: When TAC is written, the edge check happens immediately via the write listener, but this check happens OUTSIDE the normal mCycle loop. The timing of when the TAC write edge check occurs relative to the T-cycle edge checks might not be correct.

**Expected Behavior**: The TAC write should trigger an edge check at a specific point in the instruction's execution, but the current implementation checks immediately when the memory write occurs, which might be before or after the internal counter has incremented for that T-cycle.

#### Issue 2: Order of Operations During TAC Write

When TAC is written during an instruction:
1. The instruction executes over multiple M-cycles
2. The memory write happens at a specific M-cycle within the instruction
3. The internal counter is incrementing throughout

The current implementation might check for falling edges in the wrong order:
- Does the TAC write listener fire before or after the T-cycles for that M-cycle?
- Should the edge check happen with the OLD TAC value or NEW TAC value for the current T-cycle?

#### Issue 3: State of wasTimerBitHigh During Rapid Toggles

During the tight loop with rapid enable/disable:
- Enable writes might not properly capture the "was high" state
- The sequence of enable -> disable -> enable might not track state correctly if checks aren't happening at the right moments

#### Issue 4: Counter State at Loop Start

The test resets DIV which resets the internal counter to 0. The test then:
1. Starts the timer with TAC = 4
2. Begins the rapid toggle loop

**Critical Question**: What is the state of `wasTimerBitHigh` after the first TAC write?
- After DIV reset: counter = 0, bit 9 = 0
- After TAC write to enable: `isTimerBitHigh()` checks and returns false (bit 9 still 0)
- `wasTimerBitHigh` becomes false

As the loop runs and counter increments to 512 (bit 9 becomes 1), the glitchy increments should start happening. But the timing needs to be precise.

### Most Likely Failure Cause

**Hypothesis**: The edge detection during TAC writes happens at the wrong timing point relative to the internal counter increments. Specifically:

1. The TAC write listener calls `checkForFallingEdge()` synchronously during the write
2. This happens outside the normal 4-T-cycles-per-M-cycle loop in `mCycle()`
3. The timing of when this edge check sees the internal counter value might be off by a few T-cycles

**Expected BC value range**: The test expects BC = $FFD9 (within a small tolerance, likely a few iterations)

**If BC < $FFF8**: Interrupt happens too early (too many glitchy increments)
**If test fails with "NO INTR"**: Interrupt happens too late or not at all (too few glitchy increments)

The emulator needs to ensure that:
1. TAC write edge checks happen at the correct T-cycle within the instruction
2. The edge check sees the internal counter value at the right moment
3. The sequence of enable/disable creates the expected number of glitchy increments based on when bit 9 transitions

### Verification Steps Needed

To diagnose the actual failure:
1. Run the test and capture the actual BC value when interrupt fires (or if timeout occurs)
2. Log the internal counter value and bit 9 state during the first several loop iterations
3. Count how many glitchy increments actually occur
4. Compare with expected behavior based on T-cycle counting

The implementation appears structurally correct for the glitchy behavior, but the timing precision of when edge checks occur during TAC writes is likely the issue.
