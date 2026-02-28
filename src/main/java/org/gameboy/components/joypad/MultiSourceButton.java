package org.gameboy.components.joypad;

import java.util.HashSet;
import java.util.Set;

public class MultiSourceButton {
    private final Button button;
    private final Set<String> activeSources = new HashSet<>();

    public MultiSourceButton(Button button) {
        this.button = button;
    }

    public void press(String source) {
        boolean wasEmpty = activeSources.isEmpty();
        activeSources.add(source);
        if (wasEmpty) {
            button.press();
        }
    }

    public void release(String source) {
        activeSources.remove(source);
        if (activeSources.isEmpty()) {
            button.release();
        }
    }

    public boolean isPressed() {
        return button.isPressed();
    }
}
