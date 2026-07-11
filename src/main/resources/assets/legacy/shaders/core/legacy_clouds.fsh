#version 330

#moj_import <minecraft:fog.glsl>

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

const float CLOUD_FOG_BAND = 128.0f;

void main() {
    if (FogCloudsEnd <= 0.0) {
        fragColor = vertexColor;
        return;
    }

    float fogStart = max(0.0f, FogCloudsEnd - CLOUD_FOG_BAND);
    float fog = linear_fog_value(vertexDistance, fogStart, FogCloudsEnd);
    vec3 color = mix(vertexColor.rgb, FogColor.rgb, fog * FogColor.a);
    fragColor = vec4(color, vertexColor.a * (1.0f - fog));
}
