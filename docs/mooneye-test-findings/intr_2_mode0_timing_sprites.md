# Mooneye Test Analysis: intr_2_mode0_timing_sprites

## Test Overview

The `intr_2_mode0_timing_sprites` test verifies the precise timing of Mode 0 (H-Blank) interrupts in relation to Mode 2 (OAM scan) interrupts when sprites are present on the scanline. The test checks how sprite count, sprite position, and sprite pixel alignment affect the duration between Mode 2 and Mode 0, which determines when the STAT interrupt fires for Mode 0.

## What The Test Does

### Initialization Phase
1. **Setup**: Sets up the PPU with default configuration, loads fonts, and enables the PPU
2. **Enable STAT Interrupts**: Sets IE register to enable only STAT interrupts (bit 1)
3. **Configure STAT Register**: Sets STAT to 0x20 (bit 5 = 1), enabling Mode 2 (OAM) interrupts

### Test Case Execution

The test runs multiple test cases, each with different sprite configurations:

For each test case:

1. **Sprite Setup** (`prepare_sprites`):
   - Loads sprites into OAM at address 0xFE00
   - All sprites have Y coordinate = 0x52 (appear on scanline LY=0x42 after accounting for the 16-pixel offset)
   - X coordinates vary per test case (stored in test data)
   - Each sprite gets an incrementing tile number (0x30, 0x31, etc.)
   - Sprites have no flags (normal rendering)

2. **NOP Area Setup** (`prepare_nop_area`):
   - Creates two dynamic code areas filled with NOPs followed by RET
   - `nop_area_a`: First parameter (41 + extra_cycles) NOPs
   - `nop_area_b`: Second parameter (40 + extra_cycles) NOPs
   - These NOP sequences consume precise amounts of CPU cycles

3. **Round A Execution** (`testcase_round_a`):
   - Waits for LY=0x42
   - Waits for Mode 0 (H-Blank)
   - Waits for Mode 3 (Drawing)
   - Configures STAT to enable Mode 2 interrupts (bit 5)
   - Clears interrupt flags
   - Enables interrupts with `ei`
   - Executes `halt; nop`
   - **Expected behavior**: Mode 2 interrupt fires, STAT interrupt handler runs
   - STAT handler pops the return address (skipping the `nop` after `halt`)
   - Returns to `testcase_round_a_ret`
   - Counts cycles by incrementing B register until Mode 0 is reached
   - **Verifies**: B register should equal 1 (meaning Mode 0 was reached in 1 increment cycle)

4. **Round B Execution** (`testcase_round_b`):
   - Similar to Round A but uses `nop_area_b` with different cycle count
   - **Verifies**: B register should equal 2

5. **STAT Interrupt Handler** (at 0x0048):
   - Adds 2 to SP (pops the saved PC, skipping the `nop` after `halt`)
   - Returns (to the `testcase_round_a_ret` or `testcase_round_b_ret` label)

### Test Case Parameters

The test runs many configurations testing:
- **1-10 sprites at X=0**: Tests how sprite count affects timing (extra cycles: 2, 4, 5, 7, 8, 10, 11, 13, 14, 16)
- **10 sprites at various X positions**: Tests how sprite X coordinate affects timing
- **Split sprite groups**: Tests sprites at different X positions
- **Single sprites**: Tests individual sprite timing at different X positions
- **Pairs of sprites**: Tests sprite pairs with specific spacing
- **Sprite order**: Verifies sprite order doesn't affect timing

### Expected Timing Patterns

From the test cases, we can observe:
- Sprites at X=0-3, 8-11, 16-17: +2 cycles per sprite
- Sprites at X=4-7, 12-15: +1 cycle per sprite
- Sprites at X >= 160: Varies (0-2 cycles) based on exact position modulo 8
- Sprite position affects timing in 8-pixel alignment groups
- The pattern repeats every 8 pixels due to pixel FIFO fetching

## What The Test Expects

### Success Criteria
For each test case:
1. **Round A**: B register must equal 0x01 after Mode 0 is reached
2. **Round B**: B register must equal 0x02 after Mode 0 is reached
3. If either check fails, the test displays "TEST #XX FAILED" where XX is the test case ID

### Timing Expectations
The test expects specific "extra cycles" for different sprite configurations:
- The extra cycles parameter determines how many NOPs are in the NOP areas
- Round A uses (41 + extra_cycles) NOPs
- Round B uses (40 + extra_cycles) NOPs
- The difference of 1 NOP between rounds causes the B register difference (1 vs 2)

## What The Test Is Testing

### Core Behavior
The test validates the **Mode 3 (Drawing) duration** when sprites are present on a scanline. Specifically:

1. **Sprite Penalty Timing**: Each sprite that needs to be rendered adds cycles to Mode 3
2. **Pixel Alignment Effects**: The X position of sprites affects timing based on 8-pixel alignment
3. **Sprite Count Limits**: Up to 10 sprites can be rendered per scanline
4. **Sprite Visibility Culling**: Sprites at X >= 168 don't affect timing

