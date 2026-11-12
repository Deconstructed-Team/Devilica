package net.coderbot.iris.gl.state;

/**
 * Holds some standard update notifiers for various elements of GL state.
 */
public class StateUpdateNotifiers {
	// Devilica: these four are raised from its own state hooks (GlStateManagerMixin, the pipeline's phase setters);
	// the base had the GL state manager's events for them
	public static final MultiListenerNotifier fogNotifier = new MultiListenerNotifier();
	public static final MultiListenerNotifier blendFuncNotifier = new MultiListenerNotifier();
	public static final MultiListenerNotifier bindTextureNotifier = new MultiListenerNotifier();
	public static final MultiListenerNotifier phaseChangeNotifier = new MultiListenerNotifier();
	public static ValueUpdateNotifier normalTextureChangeNotifier;
	public static ValueUpdateNotifier specularTextureChangeNotifier;
}
