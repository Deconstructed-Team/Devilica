package net.coderbot.iris.gl.blending;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.RenderSystem;
import com.gtnewhorizons.angelica.glsm.states.BlendState;
import lombok.Getter;
import org.lwjgl.opengl.GL11;

public class BlendModeStorage {
	private static final int MAX_DRAW_BUFFERS = 8;

	private static boolean originalBlendEnable;
	private static final BlendState originalBlend = new BlendState();
	@Getter private static boolean blendLocked;
	@Getter private static boolean hasDeferredChanges;
	private static boolean globalOverride;
	private static Runnable gameBlendWatcher;
	private static boolean gameBlendChanged;
	private static final int[] bufferOverrideIndices = new int[MAX_DRAW_BUFFERS];
	private static final boolean[] bufferOverrideIsDisable = new boolean[MAX_DRAW_BUFFERS];
	private static final BlendState[] bufferOverrideStates = new BlendState[MAX_DRAW_BUFFERS];
	private static int bufferOverrideCount;

	static {
		for (int i = 0; i < MAX_DRAW_BUFFERS; i++) {
			bufferOverrideStates[i] = new BlendState();
		}
	}

    public static void overrideBlend(BlendState override) {
		if (!blendLocked) {
			// Only save the previous state if the blend mode wasn't already locked
			originalBlendEnable = GLStateManager.getBlendMode().isEnabled();
            originalBlend.set(GLStateManager.getBlendState());
		}

		final boolean buffersTouched = bufferOverrideCount > 0;
		bufferOverrideCount = 0;
		hasDeferredChanges = false;
		globalOverride = true;
		blendLocked = false;

		if (buffersTouched) {
			// Devilica: the last pass's per-buffer overrides end here, for every draw buffer
			if (override == null) {
				final BlendState current = GLStateManager.getBlendState();
				GLStateManager.forceBlend(false, current.getSrcRgb(), current.getDstRgb(), current.getSrcAlpha(), current.getDstAlpha());
			} else {
				GLStateManager.forceBlend(true, override.getSrcRgb(), override.getDstRgb(), override.getSrcAlpha(), override.getDstAlpha());
			}
		} else if (override == null) {
            GLStateManager.disableBlend();
		} else {
            GLStateManager.enableBlend();
            GLStateManager.tryBlendFuncSeparate(override.getSrcRgb(), override.getDstRgb(), override.getSrcAlpha(), override.getDstAlpha());
		}

		blendLocked = true;
	}

	public static void overrideBufferBlend(int index, BlendState override) {
		if (!blendLocked) {
			// Only save the previous state if the blend mode wasn't already locked
            originalBlendEnable = GLStateManager.getBlendMode().isEnabled();
            originalBlend.set(GLStateManager.getBlendState());
		}

		if (override == null) {
			RenderSystem.disableBufferBlend(index);
		} else {
			RenderSystem.enableBufferBlend(index);
			RenderSystem.blendFuncSeparatei(index, override.getSrcRgb(), override.getDstRgb(), override.getSrcAlpha(), override.getDstAlpha());
		}

		if (bufferOverrideCount < MAX_DRAW_BUFFERS) {
			final int slot = bufferOverrideCount++;
			bufferOverrideIndices[slot] = index;
			bufferOverrideIsDisable[slot] = (override == null);
			if (override != null) {
				bufferOverrideStates[slot].setAll(override.getSrcRgb(), override.getDstRgb(), override.getSrcAlpha(), override.getDstAlpha());
			}
		}
		blendLocked = true;
	}

	public static void deferBlendModeToggle(boolean enabled) {
		if (enabled == originalBlendEnable) return;
		originalBlendEnable = enabled;
		hasDeferredChanges = true;
	}

	public static void deferBlendFunc(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
		if (originalBlend.getSrcRgb() == srcRgb && originalBlend.getDstRgb() == dstRgb
			&& originalBlend.getSrcAlpha() == srcAlpha && originalBlend.getDstAlpha() == dstAlpha) {
			return;
		}
        originalBlend.setAll(srcRgb, dstRgb, srcAlpha, dstAlpha);
		hasDeferredChanges = true;
	}

	/**
	 * Devilica: whether the game itself, whatever a pack's override made of it, is drawing with plain alpha blending.
	 */
	public static boolean isGameBlendingAlpha() {
		if (blendLocked) {
			return originalBlendEnable && originalBlend.getSrcRgb() == GL11.GL_SRC_ALPHA
				&& originalBlend.getDstRgb() == GL11.GL_ONE_MINUS_SRC_ALPHA;
		}
		return GLStateManager.isBlendingAlpha();
	}

	/** Devilica: run before the next draw after the game changed its blending, applied or deferred; null for nobody. */
	public static void watchGameBlend(Runnable watcher) {
		gameBlendWatcher = watcher;
		gameBlendChanged = false;
	}

	public static void gameBlendChanged() {
		gameBlendChanged = gameBlendWatcher != null;
	}

	public static void beforeDraw() {
		if (gameBlendChanged) {
			gameBlendChanged = false;
			gameBlendWatcher.run();
		}
		flushDeferredBlend();
	}

	public static void flushDeferredBlend() {
		// A program's own blend mode stands until the program goes; only per-buffer overrides let the game's show through
		if (!blendLocked || !hasDeferredChanges || globalOverride) return;
		hasDeferredChanges = false;
		blendLocked = false;

		applyOriginal(bufferOverrideCount > 0);

		blendLocked = true;

		for (int i = 0; i < bufferOverrideCount; i++) {
			final int idx = bufferOverrideIndices[i];
			if (bufferOverrideIsDisable[i]) {
				RenderSystem.disableBufferBlend(idx);
			} else {
				final BlendState s = bufferOverrideStates[i];
				RenderSystem.enableBufferBlend(idx);
				RenderSystem.blendFuncSeparatei(idx, s.getSrcRgb(), s.getDstRgb(), s.getSrcAlpha(), s.getDstAlpha());
			}
		}
	}

	public static void restoreBlend() {
		if (!blendLocked) {
			return;
		}

		blendLocked = false;
		hasDeferredChanges = false;
		globalOverride = false;
		final boolean buffersTouched = bufferOverrideCount > 0;
		bufferOverrideCount = 0;

		applyOriginal(buffersTouched);
	}

	// Devilica: the game's own blend state. After per-buffer overrides it is set for every draw buffer directly: the
	// state cache speaks for buffer 0 alone, and a call it skipped left the other buffers with the override.
	private static void applyOriginal(boolean buffersTouched) {
		if (buffersTouched) {
			GLStateManager.forceBlend(originalBlendEnable, originalBlend.getSrcRgb(), originalBlend.getDstRgb(),
				originalBlend.getSrcAlpha(), originalBlend.getDstAlpha());
			return;
		}
		if (originalBlendEnable) {
			GLStateManager.enableBlend();
		} else {
			GLStateManager.disableBlend();
		}
		GLStateManager.tryBlendFuncSeparate(originalBlend.getSrcRgb(), originalBlend.getDstRgb(), originalBlend.getSrcAlpha(), originalBlend.getDstAlpha());
	}
}
