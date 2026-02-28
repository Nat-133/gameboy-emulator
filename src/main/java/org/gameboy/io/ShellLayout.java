package org.gameboy.io;

public final class ShellLayout {
    private ShellLayout() {}

    // Window dimensions
    public static final int WINDOW_WIDTH = 540;
    public static final int WINDOW_HEIGHT = 740;

    // Fraction of the original tall layout that's visible (crops bottom)
    public static final float VIEW_SCALE = 0.77f;

    // All coordinates are in normalized [0,1] space (origin top-left)
    // Body (center and half-size for the rounded rect)
    public static final float BODY_CENTER_X = 0.5f;
    public static final float BODY_CENTER_Y = 0.5f;
    public static final float BODY_HALF_W = 0.46f;
    public static final float BODY_HALF_H = 0.47f;
    public static final float BODY_CORNER_RADIUS = 0.04f;

    // Screen region (where game texture is composited)
    public static final float SCREEN_LEFT = 0.13f;
    public static final float SCREEN_RIGHT = 0.87f;
    public static final float SCREEN_TOP = 0.105f;
    public static final float SCREEN_BOTTOM = 0.40f;

    // Screen bezel (slightly larger than screen)
    public static final float BEZEL_LEFT = 0.09f;
    public static final float BEZEL_RIGHT = 0.91f;
    public static final float BEZEL_TOP = 0.075f;
    public static final float BEZEL_BOTTOM = 0.44f;
    public static final float BEZEL_CORNER_RADIUS = 0.02f;

    // D-pad
    public static final float DPAD_CENTER_X = 0.25f;
    public static final float DPAD_CENTER_Y = 0.56f;
    public static final float DPAD_ARM_WIDTH = 0.065f;       // hit-test arm width
    public static final float DPAD_VISUAL_ARM_WIDTH = 0.038f; // visual arm width (shader)
    public static final float DPAD_ARM_LENGTH = 0.095f;
    public static final float DPAD_CORNER_RADIUS = 0.008f;

    // A and B buttons
    public static final float BUTTON_A_X = 0.80f;
    public static final float BUTTON_A_Y = 0.52f;
    public static final float BUTTON_B_X = 0.65f;
    public static final float BUTTON_B_Y = 0.57f;
    public static final float AB_BUTTON_RADIUS = 0.045f;

    // Start and Select buttons
    public static final float START_X = 0.57f;
    public static final float SELECT_X = 0.43f;
    public static final float START_SELECT_Y = 0.68f;
    public static final float START_SELECT_WIDTH = 0.07f;
    public static final float START_SELECT_HEIGHT = 0.022f;
    public static final float START_SELECT_RADIUS = 0.011f;
    // Rotation angle for start/select (slight tilt)
    public static final float START_SELECT_ANGLE = -0.22f;

    // Button press animation shift (in normalized coords)
    public static final float PRESS_SHIFT_Y = 0.003f;

    // Hit test regions for mouse input (slightly larger than visual for usability)
    public static final float HIT_PADDING = 0.015f;
}
