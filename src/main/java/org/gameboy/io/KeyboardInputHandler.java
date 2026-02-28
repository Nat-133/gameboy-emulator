package org.gameboy.io;

import com.google.inject.Inject;
import org.gameboy.components.joypad.MultiSourceButton;
import org.gameboy.components.joypad.annotations.*;

import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

public class KeyboardInputHandler {
    private static final String SOURCE = "keyboard";
    private final Map<Integer, MultiSourceButton> keyToButton;

    @Inject
    public KeyboardInputHandler(
            @ButtonUp MultiSourceButton up,
            @ButtonDown MultiSourceButton down,
            @ButtonLeft MultiSourceButton left,
            @ButtonRight MultiSourceButton right,
            @ButtonA MultiSourceButton a,
            @ButtonB MultiSourceButton b,
            @ButtonStart MultiSourceButton start,
            @ButtonSelect MultiSourceButton select) {
        this.keyToButton = Map.of(
            GLFW_KEY_W, up,
            GLFW_KEY_S, down,
            GLFW_KEY_A, left,
            GLFW_KEY_D, right,
            GLFW_KEY_K, a,
            GLFW_KEY_J, b,
            GLFW_KEY_ENTER, start,
            GLFW_KEY_LEFT_SHIFT, select
        );
    }

    public void registerCallbacks(long window) {
        glfwSetKeyCallback(window, this::handleKey);
    }

    private void handleKey(long window, int key, int scancode, int action, int mods) {
        MultiSourceButton button = keyToButton.get(key);
        if (button != null) {
            if (action == GLFW_PRESS) {
                button.press(SOURCE);
            } else if (action == GLFW_RELEASE) {
                button.release(SOURCE);
            }
        }
    }
}
