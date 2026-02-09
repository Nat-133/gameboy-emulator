package org.gameboy.components.joypad;

import org.junit.jupiter.api.Test;

import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.mockito.Mockito.*;

class ButtonGroupTest {

    @Test
    void initialState_allButtonsReleased_returns0x0F() {
        ButtonGroup group = createButtonGroup();

        assertThatHex(group.getBits()).isEqualTo((byte) 0x0F);
    }

    @Test
    void bit0ButtonPressed_bit0IsLow() {
        ButtonGroup group = createButtonGroup();

        group.getButton(0).press(); // Right

        assertThatHex(group.getBits()).isEqualTo((byte) 0x0E);
    }

    @Test
    void bit1ButtonPressed_bit1IsLow() {
        ButtonGroup group = createButtonGroup();

        group.getButton(1).press(); // Left

        assertThatHex(group.getBits()).isEqualTo((byte) 0x0D);
    }

    @Test
    void bit2ButtonPressed_bit2IsLow() {
        ButtonGroup group = createButtonGroup();

        group.getButton(2).press(); // Up

        assertThatHex(group.getBits()).isEqualTo((byte) 0x0B);
    }

    @Test
    void bit3ButtonPressed_bit3IsLow() {
        ButtonGroup group = createButtonGroup();

        group.getButton(3).press(); // Down

        assertThatHex(group.getBits()).isEqualTo((byte) 0x07);
    }

    @Test
    void multipleButtonsPressed_multipleBitsLow() {
        ButtonGroup group = createButtonGroup();

        group.getButton(0).press(); // Right
        group.getButton(2).press(); // Up

        assertThatHex(group.getBits()).isEqualTo((byte) 0x0A); // 1010
    }

    @Test
    void buttonReleased_bitGoesHigh() {
        ButtonGroup group = createButtonGroup();
        group.getButton(0).press();

        group.getButton(0).release();

        assertThatHex(group.getBits()).isEqualTo((byte) 0x0F);
    }

    @Test
    void buttonChanged_notifiesListener() {
        ButtonGroup group = createButtonGroup();
        ButtonGroupListener listener = mock(ButtonGroupListener.class);
        group.setListener(listener);

        group.getButton(0).press();

        verify(listener).onGroupStateChanged(group);
    }

    @Test
    void buttonReleased_notifiesListener() {
        ButtonGroup group = createButtonGroup();
        group.getButton(0).press();
        ButtonGroupListener listener = mock(ButtonGroupListener.class);
        group.setListener(listener);

        group.getButton(0).release();

        verify(listener).onGroupStateChanged(group);
    }

    private ButtonGroup createButtonGroup() {
        return new ButtonGroup(
            new Button(),
            new Button(),
            new Button(),
            new Button()
        );
    }
}
