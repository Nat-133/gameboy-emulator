#version 410 core

in vec2 vLocalUV;
in vec2 vGlobalUV;
out vec4 FragColor;

uniform vec2 uCenter;       // button center in layout space
uniform vec2 uHalfSize;     // half-width, half-height in layout space
uniform float uAngle;       // rotation angle
uniform vec3 uColor;
uniform vec3 uPressedColor;
uniform float uPressState;  // 0..1 animation
uniform float uPressShift;  // Y offset when pressed

vec2 rotate2d(vec2 p, vec2 center, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    vec2 d = p - center;
    return center + vec2(d.x * c - d.y * s, d.x * s + d.y * c);
}

float sdRoundedRect(vec2 p, vec2 center, vec2 halfSize, float r) {
    vec2 d = abs(p - center) - halfSize + r;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

float sdPill(vec2 p, vec2 center, vec2 halfSize) {
    float r = min(halfSize.x, halfSize.y);
    return sdRoundedRect(p, center, halfSize, r);
}

void main() {
    vec2 pos = uCenter + vec2(0.0, uPressShift * uPressState);
    vec2 rotUV = rotate2d(vGlobalUV, pos, uAngle);
    float d = sdPill(rotUV, pos, uHalfSize);

    if (d >= 0.0) {
        discard;
    }

    vec3 color = mix(uColor, uPressedColor, uPressState);
    float alpha = smoothstep(0.0, -0.003, d);
    FragColor = vec4(color, alpha);
}
