#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vec4 lightColor = sample_lightmap(Sampler2, UV2);
    vertexColor = Color * lightColor;
    if (Color.a > 0.64 && Color.a < 0.66) {
        ivec3 rgb5 = ivec3(round(Color.rgb * 255.0)) >> 3;
        int packedColor = (rgb5.r << 10) | (rgb5.g << 5) | rgb5.b;
        vertexColor.rgb = vec3(float(packedColor) / 32768.0, float(packedColor & 1023) / 1024.0, float(packedColor & 31) / 32.0) * lightColor.rgb;
        vertexColor.a = Color.a + (1.0 - Color.a) * clamp(sphericalVertexDistance / max(FogEnvironmentalStart, 1.0), 0.0, 1.0);
    }
    texCoord0 = UV0;
}
