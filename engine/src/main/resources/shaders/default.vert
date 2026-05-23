#version 330 core

layout (location = 0) in vec2 aPos;
layout (location = 1) in vec4 aColor;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in float aTexId;

out vec4 vColor;
out vec2 vTexCoords;
out float vTexId;

uniform mat4 uProjection;

void main() {
    vColor = aColor;
    vTexCoords = aTexCoords;
    vTexId = aTexId;
    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
}
