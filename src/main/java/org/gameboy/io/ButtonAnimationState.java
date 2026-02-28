package org.gameboy.io;

import org.gameboy.components.joypad.MultiSourceButton;

public class ButtonAnimationState {
    private static final float PRESS_SPEED = 1.0f / 0.08f;   // ~80ms to fully press
    private static final float RELEASE_SPEED = 1.0f / 0.12f;  // ~120ms to fully release

    private final MultiSourceButton button;
    private float progress;  // 0.0 = released, 1.0 = fully pressed

    public ButtonAnimationState(MultiSourceButton button) {
        this.button = button;
        this.progress = button.isPressed() ? 1.0f : 0.0f;
    }

    public void update(float deltaTime) {
        float target = button.isPressed() ? 1.0f : 0.0f;
        if (progress < target) {
            progress = Math.min(target, progress + PRESS_SPEED * deltaTime);
        } else if (progress > target) {
            progress = Math.max(target, progress - RELEASE_SPEED * deltaTime);
        }
    }

    public float getProgress() {
        return progress;
    }
}
