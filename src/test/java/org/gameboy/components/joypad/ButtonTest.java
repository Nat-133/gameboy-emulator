package org.gameboy.components.joypad;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ButtonTest {

    @Test
    void initialState_notPressed() {
        Button button = new Button();

        assertThat(button.isPressed()).isFalse();
    }

    @Test
    void press_becomesPressed() {
        Button button = new Button();

        button.press();

        assertThat(button.isPressed()).isTrue();
    }

    @Test
    void release_becomesNotPressed() {
        Button button = new Button();
        button.press();

        button.release();

        assertThat(button.isPressed()).isFalse();
    }

    @Test
    void press_notifiesListener() {
        Button button = new Button();
        ButtonListener listener = mock(ButtonListener.class);
        button.setListener(listener);

        button.press();

        verify(listener).onButtonChanged(button);
    }

    @Test
    void release_notifiesListener() {
        Button button = new Button();
        ButtonListener listener = mock(ButtonListener.class);
        button.setListener(listener);
        button.press();
        clearInvocations(listener);

        button.release();

        verify(listener).onButtonChanged(button);
    }

    @Test
    void pressWhenAlreadyPressed_doesNotNotify() {
        Button button = new Button();
        ButtonListener listener = mock(ButtonListener.class);
        button.press(); // Press before listener attached
        button.setListener(listener);

        button.press(); // Press again

        verify(listener, never()).onButtonChanged(any());
    }

    @Test
    void releaseWhenNotPressed_doesNotNotify() {
        Button button = new Button();
        ButtonListener listener = mock(ButtonListener.class);
        button.setListener(listener);

        button.release(); // Not pressed, so no change

        verify(listener, never()).onButtonChanged(any());
    }
}
