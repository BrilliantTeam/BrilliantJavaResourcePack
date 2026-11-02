#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

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
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vec4 color = Color;
    if (ProjMat[3][3] == 1.0 && all(lessThan(abs(Color.rgb - 64.0 / 255.0), vec3(0.001)))) {
        color.rgb = vec3(224.0 / 255.0);
    }
    vertexColor = color * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
}
