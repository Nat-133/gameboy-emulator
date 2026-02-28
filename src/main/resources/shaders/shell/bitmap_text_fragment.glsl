#version 410 core

in vec2 vLocalUV;
in vec2 vGlobalUV;
out vec4 FragColor;

uniform vec3 uColor;
uniform float uAngle;        // rotation angle (0 for A/B labels)
uniform vec2 uTextCenter;    // center of text in layout space
uniform float uPixelWidth;   // width of one character pixel in layout space
uniform float uPixelHeight;  // height of one character pixel in layout space
uniform int uCharCount;      // number of characters (max 6)
uniform int uChars[6];       // 15-bit bitmap per character
uniform float uCharSpacing;  // spacing between characters (in pixel units)

vec2 rotate2d(vec2 p, vec2 center, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    vec2 d = p - center;
    return center + vec2(d.x * c - d.y * s, d.x * s + d.y * c);
}

float charPixel(int bitmap, vec2 p) {
    if (p.x < 0.0 || p.x >= 3.0 || p.y < 0.0 || p.y >= 5.0) return 0.0;
    int col = int(p.x);
    int row = int(p.y);
    int shift = (4 - row) * 3 + (2 - col);
    return float((bitmap >> shift) & 1);
}

void main() {
    // Rotate UV around text center for angled labels
    vec2 ruv = rotate2d(vGlobalUV, uTextCenter, uAngle);

    // Total text width in layout space
    float totalWidth = float(uCharCount) * uCharSpacing * uPixelWidth;

    // Text top-left position (centered on uTextCenter)
    vec2 textOrigin = uTextCenter - vec2(totalWidth * 0.5, 2.5 * uPixelHeight);

    float hit = 0.0;
    for (int i = 0; i < 6; i++) {
        if (i >= uCharCount) break;
        vec2 charPos = textOrigin + vec2(float(i) * uCharSpacing * uPixelWidth, 0.0);
        vec2 p = (ruv - charPos) / vec2(uPixelWidth, uPixelHeight);
        hit = max(hit, charPixel(uChars[i], p));
    }

    if (hit < 0.5) {
        discard;
    }

    FragColor = vec4(uColor, 0.85);
}
