package org.gameboy.components.joypad;

import org.gameboy.common.Interrupt;
import org.gameboy.common.InterruptController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.mockito.Mockito.*;

class JoypadControllerTest {

    private ButtonGroup dpadGroup;
    private ButtonGroup actionGroup;
    private InterruptController interruptController;
    private JoypadController controller;

    @BeforeEach
    void setUp() {
        dpadGroup = new ButtonGroup(
            new Button(),
            new Button(),
            new Button(),
            new Button()
        );
        actionGroup = new ButtonGroup(
            new Button(),
            new Button(),
            new Button(),
            new Button()
        );
        interruptController = mock(InterruptController.class);
        controller = new JoypadController(dpadGroup, actionGroup, interruptController);
    }

    @Nested
    class ReadBehavior {

        @Test
        void initialState_neitherGroupSelected_returnsAllReleased() {
            byte result = controller.read();

            // Bits 7-6 = 11, bits 5-4 = 11 (NONE), bits 3-0 = 1111
            assertThatHex(result).isEqualTo((byte) 0xFF);
        }

        @Test
        void write_dpadSelected_selectionBitsEchoed() {
            controller.write((byte) 0x20);
            byte result = controller.read();

            assertThatHex((byte) (result & 0x30)).isEqualTo((byte) 0x20);
        }

        @Test
        void write_actionSelected_selectionBitsEchoed() {
            controller.write((byte) 0x10);
            byte result = controller.read();

            assertThatHex((byte) (result & 0x30)).isEqualTo((byte) 0x10);
        }

        @Test
        void write_bothSelected_selectionBitsEchoed() {
            controller.write((byte) 0x00);
            byte result = controller.read();

            assertThatHex((byte) (result & 0x30)).isEqualTo((byte) 0x00);
        }

        @Test
        void write_onlySelectionBitsStored() {
            controller.write((byte) 0xFF);
            byte result = controller.read();

            assertThatHex((byte) (result & 0x30)).isEqualTo((byte) 0x30);
            assertThatHex((byte) (result & 0xC0)).isEqualTo((byte) 0xC0);
        }
    }

    @Nested
    class DpadSelection {

        @Test
        void rightPressed_bit0IsLow() {
            controller.write((byte) 0x20);
            dpadGroup.getButton(0).press();

            byte result = controller.read();

            assertThatHex((byte) (result & 0x01)).isEqualTo((byte) 0x00);
            assertThatHex((byte) (result & 0x0E)).isEqualTo((byte) 0x0E);
        }

        @Test
        void leftPressed_bit1IsLow() {
            controller.write((byte) 0x20);
            dpadGroup.getButton(1).press();

            assertThatHex((byte) (controller.read() & 0x02)).isEqualTo((byte) 0x00);
        }

        @Test
        void upPressed_bit2IsLow() {
            controller.write((byte) 0x20);
            dpadGroup.getButton(2).press();

            assertThatHex((byte) (controller.read() & 0x04)).isEqualTo((byte) 0x00);
        }

        @Test
        void downPressed_bit3IsLow() {
            controller.write((byte) 0x20);
            dpadGroup.getButton(3).press();

            assertThatHex((byte) (controller.read() & 0x08)).isEqualTo((byte) 0x00);
        }

        @Test
        void actionButtonPressed_notVisible() {
            controller.write((byte) 0x20);
            actionGroup.getButton(0).press();

            assertThatHex((byte) (controller.read() & 0x0F)).isEqualTo((byte) 0x0F);
        }
    }

    @Nested
    class ActionButtonGroupSelection {

        @Test
        void aPressed_bit0IsLow() {
            controller.write((byte) 0x10);
            actionGroup.getButton(0).press();

            byte result = controller.read();

            assertThatHex((byte) (result & 0x01)).isEqualTo((byte) 0x00);
            assertThatHex((byte) (result & 0x0E)).isEqualTo((byte) 0x0E);
        }

