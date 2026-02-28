package org.gameboy.io.shell;

import org.gameboy.components.joypad.MultiSourceButton;
import org.gameboy.io.ShellLayout;

public final class DpadElement extends ShellElement {
    private final float centerX, centerY;
    private final float armWidth, armLength, cornerRadius;
    private final float hitPadding;
    private final float layoutAspect;

    private final MultiSourceButton up, down, left, right;

    private int uAspectLoc;
    private int uCenterLoc;
    private int uArmWidthLoc;
    private int uArmLengthLoc;
    private int uCornerRLoc;
    private int uColorLoc;
    private int uPressedColorLoc;
    private int uButtonsLoc;

    private final float[] buttonStates = new float[4];

    public DpadElement(float centerX, float centerY,
                       float armWidth, float armLength, float cornerRadius,
                       float hitPadding, float layoutAspect,
                       MultiSourceButton up, MultiSourceButton down,
                       MultiSourceButton left, MultiSourceButton right) {
        super(centerX - armLength - 0.01f,
              centerY - armLength * layoutAspect - 0.01f,
              (armLength + 0.01f) * 2,
              (armLength * layoutAspect + 0.01f) * 2);
        this.centerX = centerX;
        this.centerY = centerY;
        this.armWidth = armWidth;
        this.armLength = armLength;
        this.cornerRadius = cornerRadius;
        this.hitPadding = hitPadding;
        this.layoutAspect = layoutAspect;
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
    }

    @Override
    protected String getFragmentShaderPath() {
        return "shaders/shell/dpad_fragment.glsl";
    }

    @Override
    protected void initUniforms() {
        uAspectLoc = shader.getUniformLocation("uAspect");
        uCenterLoc = shader.getUniformLocation("uCenter");
        uArmWidthLoc = shader.getUniformLocation("uArmWidth");
        uArmLengthLoc = shader.getUniformLocation("uArmLength");
        uCornerRLoc = shader.getUniformLocation("uCornerR");
        uColorLoc = shader.getUniformLocation("uColor");
        uPressedColorLoc = shader.getUniformLocation("uPressedColor");
        uButtonsLoc = shader.getUniformLocation("uButtons");
    }

    public void setButtonStates(float upState, float downState, float leftState, float rightState) {
        buttonStates[0] = upState;
        buttonStates[1] = downState;
        buttonStates[2] = leftState;
        buttonStates[3] = rightState;
    }

    @Override
    protected void setElementUniforms(float aspect) {
        shader.setUniform1f(uAspectLoc, aspect);
        shader.setUniform2f(uCenterLoc, centerX, centerY);
        shader.setUniform1f(uArmWidthLoc, armWidth);
        shader.setUniform1f(uArmLengthLoc, armLength);
        shader.setUniform1f(uCornerRLoc, cornerRadius);
        shader.setUniform3f(uColorLoc, 0.18f, 0.18f, 0.22f);
        shader.setUniform3f(uPressedColorLoc, 0.12f, 0.12f, 0.15f);
        shader.setUniform1fv(uButtonsLoc, buttonStates);
    }

    @Override
    public MultiSourceButton hitTest(float mx, float my) {
        MultiSourceButton result;
        result = testArm(mx, my, 0, -1);
        if (result != null) return result;
        result = testArm(mx, my, 0, 1);
        if (result != null) return result;
        result = testArm(mx, my, -1, 0);
        if (result != null) return result;
        result = testArm(mx, my, 1, 0);
        return result;
    }

    private MultiSourceButton testArm(float mx, float my, int dx, int dy) {
        float cx = centerX;
        float cy = centerY;
        float armW = ShellLayout.DPAD_ARM_WIDTH;
        float armL = ShellLayout.DPAD_ARM_LENGTH;
        float armWY = armW * layoutAspect;
        float armLY = armL * layoutAspect;
        float pad = hitPadding;

        if (dx == 0) {
            float armLeft = cx - armW - pad;
            float armRight = cx + armW + pad;
            if (dy < 0) {
                float armTop = cy - armLY - pad;
                float armBottom = cy - armWY * 0.3f;
                if (mx >= armLeft && mx <= armRight && my >= armTop && my <= armBottom) return up;
            } else {
                float armTop = cy + armWY * 0.3f;
                float armBottom = cy + armLY + pad;
                if (mx >= armLeft && mx <= armRight && my >= armTop && my <= armBottom) return down;
            }
        } else {
            float armTop = cy - armWY - pad;
            float armBottom = cy + armWY + pad;
            if (dx < 0) {
                float armLeft = cx - armL - pad;
                float armRight = cx - armW * 0.3f;
                if (mx >= armLeft && mx <= armRight && my >= armTop && my <= armBottom) return left;
            } else {
                float armLeft = cx + armW * 0.3f;
                float armRight = cx + armL + pad;
                if (mx >= armLeft && mx <= armRight && my >= armTop && my <= armBottom) return right;
            }
        }
        return null;
    }
}
