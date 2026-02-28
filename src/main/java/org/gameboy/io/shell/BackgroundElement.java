package org.gameboy.io.shell;

public final class BackgroundElement extends ShellElement {
    private final float r, g, b;
    private int uColorLoc;

    public BackgroundElement(float x, float y, float width, float height,
                             float r, float g, float b) {
        super(x, y, width, height);
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    protected String getFragmentShaderPath() {
        return "shaders/shell/solid_fill_fragment.glsl";
    }

    @Override
    protected void initUniforms() {
        uColorLoc = shader.getUniformLocation("uColor");
    }

    @Override
    protected void setElementUniforms(float aspect) {
        shader.setUniform3f(uColorLoc, r, g, b);
    }
}