### Technical Details

The Game Boy PPU operates as follows:
- **Mode 2 (OAM Scan)**: 80 dots (20 M-cycles), searches OAM for sprites on the current scanline
- **Mode 3 (Drawing)**: Variable duration (43-80 M-cycles), renders pixels using FIFO
- **Mode 0 (H-Blank)**: Remaining cycles to complete 456-dot scanline

Mode 3 duration increases when sprites are present because:
1. The sprite fetcher must pause the background fetcher
2. Each sprite fetch takes 6 dots (fetching tile number, low byte, high byte, then merging with FIFO)
3. Additional penalties occur based on pixel alignment and sprite position

The test uses the STAT interrupt timing to measure precisely how long Mode 3 takes with different sprite configurations.

## Potential Failure Reasons

### 1. Missing Sprite Penalty Implementation

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/SpriteFetcher.java`

**Issue**: The current implementation has a fixed 6-cycle sprite fetch:
- `FETCH_TILE_NO`: 1 cycle (line 72-73)
- `TICK_FETCH_TILE_DATA_LOW`: 1 extra tick (line 110-112)
- `FETCH_TILE_DATA_LOW`: 1 cycle (line 80-81)
- `TICK_FETCH_TILE_DATA_HIGH`: 1 extra tick (line 110-112)
- `FETCH_TILE_DATA_HIGH`: 1 cycle (line 88-89)
- `PUSH_TO_FIFO`: 1 cycle (line 105)

**Problem**: The test shows sprite penalties vary based on X position alignment. The emulator doesn't implement position-based timing penalties.

### 2. No Pixel-Alignment Penalty Logic

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/ScanlineController.java`

**Issue**: The `shouldPerformSpriteFetch()` method (lines 135-137) only checks if a sprite exists at position X, but doesn't account for pixel alignment penalties.

**Expected behavior**:
- Sprites whose X coordinate aligns with certain pixel boundaries cause different penalties
- The pattern appears to be related to the 8-pixel tile fetching cycle
- When X % 8 is in range 0-3 or 8-11 or 16-17, there's a 2-cycle penalty per sprite
- When X % 8 is in range 4-7 or 12-15, there's a 1-cycle penalty per sprite

**Current behavior**: All sprites cause the same 6-cycle fetch penalty regardless of position.

### 3. Sprite X Position Filtering

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/SpriteBuffer.java`

**Issue**: The `getSprite()` method (lines 16-20) checks if `uint(data.x()) - 8 <= x`, which determines when to start fetching a sprite.

**Potential problem**: The timing of when a sprite is "seen" by the pixel fetcher might not align with actual hardware behavior. The test shows sprites at X >= 168 (off-screen right edge) have different timing than expected.

### 4. Missing SCX Scroll Penalty

**Issue**: The test doesn't explicitly test SCX scrolling, but on real hardware, the SCX register value affects Mode 3 timing through the pixel discard logic.

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/ScanlineController.java` (lines 156-160)

The `shouldDiscardPixel()` logic is implemented, but the interaction between sprite fetching and pixel discarding might not be accurate.

### 5. Mode 0 Transition Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/PictureProcessingUnit.java`

**Issue**: The transition from Mode 3 to Mode 0 happens when `scanlineController.drawingComplete()` returns true (lines 96-101).

**Potential problem**: The timing of when Mode 0 interrupt fires might be off by a few cycles. The test is extremely sensitive to cycle-accurate timing.

### 6. Missing Sprite Fetch Abort Conditions

**Expected behavior**: On real hardware, sprite fetching can be interrupted or modified based on:
- Whether the background FIFO is empty or full
- Whether pixels are currently being discarded (SCX scrolling)
- The exact cycle within the 8-pixel fetch window

**Current behavior**: The emulator has a simple state machine that always completes sprite fetches in exactly 6 cycles.

## Summary of Likely Root Cause

The most likely reason this test fails is **missing position-based sprite penalty logic**. The emulator treats all sprite fetches as taking 6 cycles regardless of the sprite's X position, but the test clearly shows that:

1. Sprite X position modulo 8 affects timing
2. Different X positions cause different cycle penalties (0, 1, or 2 extra cycles per sprite)
3. The penalty pattern repeats every 8 pixels

To fix this, the emulator would need to:
1. Calculate the X position alignment penalty for each sprite
2. Add variable-length delays to sprite fetching based on X % 8
3. Implement the specific penalty table shown by the test cases
4. Handle edge cases for sprites at X >= 160

The exact penalty table appears to be:
- X % 8 in {0, 1, 2, 3}: +2 cycles per sprite
- X % 8 in {4, 5, 6, 7}: +1 cycle per sprite
- X >= 168: 0 cycles (sprite culled before rendering)

This complexity arises from the Game Boy's pixel FIFO architecture, where sprite fetching must synchronize with the background fetcher's 8-pixel fetch cycle.
