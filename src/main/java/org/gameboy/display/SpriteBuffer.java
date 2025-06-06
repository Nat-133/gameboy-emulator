package org.gameboy.display;

import org.gameboy.common.Clock;
import org.gameboy.utils.BitUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.gameboy.utils.BitUtilities.uint;

public class SpriteBuffer {
    private final ObjectAttributeMemory oam;
    private final List<SpriteData> buffer;

    public SpriteBuffer(ObjectAttributeMemory oam) {
        this.oam = oam;
        buffer = new ArrayList<>(10);
    }

    public Optional<SpriteData> getSprite(int x) {
        return buffer.stream()
                .filter(data -> data.x() == x)
                .findFirst();
    };

    public void scanOAM(ObjectAttributeMemory oam, int LY, Clock clock) {
        int spriteHeight = 8;
        buffer.clear();
        for (int i=0; i<ObjectAttributeMemory.SIZE; i+=4) {
            short spriteCoordinate = oam.read(i);
            byte yPos = BitUtilities.lower_byte(spriteCoordinate);
            byte xPos = BitUtilities.upper_byte(spriteCoordinate);

            clock.tick();

            if (LY + 16 >= uint(yPos)
                    && LY + 16 < uint(yPos) + spriteHeight
                    && uint(xPos) > 0
                    && buffer.size() <= 10) {
                short spriteData = oam.read(i + 2);
                byte tileNumber = BitUtilities.lower_byte(spriteData);
                byte flags = BitUtilities.upper_byte(spriteData);

                buffer.add(new SpriteData(yPos, xPos, tileNumber, flags));
            }

            clock.tick();
        }
    }
}
