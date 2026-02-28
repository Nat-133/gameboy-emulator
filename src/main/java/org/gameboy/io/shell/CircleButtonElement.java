package org.gameboy.io.shell;

import org.gameboy.components.joypad.MultiSourceButton;

public final class CircleButtonElement extends ShellElement {
    private final float centerX, centerY;
    private final float visualRadius;
    private final float hitRadius;
    private final float pressShift;
    private final float layoutAspect;
    private final float colorR, colorG, colorB;
    private final float pressedR, pressedG, pressedB;
    private final MultiSourceButton button;

    private int uAspectLoc;
    private int uCenterLoc;
    private int uRadiusLoc;
    private int uColorLoc;
    private int uPressedColorLoc;
    private int uPressStateLoc;
    private int uPressShiftLoc;

    private float pressState;

    public CircleButtonElement(float centerX, float centerY,
                               float visualRadius, float hitPadding,
                               float pressShift, float layoutAspect,
                               float colorR, float colorG, float colorB,
                               float pressedR, float pressedG, float pressedB,
                               MultiSourceButton button) {
        super(centerX - visualRadius - 0.01f,
              centerY - visualRadius * layoutAspect - 0.01f,
              (visualRadius + 0.01f) * 2,
              (visualRadius * layoutAspect + 0.01f) * 2);
        this.centerX = centerX;
        this.centerY = centerY;
        this.visualRadius = visualRadius;
        this.hitRadius = visualRadius + hitPadding;
        this.pressShift = pressShift;
        this.layoutAspect = layoutAspect;
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
        return "shaders/shell/circle_button_fragment.glsl";
    }

    @Override
    protected void initUniforms() {
        uAspectLoc = shader.getUniformLocation("uAspect");
        uCenterLoc = shader.getUniformLocation("uCenter");
        uRadiusLoc = shader.getUniformLocation("uRadius");
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
        shader.setUniform1f(uAspectLoc, aspect);
        shader.setUniform2f(uCenterLoc, centerX, centerY);
        shader.setUniform1f(uRadiusLoc, visualRadius);
        shader.setUniform3f(uColorLoc, colorR, colorG, colorB);
        shader.setUniform3f(uPressedColorLoc, pressedR, pressedG, pressedB);
        shader.setUniform1f(uPressStateLoc, pressState);
        shader.setUniform1f(uPressShiftLoc, pressShift);
    }

    @Override
    public MultiSourceButton hitTest(float mx, float my) {
        float dx = (mx - centerX) * layoutAspect;
        float dy = my - centerY;
        float ra = hitRadius * layoutAspect;
        if ((dx * dx + dy * dy) <= ra * ra) {
            return button;
        }
        return null;
    }
}
