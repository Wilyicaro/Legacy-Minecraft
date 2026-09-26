#version 330

#moj_import <minecraft:fog.glsl>

in float vertexDistance;
in vec4 vertexColor;
in vec4 outerBandColor;

out vec4 fragColor;

const float CLOUD_WARM_TINT_STRENGTH = 0.78f;
const float CLOUD_WARM_INNER_TINT_STRENGTH = 0.22f;
const float CLOUD_FOG_BAND = 128.0f;
const float CLOUD_ALPHA_FADE_STRENGTH = 0.28f;

void main() {
    if (FogCloudsEnd <= 0.0) {
        fragColor = vertexColor;
        return;
    }

    float fogStart = max(0.0f, FogCloudsEnd - CLOUD_FOG_BAND);
    float distanceFade = linear_fog_value(vertexDistance, fogStart, FogCloudsEnd);
    vec3 color = mix(vertexColor.rgb, FogColor.rgb, distanceFade * FogColor.a);
    color = mix(color, outerBandColor.rgb, distanceFade);
    float warmTint = smoothstep(0.06f, 0.22f, FogColor.r - FogColor.b);
    float innerBlend = (1.0f - distanceFade) * warmTint * CLOUD_WARM_INNER_TINT_STRENGTH;
    color = mix(color, FogColor.rgb, innerBlend);
    float sunsetBlend = distanceFade * distanceFade * warmTint * CLOUD_WARM_TINT_STRENGTH;
    color = mix(color, FogColor.rgb, sunsetBlend);
    float alpha = mix(vertexColor.a, outerBandColor.a, distanceFade) * (1.0f - distanceFade * CLOUD_ALPHA_FADE_STRENGTH);
    fragColor = vec4(color, alpha);
}
