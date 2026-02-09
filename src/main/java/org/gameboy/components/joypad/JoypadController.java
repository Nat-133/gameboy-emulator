package org.gameboy.components.joypad;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gameboy.common.Interrupt;
import org.gameboy.common.InterruptController;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

@Singleton
public class JoypadController implements ButtonGroupListener {
    private static final int SELECTION_START_BIT = 4;
    private static final byte HIGH_BITS = (byte) 0xC0;

    private final ButtonGroup dpadGroup;
    private final ButtonGroup actionGroup;
    private final InterruptController interruptController;
    private GroupSelection selection = GroupSelection.NONE;
    private byte lastValue = (byte) 0xFF;

    @Inject
    public JoypadController(DpadButtonGroup dpadGroup, ActionButtonGroup actionGroup,
                            InterruptController interruptController) {
        this(dpadGroup, (ButtonGroup) actionGroup, interruptController);
    }

    public JoypadController(ButtonGroup dpadGroup, ButtonGroup actionGroup,
                           InterruptController interruptController) {
        this.dpadGroup = dpadGroup;
        this.actionGroup = actionGroup;
        this.interruptController = interruptController;

        dpadGroup.setListener(this);
        actionGroup.setListener(this);
    }

    public void write(byte value) {
        GroupSelection oldSelection = selection;
        selection = selectionFromByte(value);

        if (oldSelection != selection) {
            checkForInterrupt();
        }
    }

    public byte read() {
        byte result = computeValue();
        lastValue = result;
        return result;
    }

    @Override
    public void onGroupStateChanged(ButtonGroup group) {
        checkForInterrupt();
    }

    private GroupSelection selectionFromByte(byte value) {
        TwoBitValue bits = TwoBitValue.from(value, SELECTION_START_BIT);
        return GroupSelection.fromTwoBitValue(bits);
    }

    private byte selectionToBits() {
        return (byte) (selection.toTwoBitValue().ordinal() << SELECTION_START_BIT);
    }

    private boolean isDpadSelected() {
        return selection == GroupSelection.DPAD || selection == GroupSelection.BOTH;
    }

    private boolean isActionSelected() {
        return selection == GroupSelection.ACTION || selection == GroupSelection.BOTH;
    }

    private byte computeValue() {
        byte dpadBits = isDpadSelected() ? dpadGroup.getBits() : 0x0F;
        byte actionBits = isActionSelected() ? actionGroup.getBits() : 0x0F;
        byte buttons = (byte) (dpadBits & actionBits);
        return (byte) (HIGH_BITS | selectionToBits() | buttons);
    }

    private void checkForInterrupt() {
        byte currentValue = computeValue();
        byte oldLowNibble = (byte) (lastValue & 0x0F);
        byte newLowNibble = (byte) (currentValue & 0x0F);

        // Check for any 1→0 transition in low nibble
        boolean anyTransition = (oldLowNibble & ~newLowNibble) != 0;

        if (anyTransition) {
            interruptController.setInterrupt(Interrupt.JOYPAD);
        }

        lastValue = currentValue;
    }

    /**
     * Represents the button group selection state.
     * Ordered to match TwoBitValue ordinals for direct conversion.
     */
    enum GroupSelection {
        BOTH,   // 00
        ACTION, // 01
        DPAD,   // 10
        NONE;   // 11

        static GroupSelection fromTwoBitValue(TwoBitValue value) {
            return values()[value.ordinal()];
        }

        TwoBitValue toTwoBitValue() {
            return TwoBitValue.values()[ordinal()];
        }
    }
}
