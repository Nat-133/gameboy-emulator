package org.gameboy.components.joypad;

public class Button {
    private ButtonListener listener;
    private boolean pressed = false;

    public void press() {
        if (!pressed) {
            pressed = true;
            notifyListener();
        }
    }

    public void release() {
        if (pressed) {
            pressed = false;
            notifyListener();
        }
    }

    public boolean isPressed() {
        return pressed;
    }

    public void setListener(ButtonListener listener) {
        this.listener = listener;
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onButtonChanged(this);
        }
    }
}
