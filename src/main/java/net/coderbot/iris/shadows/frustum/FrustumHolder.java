package net.coderbot.iris.shadows.frustum;

import net.minecraft.client.renderer.culling.ICamera;

public class FrustumHolder {
	private ICamera frustum;
	private String distanceInfo = "(unavailable)";
	private String cullingInfo = "(unavailable)";

	public FrustumHolder setInfo(ICamera frustum, String distanceInfo, String cullingInfo) {
		this.frustum = frustum;
		this.distanceInfo = distanceInfo;
		this.cullingInfo = cullingInfo;
		return this;
	}

	public ICamera getFrustum() {
		return frustum;
	}

	public String getDistanceInfo() {
		return distanceInfo;
	}

	public String getCullingInfo() {
		return cullingInfo;
	}
}
