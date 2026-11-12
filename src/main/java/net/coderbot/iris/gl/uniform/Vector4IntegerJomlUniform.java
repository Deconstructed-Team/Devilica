package net.coderbot.iris.gl.uniform;

import com.gtnewhorizons.angelica.glsm.RenderSystem;
import net.coderbot.iris.gl.state.ValueUpdateNotifier;
import org.joml.Vector4i;

import java.util.function.Supplier;

public class Vector4IntegerJomlUniform extends Uniform {
	private final Vector4i cachedValue;
	private final Supplier<Vector4i> value;

	Vector4IntegerJomlUniform(int location, Supplier<Vector4i> value) {
		this(location, value, null);
	}

	Vector4IntegerJomlUniform(int location, Supplier<Vector4i> value, ValueUpdateNotifier notifier) {
		super(location, notifier);

		this.cachedValue = new Vector4i();
		this.value = value;
	}

	@Override
	public void update() {
		updateValue();

		if (notifier != null) {
			notifier.setListener(this::updateValue);
		}
	}

	private void updateValue() {
		Vector4i newValue = value.get();

		if (!newValue.equals(cachedValue)) {
			cachedValue.set(newValue);
			RenderSystem.uniform4i(this.location, cachedValue.x, cachedValue.y, cachedValue.z, cachedValue.w);
		}
	}
}
