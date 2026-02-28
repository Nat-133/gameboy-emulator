#version 410 core

in vec2 vLocalUV;
in vec2 vGlobalUV;
out vec4 FragColor;

uniform vec3 uColor;
uniform vec3 uEdgeColor;
uniform vec2 uCenter;        // center in global normalized layout space
uniform vec2 uHalfSize;      // half-size in global normalized layout space
uniform float uCornerRadius; // in global normalized layout space
uniform float uGradientStrength; // 0 = no gradient, >0 = gradient amount
uniform float uEdgeDarken;       // edge darkening amount

float sdRoundedRect(vec2 p, vec2 center, vec2 halfSize, float r) {
    vec2 d = abs(p - center) - halfSize + r;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

void main() {
    float d = sdRoundedRect(vGlobalUV, uCenter, uHalfSize, uCornerRadius);

    if (d > 0.003) {
        discard;
    }

    // Base color with optional vertical gradient (using global Y)
    float grad = smoothstep(0.0, 1.0, vGlobalUV.y);
    vec3 color = mix(uColor, uEdgeColor, grad * uGradientStrength);

    // Edge darkening
    float edgeFactor = smoothstep(-0.01, 0.0, d);
    color = mix(color, uEdgeColor, edgeFactor * uEdgeDarken);

    // Anti-alias
    float alpha = smoothstep(0.003, -0.003, d);

    FragColor = vec4(color, alpha);
}
