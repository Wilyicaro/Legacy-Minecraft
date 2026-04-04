#version 330

#moj_import <minecraft:fog.glsl>

in float vertexDistance;
in vec4 vertexColor;
in vec4 outerBandColor;

out vec4 fragColor;

const float CLOUD_EDGE_FADE_DISTANCE = 8.0f * 16.0f;
const float CLOUD_ALPHA_FADE_STRENGTH = 0.4f;

void main() {
    if (FogCloudsEnd <= 0.0) {
        fragColor = vertexColor;
        return;
    }

    vec4 color = vertexColor;
    float cloudFogEnd = max(FogEnvironmentalStart + 0.001f, FogCloudsEnd);
    float fadeStart = max(0.0f, cloudFogEnd - CLOUD_EDGE_FADE_DISTANCE);
    float distanceFade = linear_fog_value(vertexDistance, fadeStart, cloudFogEnd);
    color.rgb = mix(color.rgb, outerBandColor.rgb, distanceFade);
    color.a = mix(color.a, outerBandColor.a, distanceFade);
    color.a *= 1.0f - distanceFade * CLOUD_ALPHA_FADE_STRENGTH;
    fragColor = color;
}
