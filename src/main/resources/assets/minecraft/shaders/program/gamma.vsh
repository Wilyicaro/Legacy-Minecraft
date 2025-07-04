#version 150

#moj_import <minecraft:projection.glsl>

in vec4 Position;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform GammaConfig {
    float gamma;
};

out vec2 texCoord;
out float aspectRatio;

void main(){
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);

    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    aspectRatio = SamplerInfo.InSize.x / SamplerInfo.InSize.y;
    texCoord = outPos.xy * 0.5 + 0.5;
}