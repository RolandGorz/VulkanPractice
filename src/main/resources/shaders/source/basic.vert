#version 460
#extension GL_EXT_scalar_block_layout : require

layout(binding = 0, std430) uniform UniformBufferObject {
    mat2 translation;
} ubo;

layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec3 inColor;

layout(location = 0) out vec3 fragColor;

void main() {
    gl_Position = vec4(ubo.translation * inPosition, 0.0, 1.0);
    fragColor = inColor;
}