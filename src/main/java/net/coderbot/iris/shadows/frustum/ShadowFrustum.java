package net.coderbot.iris.shadows.frustum;

import net.minecraft.client.renderer.culling.ICamera;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum;

/**
 * Devilica: base of the shadow pass's frustums. In 1.7.10 they extend vanilla's {@code Frustrum} for its camera
 * position; 1.12.2's {@code Frustum} keeps the position private, so they implement {@link ICamera}, which is what
 * vanilla's entity and tile entity culling takes, and keep the position here.
 */
public abstract class ShadowFrustum implements ICamera, ViewportProvider, Frustum {
	protected double xPosition;
	protected double yPosition;
	protected double zPosition;

	@Override
	public void setPosition(double cameraX, double cameraY, double cameraZ) {
		this.xPosition = cameraX;
		this.yPosition = cameraY;
		this.zPosition = cameraZ;
	}
}
