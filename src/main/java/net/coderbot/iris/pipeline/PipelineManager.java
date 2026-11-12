package net.coderbot.iris.pipeline;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import lombok.Getter;
import net.coderbot.iris.Iris;
import net.coderbot.iris.uniforms.SystemTimeUniforms;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class PipelineManager {

	private final Function<String, WorldRenderingPipeline> pipelineFactory;
	// Devilica: what tells one dimension's pipeline from another's, the pack's set of programs for it
	private final Function<String, Object> programsOf;
	private Object lastPreparedPrograms = null;
	private final Map<String, WorldRenderingPipeline> pipelinesPerDimension = new HashMap<>();
	private WorldRenderingPipeline pipeline = new FixedFunctionWorldRenderingPipeline();
	private String lastPreparedDimension = null;
    @Getter
	private int versionCounterForSodiumShaderReload = 0;

	public PipelineManager(Function<String, WorldRenderingPipeline> pipelineFactory, Function<String, Object> programsOf) {
		this.pipelineFactory = pipelineFactory;
		this.programsOf = programsOf;
	}

	public WorldRenderingPipeline preparePipeline(String currentDimension) {
		// Detect dimension change and do full teardown/recreation
		if (lastPreparedDimension != null && !lastPreparedDimension.equals(currentDimension)) {
			// Devilica: unless the pack draws both with the same programs, as most do for the overworld and every
			// dimension mods add. The pipeline reads the world as it draws, so it serves the new one as it is, and
			// the trip does not wait several seconds for the same programs to compile again.
			final Object programs = this.programsOf.apply(currentDimension);
			final WorldRenderingPipeline kept = pipelinesPerDimension.get(lastPreparedDimension);
			if (programs != null && programs == lastPreparedPrograms && kept != null && pipelinesPerDimension.size() == 1) {
				Iris.logger.info("Dimension changed from '{}' to '{}', same programs: keeping the pipeline", lastPreparedDimension, currentDimension);
				pipelinesPerDimension.clear();
				pipelinesPerDimension.put(currentDimension, kept);
			} else {
				Iris.logger.info("Dimension changed from '{}' to '{}', reloading pipeline", lastPreparedDimension, currentDimension);
				destroyPipeline();
			}
		}
		lastPreparedDimension = currentDimension;

		if (!pipelinesPerDimension.containsKey(currentDimension)) {
			SystemTimeUniforms.COUNTER.reset();
			SystemTimeUniforms.TIMER.reset();

			Iris.logger.info("Creating pipeline for dimension '{}'", currentDimension);
			pipeline = pipelineFactory.apply(currentDimension);
			pipelinesPerDimension.put(currentDimension, pipeline);
			lastPreparedPrograms = this.programsOf.apply(currentDimension);
		} else {
			pipeline = pipelinesPerDimension.get(currentDimension);
		}

		return pipeline;
	}

	@Nullable
	public WorldRenderingPipeline getPipelineNullable() {
		return pipeline;
	}

	public Optional<WorldRenderingPipeline> getPipeline() {
		return Optional.ofNullable(pipeline);
	}


	/**
	 * Destroys all the current pipelines.
	 *
	 * <p>This method is <b>EXTREMELY DANGEROUS!</b> It is a huge potential source of hard-to-trace inconsistencies
	 * in program state. You must make sure that you <i>immediately</i> re-prepare the pipeline after destroying
	 * it to prevent the program from falling into an inconsistent state.</p>
	 *
	 * <p>In particular, </p>
	 *
	 * @see <a href="https://github.com/IrisShaders/Iris/issues/1330">this GitHub issue</a>
	 */
	public void destroyPipeline() {
		pipelinesPerDimension.forEach((dimensionName, pipeline) -> {
			Iris.logger.info("Destroying pipeline for dimension '{}'", dimensionName);
			resetTextureState();
			pipeline.destroy();
		});

		pipelinesPerDimension.clear();
		pipeline = null;
		lastPreparedDimension = null;
		lastPreparedPrograms = null;
		versionCounterForSodiumShaderReload++;
	}

	private void resetTextureState() {
		// Unbind all textures
		//
		// This is necessary because we don't want destroyed render target textures to remain bound to certain texture
		// units. Vanilla appears to properly rebind all textures as needed, and we do so too, so this does not cause
		// issues elsewhere.
		//
		// Without this code, there will be weird issues when reloading certain shaderpacks.
		for (int i = 0; i < 16; i++) {
            GLStateManager.glActiveTexture(GL13.GL_TEXTURE0 + i);
			GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		}

		// Set the active texture unit to unit 0
		//
		// This seems to be what most code expects. It's a sane default in any case.
        GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
	}
}