        @Test
        void bPressed_bit1IsLow() {
            controller.write((byte) 0x10);
            actionGroup.getButton(1).press();

            assertThatHex((byte) (controller.read() & 0x02)).isEqualTo((byte) 0x00);
        }

        @Test
        void selectPressed_bit2IsLow() {
            controller.write((byte) 0x10);
            actionGroup.getButton(2).press();

            assertThatHex((byte) (controller.read() & 0x04)).isEqualTo((byte) 0x00);
        }

        @Test
        void startPressed_bit3IsLow() {
            controller.write((byte) 0x10);
            actionGroup.getButton(3).press();

            assertThatHex((byte) (controller.read() & 0x08)).isEqualTo((byte) 0x00);
        }

        @Test
        void dpadButtonPressed_notVisible() {
            controller.write((byte) 0x10);
            dpadGroup.getButton(0).press();

            assertThatHex((byte) (controller.read() & 0x0F)).isEqualTo((byte) 0x0F);
        }
    }

    @Nested
    class BothSelected {

        @Test
        void dpadButtonPressed_appearsInResult() {
            controller.write((byte) 0x00);
            dpadGroup.getButton(0).press();

            assertThatHex((byte) (controller.read() & 0x01)).isEqualTo((byte) 0x00);
        }

        @Test
        void actionButtonPressed_appearsInResult() {
            controller.write((byte) 0x00);
            actionGroup.getButton(0).press();

            assertThatHex((byte) (controller.read() & 0x01)).isEqualTo((byte) 0x00);
        }

        @Test
        void buttonFromEachGroup_bothAppear() {
            controller.write((byte) 0x00);
            dpadGroup.getButton(0).press();  // Right
            actionGroup.getButton(1).press(); // B

            byte result = controller.read();

            assertThatHex((byte) (result & 0x01)).isEqualTo((byte) 0x00);
            assertThatHex((byte) (result & 0x02)).isEqualTo((byte) 0x00);
            assertThatHex((byte) (result & 0x0C)).isEqualTo((byte) 0x0C);
        }

        @Test
        void noButtonsPressed_allBitsHigh() {
            controller.write((byte) 0x00);

            assertThatHex((byte) (controller.read() & 0x0F)).isEqualTo((byte) 0x0F);
        }
    }

    @Nested
    class Interrupts {

        @Test
        void buttonPressed_whileGroupSelected_firesInterrupt() {
            controller.write((byte) 0x20);
            controller.read(); // Establish baseline

            dpadGroup.getButton(0).press();

            verify(interruptController).setInterrupt(Interrupt.JOYPAD);
        }

        @Test
        void buttonPressed_whileGroupNotSelected_noInterrupt() {
            controller.write((byte) 0x10); // Action selected
            controller.read();

            dpadGroup.getButton(0).press(); // D-pad button

            verify(interruptController, never()).setInterrupt(any());
        }

        @Test
        void buttonReleased_noInterrupt() {
            controller.write((byte) 0x20);
            dpadGroup.getButton(0).press();
            controller.read();
            clearInvocations(interruptController);

            dpadGroup.getButton(0).release();

            verify(interruptController, never()).setInterrupt(any());
        }

        @Test
        void groupSelected_whileButtonAlreadyPressed_firesInterrupt() {
            dpadGroup.getButton(0).press();
            controller.write((byte) 0x30); // Neither selected
            controller.read();

            controller.write((byte) 0x20); // Select D-pad

            verify(interruptController).setInterrupt(Interrupt.JOYPAD);
        }

        @Test
        void sameSelectionWrittenAgain_noInterrupt() {
            controller.write((byte) 0x20);
            controller.read();

            controller.write((byte) 0x20);

            verify(interruptController, never()).setInterrupt(any());
        }

        @Test
        void actionButtonPressed_whileActionSelected_firesInterrupt() {
            controller.write((byte) 0x10);
            controller.read();

            actionGroup.getButton(0).press();

            verify(interruptController).setInterrupt(Interrupt.JOYPAD);
        }
    }
}
