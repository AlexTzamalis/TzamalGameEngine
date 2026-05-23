#version 330 core

in vec4 vColor;
in vec2 vTexCoords;
in float vTexId;

out vec4 fragColor;

uniform sampler2D uTextures[16];

void main() {
    vec4 texColor;
    if (vTexId < 0.0) {
        // No texture bound - use solid vertex color
        texColor = vec4(1.0);
    } else {
        // Sample from the correct texture slot.
        // GLSL 330 requires a constant index for sampler arrays, so we
        // use an explicit if-else chain. The driver will typically
        // optimise this into a single branch.
        int id = int(vTexId);
        if      (id == 0)  texColor = texture(uTextures[0],  vTexCoords);
        else if (id == 1)  texColor = texture(uTextures[1],  vTexCoords);
        else if (id == 2)  texColor = texture(uTextures[2],  vTexCoords);
        else if (id == 3)  texColor = texture(uTextures[3],  vTexCoords);
        else if (id == 4)  texColor = texture(uTextures[4],  vTexCoords);
        else if (id == 5)  texColor = texture(uTextures[5],  vTexCoords);
        else if (id == 6)  texColor = texture(uTextures[6],  vTexCoords);
        else if (id == 7)  texColor = texture(uTextures[7],  vTexCoords);
        else if (id == 8)  texColor = texture(uTextures[8],  vTexCoords);
        else if (id == 9)  texColor = texture(uTextures[9],  vTexCoords);
        else if (id == 10) texColor = texture(uTextures[10], vTexCoords);
        else if (id == 11) texColor = texture(uTextures[11], vTexCoords);
        else if (id == 12) texColor = texture(uTextures[12], vTexCoords);
        else if (id == 13) texColor = texture(uTextures[13], vTexCoords);
        else if (id == 14) texColor = texture(uTextures[14], vTexCoords);
        else               texColor = texture(uTextures[15], vTexCoords);
    }

    fragColor = texColor * vColor;
}
