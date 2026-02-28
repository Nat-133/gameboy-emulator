#version 410 core
layout (location = 0) in vec2 aPos;

uniform vec4 uElementBounds; // x, y, w, h in normalized [0,1] top-left space
uniform float uViewScale;    // VIEW_SCALE cropping factor

out vec2 vLocalUV;
out vec2 vGlobalUV;

void main() {
    // aPos is [0,1]x[0,1] unit quad
    vLocalUV = aPos;

    // Map to global normalized position
    float normX = uElementBounds.x + aPos.x * uElementBounds.z;
    float normY = uElementBounds.y + aPos.y * uElementBounds.w;
    vGlobalUV = vec2(normX, normY);

    // Convert to NDC [-1,1]
    float ndcX = normX * 2.0 - 1.0;
    float ndcY = 1.0 - (normY / uViewScale) * 2.0;
    gl_Position = vec4(ndcX, ndcY, 0.0, 1.0);
}
