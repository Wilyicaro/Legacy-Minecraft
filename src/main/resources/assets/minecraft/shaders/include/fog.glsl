#version 330

layout(std140) uniform Fog {
    vec4 FogColor;
    float FogEnvironmentalStart;
    float FogEnvironmentalEnd;
    float FogRenderDistanceStart;
    float FogRenderDistanceEnd;
    float FogSkyEnd;
    float FogCloudsEnd;
};

float linear_fog_value(float vertexDistance, float fogStart, float fogEnd) {
    if (fogEnd <= fogStart) {
        return 1.0;
    }
    float value = clamp((vertexDistance - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
    float curve = value * value * (3.0 - 2.0 * value);
    float dense = curve * (0.55 + value * 0.45);
    float limit = fogStart >= 16.0 && fogEnd >= 128.0 ? 0.92 : 1.0;
    return dense * limit;
}

float edge_fog_value(float vertexDistance, float fogStart, float fogEnd) {
    if (fogEnd <= fogStart) {
        return 1.0;
    }
    if (fogStart < 16.0 || fogEnd < 128.0) {
        return linear_fog_value(vertexDistance, fogStart, fogEnd);
    }
    float value = clamp((vertexDistance - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
    float edgeStart = max(0.0, 1.0 - 160.0 / (fogEnd - fogStart));
    return smoothstep(edgeStart, 1.0, value);
}

float total_fog_value(float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd) {
    return max(linear_fog_value(sphericalVertexDistance, environmentalStart, environmantalEnd), edge_fog_value(cylindricalVertexDistance, renderDistanceStart, renderDistanceEnd));
}

float fog_legacy_distance(vec4 viewPos) {
    return length(viewPos.xyz);
}

vec4 apply_fog(vec4 inColor, float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd, vec4 fogColor) {
    float fogValue = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, environmentalStart, environmantalEnd, renderDistanceStart, renderDistanceEnd);
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

float fog_spherical_distance(vec3 pos) {
    return length(pos);
}

float fog_cylindrical_distance(vec3 pos) {
    float distXZ = length(pos.xz);
    float distY = abs(pos.y);
    return max(distXZ, distY);
}
