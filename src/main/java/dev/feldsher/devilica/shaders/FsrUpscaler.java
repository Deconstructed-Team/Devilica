/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.shaders;

import dev.feldsher.devilica.Devilica;
import net.coderbot.iris.postprocess.FullScreenQuadRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * AMD FidelityFX Super Resolution 1.0 for the render scale: EASU stretches the lower resolution world to the window's
 * size into an intermediate texture, RCAS sharpens that into the main framebuffer. After Celeritas's
 * {@code FsrUpscaler} (LGPL-3.0); the filter itself is AMD's reference headers, unchanged (MIT, see
 * {@code licenses/fidelityfx-fsr.txt}). FSR expects what the shader pack's final pass gives: anti-aliased, display
 * referred, in [0, 1].
 */
final class FsrUpscaler {

    /** {@code ffx_a.h} uses packHalf2x16, core from GLSL 4.20; compatibility for the quad's gl_Vertex */
    private static final String VERSION = "#version 420 compatibility\n";
    private static final String DEFINES = "#define A_GPU 1\n#define A_GLSL 1\n";
    private static final String SHADERS = "/assets/devilica/shaders/upscale/";

    private final int easu;
    private final int rcas;
    private final int[] easuConstants = new int[4];
    private final int easuInput;
    private final int rcasSharpness;
    private final int rcasMaxCoord;
    private final int rcasInput;
    private final int texture;
    private final int framebuffer;
    private int width;
    private int height;

    private FsrUpscaler(int width, int height) {
        this.easu = link("fsrEasu", "FSR_EASU_F", "fsr_easu.fsh");
        this.rcas = link("fsrRcas", "FSR_RCAS_F", "fsr_rcas.fsh");
        for (int i = 0; i < 4; i++) {
            this.easuConstants[i] = GL20.glGetUniformLocation(this.easu, "easuCon" + i);
        }
        this.easuInput = GL20.glGetUniformLocation(this.easu, "inputImage");
        this.rcasSharpness = GL20.glGetUniformLocation(this.rcas, "rcasSharpness");
        this.rcasMaxCoord = GL20.glGetUniformLocation(this.rcas, "rcasMaxCoord");
        this.rcasInput = GL20.glGetUniformLocation(this.rcas, "inputImage");

        this.texture = GlStateManager.generateTexture();
        this.width = width;
        this.height = height;
        allocate();
        this.framebuffer = OpenGlHelper.glGenFramebuffers();
        OpenGlHelper.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.texture, 0);
        final int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            destroy();
            throw new IllegalStateException("FSR intermediate framebuffer is incomplete: " + status);
        }
    }

    /** An upscaler, or null when the driver cannot build it (bilinear stretching then) */
    static FsrUpscaler createOrNull(int width, int height) {
        try {
            return new FsrUpscaler(width, height);
        } catch (RuntimeException e) {
            Devilica.LOGGER.error("Could not set up FSR upscaling, stretching the world bilinearly instead", e);
            return null;
        }
    }

    private static int link(String name, String stage, String wrapper) {
        final int vertex = compile(GL20.GL_VERTEX_SHADER, name, VERSION + read("upscale.vsh"));
        final int fragment = compile(GL20.GL_FRAGMENT_SHADER, name, VERSION + DEFINES + "#define " + stage + " 1\n"
            + read("ffx_a.h") + read("ffx_fsr1.h") + read(wrapper));
        final int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glLinkProgram(program);
        GL20.glDetachShader(program, vertex);
        GL20.glDetachShader(program, fragment);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            final String log = GL20.glGetProgramInfoLog(program, 32768);
            GL20.glDeleteProgram(program);
            throw new IllegalStateException(name + " did not link: " + log);
        }
        return program;
    }

    private static int compile(int type, String name, String source) {
        final int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            final String log = GL20.glGetShaderInfoLog(shader, 32768);
            GL20.glDeleteShader(shader);
            // Line numbers count the prepended ffx_a.h and ffx_fsr1.h
            throw new IllegalStateException(name + " did not compile: " + log);
        }
        return shader;
    }

    private static String read(String file) {
        try (InputStream stream = FsrUpscaler.class.getResourceAsStream(SHADERS + file)) {
            return new String(IOUtils.toByteArray(Objects.requireNonNull(stream, file)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file, e);
        }
    }

    private void allocate() {
        GlStateManager.bindTexture(this.texture);
        // RGBA8 like the main framebuffer; RCAS reads it texel by texel
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, this.width, this.height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager.bindTexture(0);
    }

    /**
     * Upscales the source into the destination at the destination's size, leaving the destination bound.
     *
     * @param sharpnessStops RCAS's sharpening held back in stops: 0 is the sharpest, each whole stop halves it
     */
    void process(Framebuffer source, Framebuffer destination, float sharpnessStops) {
        if (destination.framebufferWidth != this.width || destination.framebufferHeight != this.height) {
            this.width = destination.framebufferWidth;
            this.height = destination.framebufferHeight;
            allocate();
        }
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        FullScreenQuadRenderer.INSTANCE.begin();

        // EASU: the world up to the window's size
        OpenGlHelper.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebuffer);
        GlStateManager.viewport(0, 0, this.width, this.height);
        GL20.glUseProgram(this.easu);
        easuConstants(source.framebufferWidth, source.framebufferHeight);
        GL20.glUniform1i(this.easuInput, 0);
        GlStateManager.bindTexture(source.framebufferTexture);
        FullScreenQuadRenderer.INSTANCE.renderQuad();

        // RCAS: sharpened into the destination
        destination.bindFramebuffer(true);
        GL20.glUseProgram(this.rcas);
        GL20.glUniform1f(this.rcasSharpness, sharpnessStops);
        GL20.glUniform2i(this.rcasMaxCoord, this.width - 1, this.height - 1);
        GL20.glUniform1i(this.rcasInput, 0);
        GlStateManager.bindTexture(this.texture);
        FullScreenQuadRenderer.INSTANCE.renderQuad();

        FullScreenQuadRenderer.end();
        GL20.glUseProgram(0);
        GlStateManager.bindTexture(0);
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
    }

    /** {@code FsrEasuCon} from ffx_fsr1.h, as float bits */
    private void easuConstants(int inputWidth, int inputHeight) {
        final float inW = inputWidth;
        final float inH = inputHeight;
        final float outW = this.width;
        final float outH = this.height;
        uniform(0, inW / outW, inH / outH, 0.5F * inW / outW - 0.5F, 0.5F * inH / outH - 0.5F);
        uniform(1, 1.0F / inW, 1.0F / inH, 1.0F / inW, -1.0F / inH);
        uniform(2, -1.0F / inW, 2.0F / inH, 1.0F / inW, 2.0F / inH);
        uniform(3, 0.0F, 4.0F / inH, 0.0F, 0.0F);
    }

    private void uniform(int index, float x, float y, float z, float w) {
        GL20.glUniform4i(this.easuConstants[index], Float.floatToRawIntBits(x), Float.floatToRawIntBits(y),
            Float.floatToRawIntBits(z), Float.floatToRawIntBits(w));
    }

    void destroy() {
        GL20.glDeleteProgram(this.easu);
        GL20.glDeleteProgram(this.rcas);
        OpenGlHelper.glDeleteFramebuffers(this.framebuffer);
        GlStateManager.deleteTexture(this.texture);
    }
}
