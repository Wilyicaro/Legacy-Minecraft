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
    if (vertexDistance <= fogStart) {
        return 0.0;
    }
    if (vertexDistance >= fogEnd) {
        return 1.0;
    }
    return (vertexDistance - fogStart) / (fogEnd - fogStart);
}

float legacy_fog_value(float vertexDistance, float fogStart, float fogEnd) {
    if (fogEnd <= fogStart) {
        return 1.0;
    }
    float value = clamp((vertexDistance - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
    float curve = value * value * (3.0 - 2.0 * value);
    float dense = curve * (0.55 + value * 0.45);
    return dense * 0.92;
}

float legacy_edge_fog_value(float vertexDistance, float fogStart, float fogEnd) {
    if (fogEnd <= fogStart) {
        return 1.0;
    }
    float value = clamp((vertexDistance - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
    float edgeStart = max(0.0, 1.0 - 160.0 / (fogEnd - fogStart));
    return smoothstep(edgeStart, 1.0, value);
}

bool legacy_fog_enabled(float environmentalStart, float environmentalEnd, float renderDistanceStart, float renderDistanceEnd) {
    return environmentalStart >= 16.0 && environmentalEnd >= 128.0 && abs(environmentalStart - renderDistanceStart) < 0.5 && abs(environmentalEnd - renderDistanceEnd) < 0.5;
}

float total_fog_value(float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd) {
    if (legacy_fog_enabled(environmentalStart, environmantalEnd, renderDistanceStart, renderDistanceEnd)) {
        return max(legacy_fog_value(sphericalVertexDistance, environmentalStart, environmantalEnd), legacy_edge_fog_value(cylindricalVertexDistance, renderDistanceStart, renderDistanceEnd));
    }
    return max(linear_fog_value(sphericalVertexDistance, environmentalStart, environmantalEnd), linear_fog_value(cylindricalVertexDistance, renderDistanceStart, renderDistanceEnd));
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
