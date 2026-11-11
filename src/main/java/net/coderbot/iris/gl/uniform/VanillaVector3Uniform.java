package net.coderbot.iris.gl.uniform;

import com.gtnewhorizons.angelica.glsm.RenderSystem;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

public class VanillaVector3Uniform extends Uniform {
	private Vec3d cachedValue;
	private final Supplier<Vec3d> value;

	VanillaVector3Uniform(int location, Supplier<Vec3d> value) {
		super(location);

		this.cachedValue = null;
		this.value = value;
	}

	@Override
	public void update() {
		final Vec3d newValue = value.get();

		if (!newValue.equals(cachedValue)) {
			cachedValue = newValue;
			RenderSystem.uniform3f(location, (float) newValue.x, (float) newValue.y, (float) newValue.z);
		}
	}
}
