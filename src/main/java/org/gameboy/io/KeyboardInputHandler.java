package org.gameboy.io;

import com.google.inject.Inject;
import org.gameboy.components.joypad.Button;
import org.gameboy.components.joypad.annotations.*;

import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

public class KeyboardInputHandler {
    private final Map<Integer, Button> keyToButton;

    @Inject
    public KeyboardInputHandler(
            @ButtonUp Button up,
            @ButtonDown Button down,
            @ButtonLeft Button left,
            @ButtonRight Button right,
            @ButtonA Button a,
            @ButtonB Button b,
            @ButtonStart Button start,
            @ButtonSelect Button select) {
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
        Button button = keyToButton.get(key);
        if (button != null) {
            if (action == GLFW_PRESS) {
                button.press();
            } else if (action == GLFW_RELEASE) {
                button.release();
            }
        }
    }
}
