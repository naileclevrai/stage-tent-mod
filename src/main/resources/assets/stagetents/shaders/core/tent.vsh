#version 150

// Tent canvas held in GPU memory: the mesh is uploaded once and the wind ripple is applied here.
// UV1 carries the flex of each vertex instead of the overlay: x = weight (0..1000), y = octahedral direction (7 + 7 bits).

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

// Camera position in tent space, and the wind: amplitude (blocks), wave phase.
uniform vec3 TentCamera;
uniform vec2 TentWind;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec2 texCoord0;

vec3 octDecode(int e) {
    vec2 f = vec2(float(e / 128), float(e - (e / 128) * 128)) / 127.0 * 2.0 - 1.0;
    vec3 n = vec3(f.x, 1.0 - abs(f.x) - abs(f.y), f.y);
    float t = max(-n.y, 0.0);
    n.x += n.x >= 0.0 ? -t : t;
    n.z += n.z >= 0.0 ? -t : t;
    return normalize(n);
}

void main() {
    vec3 pos = Position;
    float w = float(UV1.x) / 1000.0;
    if (w > 0.0 && TentWind.x > 0.0) {
        float phase = TentWind.y - (Position.x * 0.35 + Position.z * 0.22);
        float d = TentWind.x * w * (sin(phase) + 0.45 * sin(phase * 2.3 + Position.z * 0.5));
        pos += octDecode(UV1.y) * d;
    }
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vec3 rel = pos - TentCamera;
    vertexDistance = FogShape == 0 ? length(rel) : max(length(rel.xz), abs(rel.y));

    // Same diffuse as vanilla entities; the lights are in view space.
    vec3 n = normalize(mat3(ModelViewMat) * Normal);
    float l0 = max(0.0, dot(normalize(Light0_Direction), n));
    float l1 = max(0.0, dot(normalize(Light1_Direction), n));
    vertexColor = vec4(Color.rgb * min(1.0, (l0 + l1) * 0.6 + 0.4), Color.a);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
}
