package org.gameboy.io;

import org.gameboy.components.joypad.MultiSourceButton;
import org.gameboy.io.shell.*;

import java.util.ArrayList;
import java.util.List;

import static org.gameboy.io.ShellLayout.*;
import static org.gameboy.io.shell.BitmapTextElement.*;
import static org.lwjgl.opengl.GL41.*;

public class GameBoyShell {
    private static final float LAYOUT_ASPECT =
            ((float) WINDOW_WIDTH / WINDOW_HEIGHT) * VIEW_SCALE;

    // Colors matching the original shader
    private static final float[] COL_BG = {0.15f, 0.15f, 0.18f};
    private static final float[] COL_BODY = {0.78f, 0.78f, 0.75f};
    private static final float[] COL_BODY_EDGE = {0.68f, 0.68f, 0.65f};
    private static final float[] COL_BEZEL = {0.35f, 0.36f, 0.42f};
    private static final float[] COL_BEZEL_INNER = {0.28f, 0.29f, 0.34f};
    private static final float[] COL_AB = {0.55f, 0.14f, 0.22f};
    private static final float[] COL_AB_PRESSED = {0.40f, 0.08f, 0.14f};
    private static final float[] COL_SS = {0.50f, 0.50f, 0.48f};
    private static final float[] COL_SS_PRESSED = {0.38f, 0.38f, 0.36f};
    private static final float[] COL_LABEL = {0.28f, 0.28f, 0.38f};

    private final QuadRenderer quadRenderer = new QuadRenderer();
    private final ShaderCache shaderCache = new ShaderCache();
    private final List<ShellElement> elements = new ArrayList<>();
    private final List<ShellElement> interactiveElements = new ArrayList<>();

    // Direct references for per-frame updates
    private GameScreenElement gameScreen;
    private DpadElement dpad;
    private CircleButtonElement buttonA;
    private CircleButtonElement buttonB;
    private PillButtonElement buttonStart;
    private PillButtonElement buttonSelect;

    public void init(MultiSourceButton up, MultiSourceButton down,
                     MultiSourceButton left, MultiSourceButton right,
                     MultiSourceButton a, MultiSourceButton b,
                     MultiSourceButton start, MultiSourceButton select) {
        quadRenderer.init();
        createElements(up, down, left, right, a, b, start, select);

        for (ShellElement element : elements) {
            element.init(shaderCache);
        }
    }

