package org.gameboy.io.shell;

import org.gameboy.components.joypad.MultiSourceButton;

public final class PillButtonElement extends ShellElement {
    private final float centerX, centerY;
    private final float halfWidth, halfHeight;
    private final float angle;
    private final float pressShift;
    private final float hitPadding;
    private final float colorR, colorG, colorB;
    private final float pressedR, pressedG, pressedB;
    private final MultiSourceButton button;

    private int uCenterLoc;
    private int uHalfSizeLoc;
    private int uAngleLoc;
    private int uColorLoc;
    private int uPressedColorLoc;
    private int uPressStateLoc;
    private int uPressShiftLoc;

    private float pressState;

    public PillButtonElement(float centerX, float centerY,
                             float halfWidth, float halfHeight,
                             float angle, float hitPadding, float pressShift,
                             float colorR, float colorG, float colorB,
                             float pressedR, float pressedG, float pressedB,
                             MultiSourceButton button) {
        super(centerX - halfWidth - 0.02f,
              centerY - halfHeight - 0.02f,
              (halfWidth + 0.02f) * 2,
              (halfHeight + 0.02f) * 2);
        this.centerX = centerX;
        this.centerY = centerY;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.angle = angle;
        this.hitPadding = hitPadding;
        this.pressShift = pressShift;
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;
        this.pressedR = pressedR;
        this.pressedG = pressedG;
        this.pressedB = pressedB;
        this.button = button;
    }

    @Override
    protected String getFragmentShaderPath() {
        return "shaders/shell/pill_button_fragment.glsl";
    }

    @Override
    protected void initUniforms() {
        uCenterLoc = shader.getUniformLocation("uCenter");
        uHalfSizeLoc = shader.getUniformLocation("uHalfSize");
        uAngleLoc = shader.getUniformLocation("uAngle");
        uColorLoc = shader.getUniformLocation("uColor");
        uPressedColorLoc = shader.getUniformLocation("uPressedColor");
        uPressStateLoc = shader.getUniformLocation("uPressState");
        uPressShiftLoc = shader.getUniformLocation("uPressShift");
    }

    public void setPressState(float state) {
        this.pressState = state;
    }

    @Override
    protected void setElementUniforms(float aspect) {
        shader.setUniform2f(uCenterLoc, centerX, centerY);
        shader.setUniform2f(uHalfSizeLoc, halfWidth, halfHeight);
        shader.setUniform1f(uAngleLoc, angle);
        shader.setUniform3f(uColorLoc, colorR, colorG, colorB);
        shader.setUniform3f(uPressedColorLoc, pressedR, pressedG, pressedB);
        shader.setUniform1f(uPressStateLoc, pressState);
        shader.setUniform1f(uPressShiftLoc, pressShift);
    }

    @Override
    public MultiSourceButton hitTest(float mx, float my) {
        float hw = halfWidth + hitPadding;
        float hh = halfHeight + hitPadding;
        float cos = (float) Math.cos(-angle);
        float sin = (float) Math.sin(-angle);
        float ddx = mx - centerX;
        float ddy = my - centerY;
        float rx = ddx * cos - ddy * sin;
        float ry = ddx * sin + ddy * cos;
        float r = Math.min(hw, hh);
        float ex = Math.max(Math.abs(rx) - (hw - r), 0.0f);
        float ey = Math.max(Math.abs(ry) - (hh - r), 0.0f);
        if ((ex * ex + ey * ey) <= r * r) {
            return button;
        }
        return null;
    }
}
