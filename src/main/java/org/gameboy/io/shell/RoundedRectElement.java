package org.gameboy.io.shell;

public final class RoundedRectElement extends ShellElement {
    private final float r, g, b;
    private final float edgeR, edgeG, edgeB;
    private final float cornerRadius;
    private final float gradientStrength;
    private final float edgeDarken;

    private int uColorLoc;
    private int uEdgeColorLoc;
    private int uCenterLoc;
    private int uHalfSizeLoc;
    private int uCornerRadiusLoc;
    private int uGradientStrengthLoc;
    private int uEdgeDarkenLoc;

    public RoundedRectElement(float x, float y, float width, float height,
                              float r, float g, float b,
                              float edgeR, float edgeG, float edgeB,
                              float cornerRadius,
                              float gradientStrength, float edgeDarken) {
        super(x, y, width, height);
        this.r = r;
        this.g = g;
        this.b = b;
        this.edgeR = edgeR;
        this.edgeG = edgeG;
        this.edgeB = edgeB;
        this.cornerRadius = cornerRadius;
        this.gradientStrength = gradientStrength;
        this.edgeDarken = edgeDarken;
    }

    @Override
    protected String getFragmentShaderPath() {
        return "shaders/shell/rounded_rect_fragment.glsl";
    }

    @Override
    protected void initUniforms() {
        uColorLoc = shader.getUniformLocation("uColor");
        uEdgeColorLoc = shader.getUniformLocation("uEdgeColor");
        uCenterLoc = shader.getUniformLocation("uCenter");
        uHalfSizeLoc = shader.getUniformLocation("uHalfSize");
        uCornerRadiusLoc = shader.getUniformLocation("uCornerRadius");
        uGradientStrengthLoc = shader.getUniformLocation("uGradientStrength");
        uEdgeDarkenLoc = shader.getUniformLocation("uEdgeDarken");
    }

    @Override
    protected void setElementUniforms(float aspect) {
        shader.setUniform3f(uColorLoc, r, g, b);
        shader.setUniform3f(uEdgeColorLoc, edgeR, edgeG, edgeB);
        shader.setUniform2f(uCenterLoc, x + width * 0.5f, y + height * 0.5f);
        shader.setUniform2f(uHalfSizeLoc, width * 0.5f, height * 0.5f);
        shader.setUniform1f(uCornerRadiusLoc, cornerRadius);
        shader.setUniform1f(uGradientStrengthLoc, gradientStrength);
        shader.setUniform1f(uEdgeDarkenLoc, edgeDarken);
    }
}
