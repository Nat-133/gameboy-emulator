package org.gameboy.display;

import org.gameboy.common.Clock;
import org.gameboy.utils.BitUtilities;

import static org.gameboy.utils.BitUtilities.uint;

public class OamScanController {
    private final ObjectAttributeMemory oam;
    private final Clock clock;
    private final SpriteBuffer spriteBuffer;
    private State state;
    private int currentSpriteIndex;
    private int LY;
    private int spriteHeight;
    private byte currentSpriteY;
    private byte currentSpriteX;

    public OamScanController(ObjectAttributeMemory oam, Clock clock, SpriteBuffer spriteBuffer) {
        this.oam = oam;
        this.clock = clock;
        this.spriteBuffer = spriteBuffer;
    }

    public void setupOamScan(int LY) {
        state = State.READ_SPRITE_COORDINATE;
        currentSpriteIndex = 0;
        this.LY = LY;
        this.spriteHeight = 8;
        spriteBuffer.clear();
    }

    public void performOneClockCycle() {
        state = switch(state) {
            case READ_SPRITE_COORDINATE -> readSpriteCoordinate();
            case READ_SPRITE_DATA -> readSpriteData();
        };
    }

    private State readSpriteCoordinate() {
        short spriteCoordinate = oam.read(currentSpriteIndex * 4);
        currentSpriteY = BitUtilities.lower_byte(spriteCoordinate);
        currentSpriteX = BitUtilities.upper_byte(spriteCoordinate);

        clock.tick();
        return State.READ_SPRITE_DATA;
    }

    private State readSpriteData() {
        short spriteData = oam.read(currentSpriteIndex*4 + 2);
        if (LY + 16 >= uint(currentSpriteY)
                && LY + 16 < uint(currentSpriteY) + spriteHeight
                && uint(currentSpriteX) > 0
                && spriteBuffer.spriteCount() <= 10) {
            byte tileNumber = BitUtilities.lower_byte(spriteData);
            byte flags = BitUtilities.upper_byte(spriteData);

            spriteBuffer.add(new SpriteData(currentSpriteY, currentSpriteX, tileNumber, flags));
        }

        currentSpriteIndex++;

        return State.READ_SPRITE_COORDINATE;
    }

    private enum State {
        READ_SPRITE_COORDINATE,
        READ_SPRITE_DATA
    }
}
