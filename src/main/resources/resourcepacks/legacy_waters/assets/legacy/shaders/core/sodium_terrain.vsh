#version 330 core

#import <sodium:include/fog.glsl>
#import <sodium:include/globals.glsl>
#import <sodium:include/chunk_vertex.glsl>

out vec4 v_Color;
out vec2 v_TexCoord;

flat out uint v_Material;

#ifdef USE_FOG
out vec2 v_FragDistance;
out float fadeFactor;
#endif
uniform sampler2D u_LightTex;

uniform isamplerBuffer u_SectionTimeInfo;

uniform vec3 u_RegionOffset;
uniform int u_CurrentTime;
uniform uint u_RegionID;

uvec3 _get_relative_chunk_coord(uint pos) {
    return uvec3(pos) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
}

vec3 _get_draw_translation(uint pos) {
    return _get_relative_chunk_coord(pos) * vec3(16.0);
}

void main() {
    _vert_init();

    vec3 translation = u_RegionOffset + _get_draw_translation(_draw_id);
    vec3 position = _vert_position + translation;

#ifdef USE_FOG
    v_FragDistance = getFragDistance(position);

    int chunkFade = texelFetch(u_SectionTimeInfo, int((u_RegionID * 256u) + uint(_draw_id))).r;
    float fade = clamp(float(u_CurrentTime - chunkFade) * u_FadePeriodInv, 0.0, 1.0);
    fadeFactor = (chunkFade < 0) ? 1.0 : fade;
#endif

    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);

    vec4 lightColor = texture(u_LightTex, _vert_tex_light_coord);
    v_Color = _vert_color * lightColor;
    if (_vert_color.a > 0.64 && _vert_color.a < 0.66) {
        ivec3 rgb5 = ivec3(round(_vert_color.rgb * 255.0)) >> 3;
        int packedColor = (rgb5.r << 10) | (rgb5.g << 5) | rgb5.b;
        v_Color.rgb = vec3(float(packedColor) / 32768.0, float(packedColor & 1023) / 1024.0, float(packedColor & 31) / 32.0) * lightColor.rgb;
        v_Color.a = _vert_color.a + (1.0 - _vert_color.a) * clamp(length(position) / max(u_EnvironmentFog.x, 1.0), 0.0, 1.0);
    }
    v_TexCoord = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;

    v_Material = _material_params;
}
