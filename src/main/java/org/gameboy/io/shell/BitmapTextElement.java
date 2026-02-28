package org.gameboy.io.shell;

public final class BitmapTextElement extends ShellElement {
    // 3x5 bitmap font characters (15 bits each)
    public static final int CH_A = 11245;
    public static final int CH_B = 27566;
    public static final int CH_C = 14627;
    public static final int CH_E = 31143;
    public static final int CH_L = 18727;
    public static final int CH_R = 27556;
    public static final int CH_S = 14478;
    public static final int CH_T = 29842;

    private final float textCenterX, textCenterY;
    private final float pixelWidth, pixelHeight;
    private final float angle;
    private final float charSpacing;
    private final int[] chars;
    private final float colorR, colorG, colorB;

    private final int[] paddedChars;

    private int uColorLoc;
    private int uAngleLoc;
    private int uTextCenterLoc;
    private int uPixelWidthLoc;
    private int uPixelHeightLoc;
    private int uCharCountLoc;
    private int uCharsLoc;
    private int uCharSpacingLoc;

    public BitmapTextElement(float textCenterX, float textCenterY,
                             float pixelWidth, float pixelHeight,
                             float angle, float charSpacing,
                             int[] chars,
                             float colorR, float colorG, float colorB) {
        super(computeBoundsX(textCenterX, pixelWidth, charSpacing, chars.length),
              computeBoundsY(textCenterY, pixelHeight),
              computeBoundsW(pixelWidth, charSpacing, chars.length),
              computeBoundsH(pixelHeight));
        this.textCenterX = textCenterX;
        this.textCenterY = textCenterY;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
        this.angle = angle;
        this.charSpacing = charSpacing;
        this.chars = chars;
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;
        this.paddedChars = new int[6];
        System.arraycopy(chars, 0, paddedChars, 0, Math.min(chars.length, 6));
    }

    private static float computeBoundsX(float cx, float pw, float spacing, int count) {
        float totalW = count * spacing * pw;
        return cx - totalW * 0.5f - 0.02f;
    }

    private static float computeBoundsY(float cy, float ph) {
        return cy - 2.5f * ph - 0.02f;
    }

    private static float computeBoundsW(float pw, float spacing, int count) {
        return count * spacing * pw + 0.04f;
    }

    private static float computeBoundsH(float ph) {
        return 5.0f * ph + 0.04f;
    }

    @Override
    protected String getFragmentShaderPath() {
        return "shaders/shell/bitmap_text_fragment.glsl";
    }

    @Override
    protected void initUniforms() {
        uColorLoc = shader.getUniformLocation("uColor");
        uAngleLoc = shader.getUniformLocation("uAngle");
        uTextCenterLoc = shader.getUniformLocation("uTextCenter");
        uPixelWidthLoc = shader.getUniformLocation("uPixelWidth");
        uPixelHeightLoc = shader.getUniformLocation("uPixelHeight");
        uCharCountLoc = shader.getUniformLocation("uCharCount");
        uCharsLoc = shader.getUniformLocation("uChars[0]");
        uCharSpacingLoc = shader.getUniformLocation("uCharSpacing");
    }

    @Override
    protected void setElementUniforms(float aspect) {
        shader.setUniform3f(uColorLoc, colorR, colorG, colorB);
        shader.setUniform1f(uAngleLoc, angle);
        shader.setUniform2f(uTextCenterLoc, textCenterX, textCenterY);
        shader.setUniform1f(uPixelWidthLoc, pixelWidth);
        shader.setUniform1f(uPixelHeightLoc, pixelHeight);
        shader.setUniform1i(uCharCountLoc, chars.length);
        shader.setUniform1iv(uCharsLoc, paddedChars);
        shader.setUniform1f(uCharSpacingLoc, charSpacing);
    }
}
