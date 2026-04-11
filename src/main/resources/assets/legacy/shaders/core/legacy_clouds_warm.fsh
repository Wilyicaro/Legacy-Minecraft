#version 330

#moj_import <minecraft:fog.glsl>

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

const float CLOUD_WARM_TINT_STRENGTH = 0.78f;
const float CLOUD_WARM_INNER_TINT_STRENGTH = 0.22f;

void main() {
    if (FogCloudsEnd <= 0.0) {
        fragColor = vertexColor;
        return;
    }

    float distanceFade = linear_fog_value(vertexDistance, 0.0, FogCloudsEnd);
    vec3 color = mix(vertexColor.rgb, FogColor.rgb, distanceFade * FogColor.a);
    float warmTint = smoothstep(0.06f, 0.22f, FogColor.r - FogColor.b);
    float innerBlend = (1.0f - distanceFade) * warmTint * CLOUD_WARM_INNER_TINT_STRENGTH;
    color = mix(color, FogColor.rgb, innerBlend);
    float sunsetBlend = distanceFade * distanceFade * warmTint * CLOUD_WARM_TINT_STRENGTH;
    color = mix(color, FogColor.rgb, sunsetBlend);
    fragColor = vec4(color, vertexColor.a);
}
