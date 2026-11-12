package net.coderbot.iris.uniforms;

import com.gtnewhorizons.angelica.compat.mojang.Camera;
import com.gtnewhorizons.angelica.compat.mojang.GameModeUtil;
import dev.feldsher.devilica.config.DevilicaOptions;
import net.coderbot.iris.gl.uniform.UniformHolder;
import net.coderbot.iris.gl.uniform.UniformUpdateFrequency;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.world.WorldProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4f;

import java.util.List;

public class IrisExclusiveUniforms {
	// Reusable vectors to avoid allocations every frame
	private static final Vector3d eyePositionCache = new Vector3d();
	private static final Vector3d relativeEyePositionCache = new Vector3d();
	private static final Vector4f lightningBoltPositionCache = new Vector4f();
	private static final Vector4f ZERO_VECTOR_4f = new Vector4f(0, 0, 0, 0);

	public static void addIrisExclusiveUniforms(UniformHolder uniforms) {
		WorldInfoUniforms.addWorldInfoUniforms(uniforms);

		//All Iris-exclusive uniforms (uniforms which do not exist in either OptiFine or ShadersMod) should be registered here.
		uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "thunderStrength", IrisExclusiveUniforms::getThunderStrength);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHealth", IrisExclusiveUniforms::getCurrentHealth);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHealth", IrisExclusiveUniforms::getMaxHealth);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHunger", IrisExclusiveUniforms::getCurrentHunger);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHunger", () -> 20);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerAir", IrisExclusiveUniforms::getCurrentAir);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerAir", IrisExclusiveUniforms::getMaxAir);
		uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "firstPersonCamera", IrisExclusiveUniforms::isFirstPersonCamera);
		uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "isSpectator", IrisExclusiveUniforms::isSpectator);
		uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "isRightHanded", () -> true); // 1.7.10 doesn't support left-handed mode
		uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "eyePosition", IrisExclusiveUniforms::getEyePosition);
		uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "relativeEyePosition", IrisExclusiveUniforms::getRelativeEyePosition);
		uniforms.uniform4f(UniformUpdateFrequency.PER_TICK, "lightningBoltPosition", IrisExclusiveUniforms::getLightningBoltPosition);
	}

	private static float getThunderStrength() {
		// Note: Ensure this is in the range of 0 to 1 - some custom servers send out of range values.
		return Math.clamp(0.0F, 1.0F, Minecraft.getMinecraft().world.thunderingStrength);
	}

	private static float getCurrentHealth() {
		if (Minecraft.getMinecraft().player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
			return -1;
		}

		return Minecraft.getMinecraft().player.getHealth() / Minecraft.getMinecraft().player.getMaxHealth();
	}

	private static float getCurrentHunger() {
		if (Minecraft.getMinecraft().player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
			return -1;
		}

		return Minecraft.getMinecraft().player.getFoodStats().getFoodLevel() / 20f;
	}

	private static float getCurrentAir() {
		if (Minecraft.getMinecraft().player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
			return -1;
		}

		return (float) Minecraft.getMinecraft().player.getAir() / (float) Minecraft.getMinecraft().player.getAir();
	}

	private static float getMaxAir() {
		if (Minecraft.getMinecraft().player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
			return -1;
		}

//		return Minecraft.getMinecraft().player.getMaxAirSupply();
		return 300.0F;
	}

	private static float getMaxHealth() {
		if (Minecraft.getMinecraft().player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
			return -1;
		}

		return Minecraft.getMinecraft().player.getMaxHealth();
	}

	private static boolean isFirstPersonCamera() {
		// If camera type is not explicitly third-person, assume it's first-person.
        return !Camera.INSTANCE.isThirdPerson();
	}

	private static boolean isSpectator() {
		return GameModeUtil.isSpectator();
	}

	private static Vector3d getEyePosition() {
        final Entity eye = Minecraft.getMinecraft().getRenderViewEntity();
        // Devilica: at the frame's moment, as cameraPosition is; the tick's position trailed it by a step
        final double tickDelta = CapturedRenderingState.INSTANCE.getTickDelta();
        return eyePositionCache.set(
            eye.lastTickPosX + (eye.posX - eye.lastTickPosX) * tickDelta,
            eye.lastTickPosY + (eye.posY - eye.lastTickPosY) * tickDelta + eye.getEyeHeight(),
            eye.lastTickPosZ + (eye.posZ - eye.lastTickPosZ) * tickDelta);
	}

	private static Vector3d getRelativeEyePosition() {
		final Vector3dc cameraPos = CameraUniforms.getUnshiftedCameraPosition();
		final Vector3d eyePos = getEyePosition();
		// Devilica: Iris gives the camera as seen from the eye (packs add it to camera-relative positions); the base
		// had the eye as seen from the camera, which on 1.12, with the camera at the feet, was off by twice the eye height
		return relativeEyePositionCache.set(cameraPos).sub(eyePos);
	}

	private static Vector4f getLightningBoltPosition() {
		if (Minecraft.getMinecraft().world != null) {
			final List<Entity> weatherEffects = Minecraft.getMinecraft().world.weatherEffects;
			for (Entity entity : weatherEffects) {
				if (entity instanceof EntityLightningBolt bolt) {
                    final Vector3dc cameraPos = CameraUniforms.getUnshiftedCameraPosition();
					return lightningBoltPositionCache.set(
						(float)(bolt.posX - cameraPos.x()),
						(float)(bolt.posY - cameraPos.y()),
						(float)(bolt.posZ - cameraPos.z()),
						1.0f
					);
				}
			}
		}
		return ZERO_VECTOR_4f;
	}

	public static class WorldInfoUniforms {
		public static void addWorldInfoUniforms(UniformHolder uniforms) {
			uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "bedrockLevel", () -> 0);
			uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "cloudHeight", () -> {
				final WorldProvider provider = provider();
				return provider != null ? provider.getCloudHeight() + DevilicaOptions.get().details.cloudHeight : 192.0f;
			});
			uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "heightLimit", () -> {
				final WorldProvider provider = provider();
				return provider != null ? provider.getHeight() : 256;
			});
			uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasCeiling", () -> {
				final WorldProvider provider = provider();
				return provider != null && !provider.hasSkyLight();
			});
			uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasSkylight", () -> {
				final WorldProvider provider = provider();
				return provider == null || provider.hasSkyLight();
			});
			uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "ambientLight", () -> {
				final WorldProvider provider = provider();
				return provider != null ? provider.getLightBrightnessTable()[0] : 0f;
			});
		}

		// The world of the moment: the pipeline can be made before the world is set (it then stayed null) and outlive it.
		@Nullable
		private static WorldProvider provider() {
			final WorldClient level = Minecraft.getMinecraft().world;
			return level != null ? level.provider : null;
		}
	}
}
