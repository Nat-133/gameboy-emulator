package org.gameboy.io.shell;

import static org.lwjgl.opengl.GL41.*;

public final class GameScreenElement extends ShellElement {
    private int screenTextureId;
    private int uScreenTextureLoc;

    public GameScreenElement(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    @Override
    protected String getFragmentShaderPath() {
        return "shaders/shell/game_screen_fragment.glsl";
    }

    @Override
    protected void initUniforms() {
        uScreenTextureLoc = shader.getUniformLocation("screenTexture");
    }

    public void setScreenTextureId(int textureId) {
        this.screenTextureId = textureId;
    }

    @Override
    protected void setElementUniforms(float aspect) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, screenTextureId);
        shader.setUniform1i(uScreenTextureLoc, 0);
    }
}
