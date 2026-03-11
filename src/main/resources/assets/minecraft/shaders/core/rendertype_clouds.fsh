#version 330

#moj_import <minecraft:fog.glsl>

layout(std140) uniform CloudInfo {
    vec4 CloudColor;
    vec3 CloudOffset;
    vec3 CellSize;
    vec3 SkyColor;
};

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

const float CLOUD_FOG_BAND_RATIO = 0.5;
const float MIN_CLOUD_FOG_BAND = 96.0;
const float MAX_CLOUD_FOG_BAND = 192.0;
const float CLOUD_FOG_START_OFFSET = 32.0;
const float CLOUD_FOG_COLOR_MIX = 0.56;
const float CLOUD_FOG_DARKEN = 0.97;
const float CLOUD_FACE_FLATTEN = 0.58;
const float CLOUD_FACE_CONTRAST_FLATTEN = 0.55;
const float CLOUD_SKY_TINT = 0.4;
const float CLOUD_DISTANT_VISIBILITY = 0.18;
const vec3 LUMA = vec3(0.299, 0.587, 0.114);

void main() {
    if (FogCloudsEnd <= 0.0) {
        fragColor = vertexColor;
        return;
    }

    float fogBand = min(MAX_CLOUD_FOG_BAND, max(MIN_CLOUD_FOG_BAND, FogEnvironmentalEnd * CLOUD_FOG_BAND_RATIO));
    float fogStart = max(0.0, FogEnvironmentalEnd + CLOUD_FOG_START_OFFSET);
    float fogEnd = max(fogStart + 0.001, fogStart + fogBand);
    float fogValue = linear_fog_value(vertexDistance, fogStart, fogEnd);
    fogValue = smoothstep(0.0, 1.0, smoothstep(0.0, 1.0, fogValue * 0.82));

    vec4 color = vertexColor;
    float fogBlend = fogValue * FogColor.a;
    float daylightBlend = smoothstep(0.45, 0.75, dot(color.rgb, LUMA));
    float daytimeFogBlend = fogBlend * daylightBlend;
    float distantVisibilityBlend = smoothstep(0.35, 1.0, fogBlend) * CLOUD_DISTANT_VISIBILITY;
    vec3 unifiedCloudColor = mix(vec3(0.88, 0.9, 0.96), FogColor.rgb, 0.12);
    vec3 blueSkyColor = clamp(mix(vec3(0.82, 0.9, 1.0), SkyColor * vec3(0.92, 1.0, 1.16), 0.7), 0.0, 1.0);
    vec3 distantCloudColor = mix(unifiedCloudColor, blueSkyColor, 0.45);

    color.rgb = mix(color.rgb, unifiedCloudColor, daytimeFogBlend * CLOUD_FACE_CONTRAST_FLATTEN);
    color.rgb = mix(color.rgb, unifiedCloudColor, daytimeFogBlend * CLOUD_FACE_FLATTEN);
    color.rgb = mix(color.rgb, FogColor.rgb, fogBlend * CLOUD_FOG_COLOR_MIX);
    color.rgb *= mix(1.0, CLOUD_FOG_DARKEN, fogBlend);
    color.rgb = mix(color.rgb, blueSkyColor, daytimeFogBlend * CLOUD_SKY_TINT);
    color.rgb = mix(color.rgb, distantCloudColor, distantVisibilityBlend);

    fragColor = color;
}
