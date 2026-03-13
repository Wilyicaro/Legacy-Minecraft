#version 330

#moj_import <minecraft:fog.glsl>

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

const float CLOUD_EDGE_FADE_DISTANCE = 4.0f * 16.0f;
const float CLOUD_FOG_STRENGTH = 1.15f;
const float CLOUD_ALPHA_FADE_STRENGTH = 0.65f;

void main() {
    if (FogCloudsEnd <= 0.0) {
        fragColor = vertexColor;
        return;
    }

    vec4 color = vertexColor;
    float cloudFogEnd = max(FogEnvironmentalStart + 0.001f, FogCloudsEnd);
    float fogTint = linear_fog_value(vertexDistance, FogEnvironmentalStart, cloudFogEnd);
    float fadeStart = max(0.0f, cloudFogEnd - CLOUD_EDGE_FADE_DISTANCE);
    float edgeFogTint = linear_fog_value(vertexDistance, fadeStart, cloudFogEnd);
    float fogBlend = clamp(max(fogTint, edgeFogTint) * CLOUD_FOG_STRENGTH, 0.0f, 1.0f);
    color.rgb = mix(color.rgb, FogColor.rgb, fogBlend);

    float distanceFade = linear_fog_value(vertexDistance, fadeStart, cloudFogEnd);
    color.a *= 1.0f - distanceFade * CLOUD_ALPHA_FADE_STRENGTH;
    fragColor = color;
}
