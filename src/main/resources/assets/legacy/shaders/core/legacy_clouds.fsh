#version 330

#moj_import <minecraft:fog.glsl>

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (FogCloudsEnd <= 0.0) {
        fragColor = vertexColor;
        return;
    }

    float fog = linear_fog_value(vertexDistance, 0.0, FogCloudsEnd);
    vec3 color = mix(vertexColor.rgb, FogColor.rgb, fog * FogColor.a);
    fragColor = vec4(color, vertexColor.a);
}
