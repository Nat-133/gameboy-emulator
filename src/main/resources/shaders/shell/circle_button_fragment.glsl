#version 410 core

in vec2 vLocalUV;
in vec2 vGlobalUV;
out vec4 FragColor;

uniform float uAspect;
uniform vec2 uCenter;       // button center in layout space
uniform float uRadius;      // in layout space
uniform vec3 uColor;
uniform vec3 uPressedColor;
uniform float uPressState;  // 0..1 animation
uniform float uPressShift;  // Y offset when pressed

void main() {
    // Compute pressed center position
    vec2 pos = uCenter + vec2(0.0, uPressShift * uPressState);

    // Aspect-corrected SDF circle
    vec2 acUV = vec2(vGlobalUV.x * uAspect, vGlobalUV.y);
    vec2 acPos = vec2(pos.x * uAspect, pos.y);
    float acR = uRadius * uAspect;

    float d = length(acUV - acPos) - acR;

    if (d >= 0.0) {
        discard;
    }

    vec3 color = mix(uColor, uPressedColor, uPressState);
    // Slight highlight on top
    float highlight = smoothstep(0.5, -0.5, (vGlobalUV.y - pos.y) / uRadius);
    color = mix(color, color * 1.3, highlight * 0.2);

    float alpha = smoothstep(0.0, -0.003 * uAspect, d);
    FragColor = vec4(color, alpha);
}
