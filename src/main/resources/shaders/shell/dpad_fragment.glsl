#version 410 core

in vec2 vLocalUV;
in vec2 vGlobalUV;
out vec4 FragColor;

uniform float uAspect;
uniform vec2 uCenter;      // d-pad center in normalized layout space
uniform float uArmWidth;   // in normalized layout space
uniform float uArmLength;  // in normalized layout space
uniform float uCornerR;    // corner radius in layout space
uniform vec3 uColor;
uniform vec3 uPressedColor;
uniform float uButtons[4]; // up, down, left, right press states

float sdRoundedRect(vec2 p, vec2 center, vec2 halfSize, float r) {
    vec2 d = abs(p - center) - halfSize + r;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

void main() {
    // Work in aspect-corrected global space (same as original shader)
    vec2 acUV = vec2(vGlobalUV.x * uAspect, vGlobalUV.y);
    vec2 acCenter = vec2(uCenter.x * uAspect, uCenter.y);
    float acW = uArmWidth * uAspect;
    float acL = uArmLength * uAspect;
    float acR = uCornerR * uAspect;

    // SDF for cross shape
    float dH = sdRoundedRect(acUV, acCenter, vec2(acL, acW), acR);
    float dV = sdRoundedRect(acUV, acCenter, vec2(acW, acL), acR);
    float dDpad = min(dH, dV);

    if (dDpad >= 0.0) {
        discard;
    }

    vec3 dpadColor = uColor;
    vec2 rel = acUV - acCenter;
    bool isCenter = abs(rel.x) < acW && abs(rel.y) < acW;

    // Press highlighting per arm (not center)
    if (!isCenter) {
        if (rel.y < 0.0 && abs(rel.x) < acW)
            dpadColor = mix(uColor, uPressedColor, uButtons[0]); // up
        else if (rel.y > 0.0 && abs(rel.x) < acW)
            dpadColor = mix(uColor, uPressedColor, uButtons[1]); // down
        else if (rel.x < 0.0 && abs(rel.y) < acW)
            dpadColor = mix(uColor, uPressedColor, uButtons[2]); // left
        else if (rel.x > 0.0 && abs(rel.y) < acW)
            dpadColor = mix(uColor, uPressedColor, uButtons[3]); // right
    }

    // Triangle arrows on each arm
    float arrowH = acW * 0.7;
    float arrowHB = acW * 0.5;
    vec3 arrowCol = dpadColor * 1.3;

    // Up arrow
    {
        float along = rel.y - (-acL * 0.55);
        float across = abs(rel.x);
        if (along >= 0.0 && along <= arrowH && across <= arrowHB * (along / arrowH))
            dpadColor = arrowCol;
    }
    // Down arrow
    {
        float along = (acL * 0.55) - rel.y;
        float across = abs(rel.x);
        if (along >= 0.0 && along <= arrowH && across <= arrowHB * (along / arrowH))
            dpadColor = arrowCol;
    }
    // Left arrow
    {
        float along = rel.x - (-acL * 0.55);
        float across = abs(rel.y);
        if (along >= 0.0 && along <= arrowH && across <= arrowHB * (along / arrowH))
            dpadColor = arrowCol;
    }
    // Right arrow
    {
        float along = (acL * 0.55) - rel.x;
        float across = abs(rel.y);
        if (along >= 0.0 && along <= arrowH && across <= arrowHB * (along / arrowH))
            dpadColor = arrowCol;
    }

    // Center hub
    if (isCenter) {
        float cDist = length(rel) / acW;
        dpadColor = mix(uColor * 0.88, uColor, smoothstep(0.0, 0.8, cDist));
        float groove = smoothstep(0.7, 0.8, cDist) * smoothstep(1.0, 0.9, cDist);
        dpadColor = mix(dpadColor, uColor * 0.7, groove * 0.5);
    }

    float alpha = smoothstep(0.0, -0.003 * uAspect, dDpad);
    FragColor = vec4(dpadColor, alpha);
}
