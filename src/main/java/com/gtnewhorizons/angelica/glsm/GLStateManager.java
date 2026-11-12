package com.gtnewhorizons.angelica.glsm;

import com.gtnewhorizons.angelica.glsm.states.AlphaState;
import com.gtnewhorizons.angelica.glsm.states.BlendState;
import com.gtnewhorizons.angelica.glsm.states.ColorMask;
import com.gtnewhorizons.angelica.glsm.states.DepthState;
import com.gtnewhorizons.angelica.glsm.states.FogState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Devilica: the GL state manager Iris talks to.
 * <p>
 * Angelica's GLStateManager owns every piece of GL state: all GL calls in the game are redirected into it. Devilica
 * stays on Cleanroom's compatibility context and keeps vanilla's {@link GlStateManager} as the one cache. The methods
 * here have Angelica's names and go through {@link GlStateManager} wherever it caches the state, so vanilla, mods and
 * Iris see the same values. The Iris side effects Angelica's class had (deferring blend, alpha and depth/colour changes
 * while a shader pack override holds, texture tracking, sampler inputs) live in the {@link GlStateManager} mixin, so
 * they also run for vanilla's and mods' own calls. Vanilla does not cache the viewport or the fog colour; the mixin
 * records them here.
 */
public final class GLStateManager {

    /** The GL capabilities of the render context, read when the class first loads on the render thread. */
    public static final GLCapabilities capabilities = GL.getCapabilities();

    private static final Thread MAIN_THREAD = Thread.currentThread();

    /** OptiFine's limit too; vanilla caches 8 units and the mixin grows its array to this. */
    public static final int MAX_TEXTURE_UNITS = 32;

    private static final BooleanView BLEND_MODE = () -> GlStateManager.blendState.blend.currentState;
    private static final BooleanView ALPHA_TEST = () -> GlStateManager.alphaState.alphaTest.currentState;
    private static final BooleanView FOG_MODE = () -> GlStateManager.fogState.fog.currentState;

    private static final FogState FOG_STATE = new FogState();
    private static final int[] VIEWPORT = new int[4];
    private static final Deque<Integer> ATTRIB_MASKS = new ArrayDeque<>();

    private GLStateManager() {
    }

    /** One cached GL capability, read-only. */
    @FunctionalInterface
    public interface BooleanView {
        boolean isEnabled();
    }

    public static Thread getMainThread() {
        return MAIN_THREAD;
    }

    public static boolean isMainThread() {
        return Thread.currentThread() == MAIN_THREAD;
    }

    public static boolean isSplashComplete() {
        return true;
    }

    // ---- textures

    public static int getActiveTextureUnit() {
        return GlStateManager.activeTextureUnit;
    }

    public static void glActiveTexture(int texture) {
        GlStateManager.setActiveTexture(texture - GL13.GL_TEXTURE0 + OpenGlHelper.defaultTexUnit);
    }

    public static int getBoundTextureForServerState() {
        return getBoundTextureForServerState(GlStateManager.activeTextureUnit);
    }

    public static int getBoundTextureForServerState(int unit) {
        GlStateManager.TextureState[] states = GlStateManager.textureState;
        return unit >= 0 && unit < states.length ? states[unit].textureName : -1;
    }

    /** A direct-state-access bind ({@code glBindTextureUnit}) does not go through vanilla; keep its cache right. */
    public static void onTextureUnitBound(int unit, int texture) {
        GlStateManager.TextureState[] states = GlStateManager.textureState;
        if (unit >= 0 && unit < states.length) {
            states[unit].textureName = texture;
        }
    }

    public static void glBindTexture(int target, int texture) {
        if (target == GL11.GL_TEXTURE_2D) {
            GlStateManager.bindTexture(texture);
        } else {
            GL11.glBindTexture(target, texture);
        }
    }

    public static void enableTexture() {
        GlStateManager.enableTexture2D();
    }

    public static void disableTexture() {
        GlStateManager.disableTexture2D();
    }

