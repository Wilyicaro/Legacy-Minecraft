#version 150

uniform sampler2D InSampler;

layout(std140) uniform GammaInfo {
    float gamma;
} gammaInfo;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 diffuseColor = texture(InSampler, texCoord);
   
    vec3 washedOutColor = pow(diffuseColor.rgb, vec3(1.0 / gammaInfo.gamma));
    
    fragColor = vec4(washedOutColor, 1.0);
}
