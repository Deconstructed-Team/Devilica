package net.coderbot.iris.uniforms;

import com.gtnewhorizons.angelica.rendering.RenderingState;
import net.coderbot.iris.gl.uniform.UniformHolder;
import net.coderbot.iris.pipeline.ShadowRenderer;
import net.coderbot.iris.shaderpack.PackDirectives;
import org.joml.Matrix4f;

import java.util.function.Supplier;

import static net.coderbot.iris.gl.uniform.UniformUpdateFrequency.PER_FRAME;

public final class MatrixUniforms {
	private static final Previous PREVIOUS_MODEL_VIEW = new Previous(RenderingState.INSTANCE::getModelViewMatrix);
	private static final Previous PREVIOUS_PROJECTION = new Previous(RenderingState.INSTANCE::getProjectionMatrix);

	private MatrixUniforms() {
	}

	public static void addMatrixUniforms(UniformHolder uniforms, PackDirectives directives) {
		addMatrix(uniforms, "ModelView", RenderingState.INSTANCE::getModelViewMatrix, PREVIOUS_MODEL_VIEW);
		// TODO: In some cases, gbufferProjectionInverse takes on a value much different than OptiFine...
		// We need to audit Mojang's linear algebra.
		addMatrix(uniforms, "Projection", RenderingState.INSTANCE::getProjectionMatrix, PREVIOUS_PROJECTION);
		addShadowMatrix(uniforms, "ModelView", () -> ShadowRenderer.createShadowModelView(directives.getSunPathRotation(), directives.getShadowDirectives().getIntervalSize()).peek().getModel());
        addShadowMatrix(uniforms, "Projection", () -> ShadowRenderer.createShadowProjection(directives.getShadowDirectives()));
	}

	private static void addMatrix(UniformHolder uniforms, String name, Supplier<Matrix4f> supplier, Previous previous) {
		uniforms
			.uniformMatrix(PER_FRAME, "gbuffer" + name, supplier)
			.uniformMatrix(PER_FRAME, "gbuffer" + name + "Inverse", new Inverted(supplier))
			.uniformMatrix(PER_FRAME, "gbufferPrevious" + name, previous);
	}

	private static void addShadowMatrix(UniformHolder uniforms, String name, Supplier<Matrix4f> supplier) {
		uniforms
				.uniformMatrix(PER_FRAME, "shadow" + name, supplier)
				.uniformMatrix(PER_FRAME, "shadow" + name + "Inverse", new Inverted(supplier));
	}

	private static class Inverted implements Supplier<Matrix4f> {
		private final Supplier<Matrix4f> parent;

		Inverted(Supplier<Matrix4f> parent) {
			this.parent = parent;
		}

		@Override
		public Matrix4f get() {
			// PERF: Don't copy + allocate this matrix every time?
            final Matrix4f copy = new Matrix4f(parent.get());

			copy.invert();

			return copy;
		}
	}


	/**
	 * One per matrix, shared by every program: the value of the frame before, whichever programs drew in it. The
	 * uniforms copy the matrix they are handed.
	 */
	private static final class Previous implements Supplier<Matrix4f> {
		private final Supplier<Matrix4f> parent;
		private final Matrix4f previous = new Matrix4f();
		private final Matrix4f current = new Matrix4f();
		private int lastFrame = -1;

		Previous(Supplier<Matrix4f> parent) {
			this.parent = parent;
		}

		@Override
		public Matrix4f get() {
			final int frame = SystemTimeUniforms.COUNTER.getAsInt();

			if (frame != lastFrame) {
				lastFrame = frame;
				previous.set(current);
				current.set(parent.get());
			}

			return previous;
		}
	}
}
