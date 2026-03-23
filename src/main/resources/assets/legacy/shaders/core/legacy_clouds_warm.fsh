#version 330

#moj_import <minecraft:fog.glsl>

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

const float CLOUD_EDGE_FADE_DISTANCE = 4.0f * 16.0f;
const float CLOUD_ALPHA_FADE_STRENGTH = 0.65f;
const float CLOUD_WARM_ALPHA_SCALE = 0.75f;
const float CLOUD_WARM_OPACITY_SCALE = 0.70f;

void main() {
    if (FogCloudsEnd <= 0.0) {
        fragColor = vertexColor;
        return;
    }

    vec4 color = vertexColor;
    float cloudFogEnd = max(FogEnvironmentalStart + 0.001f, FogCloudsEnd);
    float fadeStart = max(0.0f, cloudFogEnd - CLOUD_EDGE_FADE_DISTANCE);
    float distanceFade = linear_fog_value(vertexDistance, fadeStart, cloudFogEnd);
    float warmTint = smoothstep(0.02f, 0.14f, FogColor.r - FogColor.b);
    color.a *= mix(1.0f, CLOUD_WARM_OPACITY_SCALE, warmTint);
    float alphaFadeStrength = mix(CLOUD_ALPHA_FADE_STRENGTH, CLOUD_ALPHA_FADE_STRENGTH * CLOUD_WARM_ALPHA_SCALE, warmTint);
    color.a *= 1.0f - distanceFade * alphaFadeStrength;
    fragColor = color;
}
