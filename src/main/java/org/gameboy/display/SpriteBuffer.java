package org.gameboy.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.gameboy.utils.BitUtilities.uint;

public class SpriteBuffer {
    private final List<SpriteData> buffer;

    public SpriteBuffer() {
        buffer = new ArrayList<>(10);
    }

    public Optional<SpriteData> getSprite(int x) {
        return buffer.stream()
                .filter(data -> uint(data.x()) - 8 <= x)
                .findFirst();
    }

    public Optional<SpriteData> popSprite(int x) {
        Optional<SpriteData> firstMatchingSprite = getSprite(x);

        firstMatchingSprite.ifPresent(buffer::remove);

        return firstMatchingSprite;
    }

    public void add(SpriteData spriteData) {
        buffer.add(spriteData);
    }

    public void clear() {
        buffer.clear();
    }

    public int spriteCount() {
        return buffer.size();
    }
}
