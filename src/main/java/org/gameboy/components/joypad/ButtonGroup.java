package org.gameboy.components.joypad;

public class ButtonGroup implements ButtonListener {
    private final Button[] buttons;
    private ButtonGroupListener listener;

    public ButtonGroup(Button bit0, Button bit1, Button bit2, Button bit3) {
        this.buttons = new Button[] { bit0, bit1, bit2, bit3 };
        for (Button button : buttons) {
            button.setListener(this);
        }
    }

    public byte getBits() {
        int bits = 0x0F;
        for (int i = 0; i < 4; i++) {
            if (buttons[i].isPressed()) {
                bits &= ~(1 << i);
            }
        }
        return (byte) bits;
    }

    public Button getButton(int index) {
        return buttons[index];
    }

    public void setListener(ButtonGroupListener listener) {
        this.listener = listener;
    }

    @Override
    public void onButtonChanged(Button button) {
        if (listener != null) {
            listener.onGroupStateChanged(this);
        }
    }
}
