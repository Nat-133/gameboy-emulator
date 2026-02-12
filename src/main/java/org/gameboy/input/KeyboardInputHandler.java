package org.gameboy.input;

import com.google.inject.Inject;
import org.gameboy.components.joypad.Button;
import org.gameboy.components.joypad.annotations.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;

public class KeyboardInputHandler implements KeyListener {
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
            KeyEvent.VK_W, up,
            KeyEvent.VK_S, down,
            KeyEvent.VK_A, left,
            KeyEvent.VK_D, right,
            KeyEvent.VK_K, a,
            KeyEvent.VK_J, b,
            KeyEvent.VK_ENTER, start,
            KeyEvent.VK_SHIFT, select
        );
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Button button = keyToButton.get(e.getKeyCode());
        if (button != null) {
            button.press();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Button button = keyToButton.get(e.getKeyCode());
        if (button != null) {
            button.release();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}