    private void createElements(MultiSourceButton up, MultiSourceButton down,
                                MultiSourceButton left, MultiSourceButton right,
                                MultiSourceButton a, MultiSourceButton b,
                                MultiSourceButton start, MultiSourceButton select) {
        // 1. Background
        elements.add(new BackgroundElement(0, 0, 1, VIEW_SCALE,
                COL_BG[0], COL_BG[1], COL_BG[2]));

        // 2. Body
        float bodyX = BODY_CENTER_X - BODY_HALF_W;
        float bodyY = BODY_CENTER_Y - BODY_HALF_H;
        float bodyW = BODY_HALF_W * 2;
        float bodyH = BODY_HALF_H * 2;
        elements.add(new RoundedRectElement(bodyX, bodyY, bodyW, bodyH,
                COL_BODY[0], COL_BODY[1], COL_BODY[2],
                COL_BODY_EDGE[0], COL_BODY_EDGE[1], COL_BODY_EDGE[2],
                BODY_CORNER_RADIUS,
                0.5f, 0.6f));

        // 3. Bezel
        float bezelW = BEZEL_RIGHT - BEZEL_LEFT;
        float bezelH = BEZEL_BOTTOM - BEZEL_TOP;
        elements.add(new RoundedRectElement(BEZEL_LEFT, BEZEL_TOP, bezelW, bezelH,
                COL_BEZEL_INNER[0], COL_BEZEL_INNER[1], COL_BEZEL_INNER[2],
                COL_BEZEL[0], COL_BEZEL[1], COL_BEZEL[2],
                BEZEL_CORNER_RADIUS,
                0.0f, 1.0f));

        // 4. Game screen
        gameScreen = new GameScreenElement(
                SCREEN_LEFT, SCREEN_TOP,
                SCREEN_RIGHT - SCREEN_LEFT, SCREEN_BOTTOM - SCREEN_TOP);
        elements.add(gameScreen);

        // 6. D-pad
        dpad = new DpadElement(
                DPAD_CENTER_X, DPAD_CENTER_Y,
                DPAD_VISUAL_ARM_WIDTH, DPAD_ARM_LENGTH, DPAD_CORNER_RADIUS,
                HIT_PADDING, LAYOUT_ASPECT,
                up, down, left, right);
        elements.add(dpad);
        interactiveElements.add(dpad);

        // 7. A button
        buttonA = new CircleButtonElement(
                BUTTON_A_X, BUTTON_A_Y,
                AB_BUTTON_RADIUS, HIT_PADDING,
                PRESS_SHIFT_Y, LAYOUT_ASPECT,
                COL_AB[0], COL_AB[1], COL_AB[2],
                COL_AB_PRESSED[0], COL_AB_PRESSED[1], COL_AB_PRESSED[2],
                a);
        elements.add(buttonA);
        interactiveElements.add(buttonA);

        // 8. B button
        buttonB = new CircleButtonElement(
                BUTTON_B_X, BUTTON_B_Y,
                AB_BUTTON_RADIUS, HIT_PADDING,
                PRESS_SHIFT_Y, LAYOUT_ASPECT,
                COL_AB[0], COL_AB[1], COL_AB[2],
                COL_AB_PRESSED[0], COL_AB_PRESSED[1], COL_AB_PRESSED[2],
                b);
        elements.add(buttonB);
        interactiveElements.add(buttonB);

        // 9-12. Text labels
        float abPx = 0.006f;
        float abPxH = abPx * LAYOUT_ASPECT;
        elements.add(new BitmapTextElement(
                BUTTON_A_X, BUTTON_A_Y + AB_BUTTON_RADIUS + 0.005f + 2.5f * abPxH,
                abPx, abPxH, 0.0f, 4.0f,
                new int[]{CH_A},
                COL_LABEL[0], COL_LABEL[1], COL_LABEL[2]));
        elements.add(new BitmapTextElement(
                BUTTON_B_X, BUTTON_B_Y + AB_BUTTON_RADIUS + 0.005f + 2.5f * abPxH,
                abPx, abPxH, 0.0f, 4.0f,
                new int[]{CH_B},
                COL_LABEL[0], COL_LABEL[1], COL_LABEL[2]));

        float ssPx = 0.004f;
        float ssPxH = ssPx * LAYOUT_ASPECT;
        elements.add(new BitmapTextElement(
                START_X, START_SELECT_Y + START_SELECT_HEIGHT + 0.022f,
                ssPx, ssPxH, START_SELECT_ANGLE, 4.0f,
                new int[]{CH_S, CH_T, CH_A, CH_R, CH_T},
                COL_LABEL[0], COL_LABEL[1], COL_LABEL[2]));
        elements.add(new BitmapTextElement(
                SELECT_X, START_SELECT_Y + START_SELECT_HEIGHT + 0.022f,
                ssPx, ssPxH, START_SELECT_ANGLE, 4.0f,
                new int[]{CH_S, CH_E, CH_L, CH_E, CH_C, CH_T},
                COL_LABEL[0], COL_LABEL[1], COL_LABEL[2]));

        // 13. Start button
        buttonStart = new PillButtonElement(
                START_X, START_SELECT_Y,
                START_SELECT_WIDTH, START_SELECT_HEIGHT,
                START_SELECT_ANGLE, HIT_PADDING, PRESS_SHIFT_Y,
                COL_SS[0], COL_SS[1], COL_SS[2],
                COL_SS_PRESSED[0], COL_SS_PRESSED[1], COL_SS_PRESSED[2],
                start);
        elements.add(buttonStart);
        interactiveElements.add(buttonStart);

        // 14. Select button
        buttonSelect = new PillButtonElement(
                SELECT_X, START_SELECT_Y,
                START_SELECT_WIDTH, START_SELECT_HEIGHT,
                START_SELECT_ANGLE, HIT_PADDING, PRESS_SHIFT_Y,
                COL_SS[0], COL_SS[1], COL_SS[2],
                COL_SS_PRESSED[0], COL_SS_PRESSED[1], COL_SS_PRESSED[2],
                select);
        elements.add(buttonSelect);
        interactiveElements.add(buttonSelect);
    }

    public void render(int screenTextureId, float time, float[] buttonStates) {
        gameScreen.setScreenTextureId(screenTextureId);

        // Update button animation states
        dpad.setButtonStates(buttonStates[0], buttonStates[1], buttonStates[2], buttonStates[3]);
        buttonA.setPressState(buttonStates[4]);
        buttonB.setPressState(buttonStates[5]);
        buttonStart.setPressState(buttonStates[6]);
        buttonSelect.setPressState(buttonStates[7]);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        for (ShellElement element : elements) {
            element.render(quadRenderer, LAYOUT_ASPECT);
        }

        glDisable(GL_BLEND);
    }

    public List<ShellElement> getInteractiveElements() {
        return interactiveElements;
    }

    public void cleanup() {
        shaderCache.cleanup();
        quadRenderer.cleanup();
    }
}
