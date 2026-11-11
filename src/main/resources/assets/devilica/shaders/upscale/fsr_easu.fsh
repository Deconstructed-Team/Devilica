// Fragment wrapper around AMD FidelityFX FSR 1.0 EASU (edge-adaptive spatial upsampling), after Celeritas's.
//
// Not a complete shader on its own: FsrUpscaler prepends the #version directive, the A_GPU / A_GLSL / FSR_EASU_F
// defines, and upscale/ffx_a.h and upscale/ffx_fsr1.h.
//
// The reference assumes a top-left origin; gl_FragCoord and the render target both use a bottom-left one, so the whole
// computation runs vertically mirrored, and GL's textureGather order mirrors D3D's the same way, so it stays
// consistent.

uniform sampler2D inputImage;

// FsrEasuCon()'s float constants as their bits, set as signed ints and read back as the uint vectors FsrEasuF takes
uniform ivec4 easuCon0;
uniform ivec4 easuCon1;
uniform ivec4 easuCon2;
uniform ivec4 easuCon3;

out vec4 iris_FragColor;

AF4 FsrEasuRF(AF2 p) { return textureGather(inputImage, p, 0); }
AF4 FsrEasuGF(AF2 p) { return textureGather(inputImage, p, 1); }
AF4 FsrEasuBF(AF2 p) { return textureGather(inputImage, p, 2); }

void main() {
    AF3 color;

    FsrEasuF(color, AU2(gl_FragCoord.xy), AU4(easuCon0), AU4(easuCon1), AU4(easuCon2), AU4(easuCon3));

    iris_FragColor = vec4(color, 1.0);
}
