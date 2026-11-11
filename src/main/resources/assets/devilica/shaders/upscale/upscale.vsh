// Shared vertex shader for the render scale upscaling passes, after Celeritas's.
//
// Not a complete shader on its own: FsrUpscaler prepends the #version directive. Iris's FullScreenQuadRenderer draws
// its quad from (0, 0) to (1, 1) through the fixed function vertex array, so the position is gl_Vertex; both passes
// address themselves by gl_FragCoord and need nothing else.

void main() {
    gl_Position = vec4(gl_Vertex.xy * 2.0 - 1.0, 0.0, 1.0);
}
