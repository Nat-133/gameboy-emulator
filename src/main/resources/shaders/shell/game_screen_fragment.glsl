#version 410 core

in vec2 vLocalUV;
in vec2 vGlobalUV;
out vec4 FragColor;

uniform sampler2D screenTexture;

void main() {
    FragColor = vec4(texture(screenTexture, vLocalUV).rgb, 1.0);
}
