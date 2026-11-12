package net.coderbot.iris.gl.uniform;

import com.gtnewhorizons.angelica.glsm.RenderSystem;
import net.coderbot.iris.gl.state.ValueUpdateNotifier;
import org.joml.Vector3i;

import java.util.function.Supplier;

public class Vector3IntegerJomlUniform extends Uniform {
    private final Vector3i cachedValue;
    private final Supplier<Vector3i> value;

    Vector3IntegerJomlUniform(int location, Supplier<Vector3i> value) {
        this(location, value, null);
    }

    Vector3IntegerJomlUniform(int location, Supplier<Vector3i> value, ValueUpdateNotifier notifier) {
        super(location, notifier);

        this.cachedValue = new Vector3i();
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
        Vector3i newValue = value.get();

        if (!newValue.equals(cachedValue)) {
            cachedValue.set(newValue);
            RenderSystem.uniform3i(this.location, cachedValue.x, cachedValue.y, cachedValue.z);
        }
    }
}