    public static void glDeleteTextures(int id) {
        GlStateManager.deleteTexture(id);
    }

    public static void glDeleteTextures(IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) {
            GlStateManager.deleteTexture(ids.get(i));
        }
    }

    public static void glTexParameteri(int target, int pname, int param) {
        GlStateManager.glTexParameteri(target, pname, param);
    }

    public static void glTexParameterf(int target, int pname, float param) {
        GlStateManager.glTexParameterf(target, pname, param);
    }

    public static void glTexParameter(int target, int pname, IntBuffer params) {
        GL11.glTexParameteriv(target, pname, params);
    }

    public static int glGetTexParameteri(int target, int pname) {
        return GL11.glGetTexParameteri(target, pname);
    }

    public static void glCopyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border) {
        GL11.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border);
    }

    // ---- blending and colour mask

    public static void enableBlend() {
        GlStateManager.enableBlend();
    }

    public static void disableBlend() {
        GlStateManager.disableBlend();
    }

    public static void tryBlendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }

    public static void defaultBlendFunc() {
        tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    public static BooleanView getBlendMode() {
        return BLEND_MODE;
    }

    public static BlendState getBlendState() {
        GlStateManager.BlendState blend = GlStateManager.blendState;
        return new BlendState(blend.srcFactor, blend.dstFactor, blend.srcFactorAlpha, blend.dstFactorAlpha);
    }

    /** After a per-draw-buffer blend toggle: vanilla's single flag stands for draw buffer 0. */
    public static void blendBufferToggled(int buffer, boolean enabled) {
        if (buffer == 0) {
            GlStateManager.blendState.blend.currentState = enabled;
        }
    }

    /**
     * Sets blending for every draw buffer, past vanilla's cache: after per-buffer toggles and functions the cache
     * speaks for buffer 0 alone and would skip the call that puts the others back.
     */
    public static void forceBlend(boolean enabled, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        final GlStateManager.BlendState blend = GlStateManager.blendState;
        if (enabled) {
            GL11.glEnable(GL11.GL_BLEND);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }
        blend.blend.currentState = enabled;
        GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        blend.srcFactor = srcRgb;
        blend.dstFactor = dstRgb;
        blend.srcFactorAlpha = srcAlpha;
        blend.dstFactorAlpha = dstAlpha;
    }

    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GlStateManager.colorMask(red, green, blue, alpha);
    }

    public static ColorMask getColorMask() {
        GlStateManager.ColorMask mask = GlStateManager.colorMaskState;
        ColorMask copy = new ColorMask();
        copy.setAll(mask.red, mask.green, mask.blue, mask.alpha);
        return copy;
    }

    // ---- depth and alpha test

    public static void enableDepthTest() {
        GlStateManager.enableDepth();
    }

    public static void disableDepthTest() {
        GlStateManager.disableDepth();
    }

    public static void glDepthMask(boolean mask) {
        GlStateManager.depthMask(mask);
    }

    /** {@link DepthState#isEnabled()} is the depth write mask, as in Angelica. */
    public static DepthState getDepthState() {
        GlStateManager.DepthState depth = GlStateManager.depthState;
        DepthState copy = new DepthState();
        copy.setEnabled(depth.maskEnabled);
        copy.setFunc(depth.depthFunc);
        return copy;
    }

    public static void enableAlphaTest() {
        GlStateManager.enableAlpha();
    }

    public static void disableAlphaTest() {
        GlStateManager.disableAlpha();
    }

    public static void glAlphaFunc(int function, float reference) {
        GlStateManager.alphaFunc(function, reference);
    }

    public static BooleanView getAlphaTest() {
        return ALPHA_TEST;
    }

    public static AlphaState getAlphaState() {
        GlStateManager.AlphaState alpha = GlStateManager.alphaState;
        AlphaState copy = new AlphaState();
        copy.setFunction(alpha.func);
        copy.setReference(alpha.ref);
        return copy;
    }

    // ---- culling, polygon offset, viewport

    public static void enableCull() {
        GlStateManager.enableCull();
    }

    public static void disableCull() {
        GlStateManager.disableCull();
    }

    public static void glPolygonOffset(float factor, float units) {
        GlStateManager.doPolygonOffset(factor, units);
    }

    public static void glViewport(int x, int y, int width, int height) {
        GlStateManager.viewport(x, y, width, height);
    }

    /** Called by the mixin on every {@code GlStateManager.viewport}. */
    public static void onViewport(int x, int y, int width, int height) {
        VIEWPORT[0] = x;
        VIEWPORT[1] = y;
        VIEWPORT[2] = width;
        VIEWPORT[3] = height;
    }

    public static int getViewportWidth() {
        return VIEWPORT[2];
    }

    public static int getViewportHeight() {
        return VIEWPORT[3];
    }

    // ---- fog

    public static FogState getFogState() {
        GlStateManager.FogState fog = GlStateManager.fogState;
        FOG_STATE.setFogMode(fog.mode);
        FOG_STATE.setDensity(fog.density);
        FOG_STATE.setStart(fog.start);
        FOG_STATE.setEnd(fog.end);
        return FOG_STATE;
    }

    public static BooleanView getFogMode() {
        return FOG_MODE;
    }

    public static Vector3d getFogColor() {
        return FOG_STATE.getFogColor();
    }

    /** Called by the mixin when {@code GL_FOG_COLOR} is set through {@code GlStateManager.glFog}. */
    public static void onFogColor(float red, float green, float blue, float alpha) {
        FOG_STATE.getFogColor().set(red, green, blue);
        FOG_STATE.setFogAlpha(alpha);
    }

    // ---- enable/disable, attributes, lightmap

    public static void glEnable(int cap) {
        switch (cap) {
            case GL11.GL_BLEND -> GlStateManager.enableBlend();
            case GL11.GL_ALPHA_TEST -> GlStateManager.enableAlpha();
            case GL11.GL_DEPTH_TEST -> GlStateManager.enableDepth();
            case GL11.GL_CULL_FACE -> GlStateManager.enableCull();
            case GL11.GL_FOG -> GlStateManager.enableFog();
            case GL11.GL_LIGHTING -> GlStateManager.enableLighting();
            case GL11.GL_TEXTURE_2D -> GlStateManager.enableTexture2D();
            case GL11.GL_POLYGON_OFFSET_FILL -> GlStateManager.enablePolygonOffset();
            case GL11.GL_COLOR_MATERIAL -> GlStateManager.enableColorMaterial();
            case GL11.GL_NORMALIZE -> GlStateManager.enableNormalize();
            default -> GL11.glEnable(cap);
        }
    }

    public static void glDisable(int cap) {
        switch (cap) {
            case GL11.GL_BLEND -> GlStateManager.disableBlend();
            case GL11.GL_ALPHA_TEST -> GlStateManager.disableAlpha();
            case GL11.GL_DEPTH_TEST -> GlStateManager.disableDepth();
            case GL11.GL_CULL_FACE -> GlStateManager.disableCull();
            case GL11.GL_FOG -> GlStateManager.disableFog();
            case GL11.GL_LIGHTING -> GlStateManager.disableLighting();
            case GL11.GL_TEXTURE_2D -> GlStateManager.disableTexture2D();
            case GL11.GL_POLYGON_OFFSET_FILL -> GlStateManager.disablePolygonOffset();
            case GL11.GL_COLOR_MATERIAL -> GlStateManager.disableColorMaterial();
            case GL11.GL_NORMALIZE -> GlStateManager.disableNormalize();
            default -> GL11.glDisable(cap);
        }
    }

    public static void glColor4f(float red, float green, float blue, float alpha) {
        GlStateManager.color(red, green, blue, alpha);
    }

    public static void glClearColor(float red, float green, float blue, float alpha) {
        GlStateManager.clearColor(red, green, blue, alpha);
    }

    public static void glShadeModel(int mode) {
        GlStateManager.shadeModel(mode);
    }

    public static void setLightmapTextureCoords(int unit, float x, float y) {
        OpenGlHelper.setLightmapTextureCoords(unit, x, y);
    }

    /**
     * GL's attribute stack. Vanilla's cache does not see what the pop restores, so the cached values the pushed bits
     * cover are read back from GL afterwards.
     */
    public static void glPushAttrib(int mask) {
        GL11.glPushAttrib(mask);
        ATTRIB_MASKS.push(mask);
    }

    public static void glPopAttrib() {
        GL11.glPopAttrib();
        resync(ATTRIB_MASKS.isEmpty() ? GL11.GL_ALL_ATTRIB_BITS : ATTRIB_MASKS.pop());
    }

    private static void resync(int mask) {
        if ((mask & (GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT)) != 0) {
            GlStateManager.blendState.blend.currentState = GL11.glIsEnabled(GL11.GL_BLEND);
            GlStateManager.alphaState.alphaTest.currentState = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        }
        if ((mask & GL11.GL_COLOR_BUFFER_BIT) != 0) {
            GlStateManager.BlendState blend = GlStateManager.blendState;
            blend.srcFactor = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            blend.dstFactor = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            blend.srcFactorAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            blend.dstFactorAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            GlStateManager.alphaState.func = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
            GlStateManager.alphaState.ref = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer colorMask = stack.malloc(4);
                GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
                GlStateManager.ColorMask cached = GlStateManager.colorMaskState;
                cached.red = colorMask.get(0) != 0;
                cached.green = colorMask.get(1) != 0;
                cached.blue = colorMask.get(2) != 0;
                cached.alpha = colorMask.get(3) != 0;
            }
        }
        if ((mask & (GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_ENABLE_BIT)) != 0) {
            GlStateManager.depthState.depthTest.currentState = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        }
        if ((mask & GL11.GL_DEPTH_BUFFER_BIT) != 0) {
            GlStateManager.depthState.depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            GlStateManager.depthState.maskEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        }
        if ((mask & (GL11.GL_POLYGON_BIT | GL11.GL_ENABLE_BIT)) != 0) {
            GlStateManager.cullState.cullFace.currentState = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        }
        if ((mask & (GL11.GL_FOG_BIT | GL11.GL_ENABLE_BIT)) != 0) {
            GlStateManager.fogState.fog.currentState = GL11.glIsEnabled(GL11.GL_FOG);
        }
        if ((mask & (GL11.GL_LIGHTING_BIT | GL11.GL_ENABLE_BIT)) != 0) {
            GlStateManager.lightingState.currentState = GL11.glIsEnabled(GL11.GL_LIGHTING);
        }
        if ((mask & GL11.GL_CURRENT_BIT) != 0) {
            float[] color = new float[4];
            GL11.glGetFloatv(GL11.GL_CURRENT_COLOR, color);
            GlStateManager.Color cached = GlStateManager.colorState;
            cached.red = color[0];
            cached.green = color[1];
            cached.blue = color[2];
            cached.alpha = color[3];
        }
    }

    // ---- matrices, buffers, framebuffers, draws

    public static void glMatrixMode(int mode) {
        GlStateManager.matrixMode(mode);
    }

    public static void glLoadMatrix(FloatBuffer matrix) {
        GL11.glLoadMatrixf(matrix);
    }

    public static void glPushMatrix() {
        GlStateManager.pushMatrix();
    }

    public static void glPopMatrix() {
        GlStateManager.popMatrix();
    }

    public static void glBindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    public static void glBindFramebuffer(int target, int framebuffer) {
        OpenGlHelper.glBindFramebuffer(target, framebuffer);
    }

    public static int glGenFramebuffers() {
        return GL30.glGenFramebuffers();
    }

    public static void glDrawArrays(int mode, int first, int count) {
        GlStateManager.glDrawArrays(mode, first, count);
    }

    public static void glUseProgram(int program) {
        GL20.glUseProgram(program);
    }
}
