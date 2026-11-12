package net.coderbot.iris.pipeline;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizons.angelica.compat.mojang.Camera;
import com.gtnewhorizons.angelica.compat.toremove.MatrixStack;
import com.gtnewhorizons.angelica.config.AngelicaConfig;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.RenderSystem;
import com.gtnewhorizons.angelica.glsm.states.AlphaState;
import com.gtnewhorizons.angelica.rendering.RenderingState;
import dev.feldsher.devilica.tesr.BlockEntityBatcher;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.coderbot.iris.Iris;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.gui.option.IrisVideoSettings;
import net.coderbot.iris.shaderpack.PackDirectives;
import net.coderbot.iris.shaderpack.ShadowCullState;
import net.coderbot.iris.shaderpack.PackShadowDirectives;
import net.coderbot.iris.shaderpack.ProgramSource;
import net.coderbot.iris.shadow.ShadowMatrices;
import net.coderbot.iris.shadows.CullingDataCache;
import net.coderbot.iris.shadows.ShadowCompositeRenderer;
import net.coderbot.iris.shadows.ShadowRenderTargets;
import net.coderbot.iris.shadows.frustum.BoxCuller;
import net.coderbot.iris.shadows.frustum.CullEverythingFrustum;
import net.coderbot.iris.shadows.frustum.FrustumHolder;
import net.coderbot.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import net.coderbot.iris.shadows.frustum.advanced.SafeZoneCullingFrustum;
import net.coderbot.iris.shadows.frustum.fallback.BoxCullingFrustum;
import net.coderbot.iris.shadows.frustum.fallback.NonCullingFrustum;
import net.coderbot.iris.uniforms.CameraUniforms;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.CelestialUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import com.gtnewhorizons.angelica.compat.mojang.GameModeUtil;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBTextureSwizzle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

public class ShadowRenderer {
	public static final Matrix4f MODELVIEW = new Matrix4f();
    public static final FloatBuffer MODELVIEW_BUFFER = BufferUtils.createFloatBuffer(16);
	public static final Matrix4f PROJECTION = new Matrix4f();
	public static final List<TileEntity> visibleTileEntities = new ArrayList<>();
	public static final List<TileEntity> globalTileEntities = new ArrayList<>();
	public static boolean ACTIVE = false;

	private static final Comparator<Entity> ENTITY_CLASS_COMPARATOR = Comparator.comparingInt(a -> System.identityHashCode(a.getClass()));
	private static final NonCullingFrustum NON_CULLING_FRUSTUM = new NonCullingFrustum();
	private static final CullEverythingFrustum CULL_EVERYTHING_FRUSTUM = new CullEverythingFrustum();

	public static ShadowRenderTargets CURRENT_TARGETS = null;
	private final float halfPlaneLength;
	private final float voxelDistance;
	private final float renderDistanceMultiplier;
	private final float entityShadowDistanceMultiplier;
	private final int resolution;
	private final float intervalSize;
	private final Float fov;
	private final ShadowRenderTargets targets;
	private final ShadowCullState packCullingState;
	private final ShadowCompositeRenderer compositeRenderer;
	private final Runnable rebindShadowProgram;
	private boolean packHasVoxelization;
	private final boolean shouldRenderTerrain;
	private final boolean shouldRenderTranslucent;
	private final boolean shouldRenderEntities;
	private final boolean shouldRenderPlayer;
	private final boolean shouldRenderBlockEntities;
	private final float sunPathRotation;
	private final List<MipmapPass> mipmapPasses = new ArrayList<>();
	private final String debugStringOverall;
	private FrustumHolder terrainFrustumHolder;
	private FrustumHolder entityFrustumHolder;
	private int renderedShadowEntities = 0;
	private int shadowFrame;
	private int renderedShadowTileEntities = 0;
	private Profiler profiler;
	private final List<Entity> renderedEntitiesList = new ArrayList<>(64);
	private final MatrixStack shadowModelView = new MatrixStack();
	private final CelestialUniforms celestialUniforms;


	private final Vector3f shadowLightVectorCache = new Vector3f();
	private final FrustumCaches terrainFrustumCaches = new FrustumCaches();
	private final FrustumCaches entityFrustumCaches = new FrustumCaches();
	private BoxCuller cachedTileEntityCuller;
	private double lastTileEntityCullerDistance = -1;

	// Devilica: terrain and entities keep frustums of their own, as in newer Angelica. The shadow search still runs
	// while entities draw and reads the terrain frustum; set up for entities, it culled at their far shorter distance,
	// and casters dropped out of the shadow map for a frame now and then.
	private static final class FrustumCaches {
		final AdvancedShadowCullingFrustum advancedFrustum = new AdvancedShadowCullingFrustum();
		BoxCuller boxCuller;
		BoxCullingFrustum boxCullingFrustum;
		BoxCuller advancedBoxCuller;
		double boxCullerDistance = -1;
		double advancedBoxCullerDistance = -1;
	}

	public ShadowRenderer(ProgramSource shadow, PackDirectives directives, ShadowRenderTargets shadowRenderTargets, ShadowCompositeRenderer compositeRenderer, Runnable rebindShadowProgram) {

		this.profiler = Minecraft.getMinecraft().profiler;
		this.rebindShadowProgram = rebindShadowProgram;

		final PackShadowDirectives shadowDirectives = directives.getShadowDirectives();

		this.halfPlaneLength = shadowDirectives.getDistance();
		this.voxelDistance = shadowDirectives.getVoxelDistance();
		this.renderDistanceMultiplier = shadowDirectives.getDistanceRenderMul();
		this.entityShadowDistanceMultiplier = shadowDirectives.getEntityShadowDistanceMul();
		this.resolution = shadowDirectives.getResolution();
		this.intervalSize = shadowDirectives.getIntervalSize();
		this.shouldRenderTerrain = shadowDirectives.shouldRenderTerrain();
		this.shouldRenderTranslucent = shadowDirectives.shouldRenderTranslucent();
		this.shouldRenderEntities = shadowDirectives.shouldRenderEntities();
		this.shouldRenderPlayer = shadowDirectives.shouldRenderPlayer();
		this.shouldRenderBlockEntities = shadowDirectives.shouldRenderBlockEntities();

		this.compositeRenderer = compositeRenderer;

		debugStringOverall = "half plane = " + halfPlaneLength + " meters @ " + resolution + "x" + resolution;

		this.terrainFrustumHolder = new FrustumHolder();
		this.entityFrustumHolder = new FrustumHolder();

		this.fov = shadowDirectives.getFov();
		this.targets = shadowRenderTargets;

		if (shadow != null) {
			// Assume that the shader pack is doing voxelization if a geometry shader is detected.
			// Also assume voxelization if image load / store is detected.
			this.packHasVoxelization = shadow.getGeometrySource().isPresent();
			this.packCullingState = shadowDirectives.getCullingState();
		} else {
			this.packHasVoxelization = false;
			this.packCullingState = ShadowCullState.DEFAULT;
		}

		this.sunPathRotation = directives.getSunPathRotation();
		this.celestialUniforms = new CelestialUniforms(this.sunPathRotation);

//		this.buffers = new RenderBuffers();
//
//		if (this.buffers instanceof RenderBuffersExt) {
//			this.renderBuffersExt = (RenderBuffersExt) buffers;
//		} else {
//			this.renderBuffersExt = null;
//		}

		configureSamplingSettings(shadowDirectives);
	}

	public void setUsesImages(boolean usesImages) {
		this.packHasVoxelization = packHasVoxelization || usesImages;
	}

	public static MatrixStack createShadowModelView(float sunPathRotation, float intervalSize) {
		// Use entity position for shadow matrix
		final Vector3d entityPos = Camera.INSTANCE.getEntityPos();

		// Set up our modelview matrix stack
		final MatrixStack modelView = new MatrixStack();
		ShadowMatrices.createModelViewMatrix(modelView, getShadowAngle(), intervalSize, sunPathRotation, entityPos.x, entityPos.y, entityPos.z);

		return modelView;
	}

	private MatrixStack getShadowModelView() {
		final Vector3d entityPos = Camera.INSTANCE.getEntityPos();

		shadowModelView.reset();
		ShadowMatrices.createModelViewMatrix(shadowModelView, getShadowAngle(), this.intervalSize, this.sunPathRotation, entityPos.x, entityPos.y, entityPos.z);
		return shadowModelView;
	}

	private static WorldClient getLevel() {
		return Objects.requireNonNull(Minecraft.getMinecraft().world);
	}

	private static float getSkyAngle() {
        return Minecraft.getMinecraft().world.getCelestialAngle(CapturedRenderingState.INSTANCE.getTickDelta());
	}

	private static float getSunAngle() {
		final float skyAngle = getSkyAngle();

		if (skyAngle < 0.75F) {
			return skyAngle + 0.25F;
		} else {
			return skyAngle - 0.75F;
		}
	}

	private static float getShadowAngle() {
		float shadowAngle = getSunAngle();

		if (!CelestialUniforms.isDay()) {
			shadowAngle -= 0.5F;
		}

		return shadowAngle;
	}

	private void configureSamplingSettings(PackShadowDirectives shadowDirectives) {
		final ImmutableList<PackShadowDirectives.DepthSamplingSettings> depthSamplingSettings =
			shadowDirectives.getDepthSamplingSettings();

		final ImmutableList<PackShadowDirectives.SamplingSettings> colorSamplingSettings =
			shadowDirectives.getColorSamplingSettings();

		GLStateManager.glActiveTexture(GL13.GL_TEXTURE4);

		configureDepthSampler(targets.getDepthTexture().getTextureId(), depthSamplingSettings.get(0));

		configureDepthSampler(targets.getDepthTextureNoTranslucents().getTextureId(), depthSamplingSettings.get(1));

		for (int i = 0; i < Math.min(colorSamplingSettings.size(), targets.getNumColorTextures()); i++) {
			if (targets.get(i) == null) continue;
			int glTextureId = targets.get(i).getMainTexture();

			configureSampler(glTextureId, colorSamplingSettings.get(i));
		}

		GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
	}

    private final IntBuffer swizzleBuf = BufferUtils.createIntBuffer(4);
	private void configureDepthSampler(int glTextureId, PackShadowDirectives.DepthSamplingSettings settings) {
		if (settings.getHardwareFiltering()) {
			// We have to do this or else shadow hardware filtering breaks entirely!
			RenderSystem.texParameteri(glTextureId, GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL30.GL_COMPARE_REF_TO_TEXTURE);
		}

		// Workaround for issues with old shader packs like Chocapic v4.
		// They expected the driver to put the depth value in z, but it's supposed to only
		// be available in r. So we set up the swizzle to fix that.
        swizzleBuf.rewind();
        swizzleBuf.put(new int[] { GL11.GL_RED, GL11.GL_RED, GL11.GL_RED, GL11.GL_ONE }).rewind();
		RenderSystem.texParameteriv(glTextureId, GL11.GL_TEXTURE_2D, ARBTextureSwizzle.GL_TEXTURE_SWIZZLE_RGBA, swizzleBuf);

		configureSampler(glTextureId, settings);
	}

	private void configureSampler(int glTextureId, PackShadowDirectives.SamplingSettings settings) {
		if (settings.getMipmap()) {
			final int filteringMode = settings.getNearest() ? GL11.GL_NEAREST_MIPMAP_NEAREST : GL11.GL_LINEAR_MIPMAP_LINEAR;
			mipmapPasses.add(new MipmapPass(glTextureId, filteringMode));
		}

		if (!settings.getNearest()) {
			// Make sure that things are smoothed
			RenderSystem.texParameteri(glTextureId, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			RenderSystem.texParameteri(glTextureId, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		} else {
			RenderSystem.texParameteri(glTextureId, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			RenderSystem.texParameteri(glTextureId, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		}
	}

	private void generateMipmaps() {
		GLStateManager.glActiveTexture(GL13.GL_TEXTURE4);

		for (MipmapPass mipmapPass : mipmapPasses) {
			setupMipmappingForTexture(mipmapPass.getTexture(), mipmapPass.getTargetFilteringMode());
		}

		GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
	}

	private void setupMipmappingForTexture(int texture, int filteringMode) {
		RenderSystem.generateMipmaps(texture, GL11.GL_TEXTURE_2D);
		RenderSystem.texParameteri(texture, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filteringMode);
	}

	private FrustumHolder createShadowFrustum(float renderMultiplier, FrustumHolder holder, FrustumCaches caches) {
		// TODO: Cull entities / block entities with Advanced Frustum Culling even if voxelization is detected.
		String distanceInfo;
		String cullingInfo;
		if ((packCullingState == ShadowCullState.DISTANCE || packHasVoxelization) && packCullingState != ShadowCullState.ADVANCED && packCullingState != ShadowCullState.SAFE_ZONE) {
			double distance = halfPlaneLength * renderMultiplier;

			String reason;

			if (packCullingState == ShadowCullState.DISTANCE) {
				reason = "(set by shader pack)";
			} else /*if (packHasVoxelization)*/ {
				reason = "(voxelization detected)";
			}

			if (distance <= 0 || distance > Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16) {
				distanceInfo = Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16
					+ " blocks (capped by normal render distance)";
				cullingInfo = "disabled " + reason;
				return holder.setInfo(NON_CULLING_FRUSTUM, distanceInfo, cullingInfo);
			} else {
				distanceInfo = distance + " blocks (set by shader pack)";
				cullingInfo = "distance only " + reason;
				holder.setInfo(getOrCreateBoxCullingFrustum(caches, distance), distanceInfo, cullingInfo);
			}
		} else {
			BoxCuller boxCuller;

			boolean hasSafeZone = packCullingState == ShadowCullState.SAFE_ZONE;

			if (hasSafeZone && renderMultiplier < 0) renderMultiplier = 1.0f;

			double distance = (hasSafeZone ? voxelDistance : halfPlaneLength) * renderMultiplier;
			String setter = "(set by shader pack)";

			if (renderMultiplier < 0) {
                // TODO: GUI
				distance = IrisVideoSettings.shadowDistance * 16; // can be zero :(
				setter = "(set by user)";
			}

			if (distance >= Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16 && !hasSafeZone) {
				distanceInfo = Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16
					+ " blocks (capped by normal render distance)";
				boxCuller = null;
			} else {
				distanceInfo = distance + " blocks " + setter;

				if (distance == 0.0 && !hasSafeZone) {
					cullingInfo = "no shadows rendered";
					return holder.setInfo(CULL_EVERYTHING_FRUSTUM, distanceInfo, cullingInfo);
				}

				boxCuller = getOrCreateAdvancedBoxCuller(caches, distance);
			}

			cullingInfo = (hasSafeZone ? "Safe Zone" : "Advanced") + " Frustum Culling enabled";

			final Vector4f shadowLightPosition = celestialUniforms.getShadowLightPositionInWorldSpace();
			shadowLightVectorCache.set(shadowLightPosition.x(), shadowLightPosition.y(), shadowLightPosition.z());
			shadowLightVectorCache.normalize();

			if (hasSafeZone) {
				BoxCuller distanceCuller = new BoxCuller(halfPlaneLength * renderMultiplier);
				SafeZoneCullingFrustum safeZoneFrustum = new SafeZoneCullingFrustum(
					RenderingState.INSTANCE.getModelViewMatrix(), RenderingState.INSTANCE.getProjectionMatrix(),
					shadowLightVectorCache, boxCuller, distanceCuller);
				return holder.setInfo(safeZoneFrustum, distanceInfo, cullingInfo);
			} else {
				caches.advancedFrustum.init(RenderingState.INSTANCE.getModelViewMatrix(), RenderingState.INSTANCE.getProjectionMatrix(), shadowLightVectorCache, boxCuller);
				return holder.setInfo(caches.advancedFrustum, distanceInfo, cullingInfo);
			}
		}

		return holder;
	}

	private static BoxCullingFrustum getOrCreateBoxCullingFrustum(FrustumCaches caches, double distance) {
		if (caches.boxCuller == null) {
			caches.boxCuller = new BoxCuller(distance);
			caches.boxCullingFrustum = new BoxCullingFrustum(caches.boxCuller);
			caches.boxCullerDistance = distance;
		} else if (caches.boxCullerDistance != distance) {
			caches.boxCuller.setMaxDistance(distance);
			caches.boxCullerDistance = distance;
		}
		return caches.boxCullingFrustum;
	}

	private static BoxCuller getOrCreateAdvancedBoxCuller(FrustumCaches caches, double distance) {
		if (caches.advancedBoxCuller == null) {
			caches.advancedBoxCuller = new BoxCuller(distance);
			caches.advancedBoxCullerDistance = distance;
		} else if (caches.advancedBoxCullerDistance != distance) {
			caches.advancedBoxCuller.setMaxDistance(distance);
			caches.advancedBoxCullerDistance = distance;
		}
		return caches.advancedBoxCuller;
	}

	private BoxCuller getOrCreateTileEntityCuller(double distance) {
		if (cachedTileEntityCuller == null) {
			cachedTileEntityCuller = new BoxCuller(distance);
			lastTileEntityCullerDistance = distance;
		} else if (lastTileEntityCullerDistance != distance) {
			cachedTileEntityCuller.setMaxDistance(distance);
			lastTileEntityCullerDistance = distance;
		}
		return cachedTileEntityCuller;
	}

	private void setupGlState(Matrix4f projMatrix) {
		// Bind shadow framebuffer and set viewport to shadow resolution
		targets.getDepthSourceFb().bind();
		GLStateManager.glViewport(0, 0, resolution, resolution);

		// Set up our projection matrix and load it into the legacy matrix stack
		RenderSystem.setupProjectionMatrix(projMatrix);

		// Disable backface culling
		// This partially works around an issue where if the front face of a mountain isn't visible, it casts no
		// shadow.
		//
		// However, it only partially resolves issues of light leaking into caves.
		//
		// TODO: Better way of preventing light from leaking into places where it shouldn't
		GLStateManager.disableCull();
	}

	private void restoreGlState() {
		// Restore backface culling
        GLStateManager.enableCull();

		// Make sure to unload the projection matrix
		RenderSystem.restoreProjectionMatrix();

		// Restore main framebuffer and viewport
		Minecraft mc = Minecraft.getMinecraft();
		mc.getFramebuffer().bindFramebuffer(false);
		GLStateManager.glViewport(0, 0, mc.getFramebuffer().framebufferWidth, mc.getFramebuffer().framebufferHeight);
	}

	private void copyPreTranslucentDepth() {
		profiler.endStartSection("translucent depth copy");

		targets.copyPreTranslucentDepth();
	}

	private static void drawTerrainLayer(CeleritasWorldRenderer celeritas, BlockRenderLayer layer, double x, double y, double z) {
		RenderDevice.enterManagedCode();
		try {
			celeritas.drawChunkLayer(layer, x, y, z);
		} finally {
			RenderDevice.exitManagedCode();
		}
	}

	private void renderEntities(EntityRenderer levelRenderer, ICamera frustum, Object bufferSource, MatrixStack modelView, double cameraX, double cameraY, double cameraZ, float tickDelta) {
		profiler.startSection("cull");

		renderedEntitiesList.clear();

		final boolean playerIsSpectator = GameModeUtil.isSpectator();
		final EntityPlayer player = Minecraft.getMinecraft().player;

		for (Entity entity : getLevel().loadedEntityList) {
			if (playerIsSpectator && entity == player) continue;

			if (!entity.ignoreFrustumCheck && !frustum.isBoundingBoxInFrustum(entity.getEntityBoundingBox())) continue;

			renderedEntitiesList.add(entity);
		}

		profiler.endStartSection("sort");

		renderedEntitiesList.sort(ENTITY_CLASS_COMPARATOR);

		profiler.endStartSection("build geometry");

		setupEntityShadowState(modelView, cameraX, cameraY, cameraZ);
		try {
			final RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
			for (Entity entity : renderedEntitiesList) {
				renderManager.renderEntityStatic(entity, tickDelta, false);
			}
		} finally {
			teardownEntityShadowState();
		}

		renderedShadowEntities = renderedEntitiesList.size();

		profiler.endSection();
	}

	// Saved RenderManager position for shadow pass
    private double savedRenderPosX, savedRenderPosY, savedRenderPosZ;

    private void setupEntityShadowState(MatrixStack modelView, double cameraX, double cameraY, double cameraZ) {
        final RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        savedRenderPosX = renderManager.renderPosX;
        savedRenderPosY = renderManager.renderPosY;
        savedRenderPosZ = renderManager.renderPosZ;

        renderManager.setRenderPosition(cameraX, cameraY, cameraZ);

        GLStateManager.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GLStateManager.glPolygonOffset(1.0f, 1.0f);

        GL11.glPushMatrix();
        MODELVIEW_BUFFER.clear().rewind();
        modelView.peek().getModel().get(MODELVIEW_BUFFER);
        GL11.glLoadMatrixf(MODELVIEW_BUFFER);
    }

    private void teardownEntityShadowState() {
        GL11.glPopMatrix();

        GLStateManager.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GLStateManager.glPolygonOffset(0.0f, 0.0f);

        Minecraft.getMinecraft().getRenderManager().setRenderPosition(savedRenderPosX, savedRenderPosY, savedRenderPosZ);
    }

	private void renderPlayerEntity(EntityRenderer levelRenderer, ICamera frustum, Object bufferSource, MatrixStack modelView, double cameraX, double cameraY, double cameraZ, float tickDelta) {
		profiler.startSection("cull");

		Entity player = Minecraft.getMinecraft().player;

		// Skip if spectating or outside frustum
		if (GameModeUtil.isSpectator()) {
			profiler.endSection();
			renderedShadowEntities = 0;
			return;
		}

		if (!player.ignoreFrustumCheck && !frustum.isBoundingBoxInFrustum(player.getEntityBoundingBox())) {
			profiler.endSection();
			renderedShadowEntities = 0;
			return;
		}

		profiler.endStartSection("build geometry");

		int shadowEntities = 0;

		setupEntityShadowState(modelView, cameraX, cameraY, cameraZ);
		try {
			final RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
			for (Entity passenger : player.getPassengers()) {
				renderManager.renderEntityStatic(passenger, tickDelta, false);
				shadowEntities++;
			}

			if (player.getRidingEntity() != null) {
				renderManager.renderEntityStatic(player.getRidingEntity(), tickDelta, false);
				shadowEntities++;
			}

			renderManager.renderEntityStatic(player, tickDelta, false);
			shadowEntities++;
		} finally {
			teardownEntityShadowState();
		}

		renderedShadowEntities = shadowEntities;

		profiler.endSection();
	}

    private void renderTileEntity(TileEntity tile, double cameraX, double cameraY, double cameraZ, float partialTicks) {
        final BlockPos pos = tile.getPos();
        if (tile.getDistanceSq(cameraX, cameraY, cameraZ) >= tile.getMaxRenderDistanceSquared()) {
            return;
        }
        final int brightness = tile.getWorld().getCombinedLight(pos, 0);
        GLStateManager.setLightmapTextureCoords(GL13.GL_TEXTURE1, (float) (brightness % 65536), (float) (brightness / 65536));
        GLStateManager.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        TileEntityRendererDispatcher.instance.render(tile,
            pos.getX() - cameraX,
            pos.getY() - cameraY,
            pos.getZ() - cameraZ,
            partialTicks, -1, 1.0F
        );
    }

    private static boolean isCulled(BoxCuller culler, TileEntity tileEntity) {
        final BlockPos pos = tileEntity.getPos();
        return culler.isCulled(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

	private void renderTileEntities(Object bufferSource, MatrixStack modelView, double cameraX, double cameraY, double cameraZ, float partialTicks, boolean hasEntityFrustum) {
		profiler.startSection("build blockentities");

		int shadowTileEntities = 0;
		BoxCuller culler = null;
		if (hasEntityFrustum) {
			double distance = halfPlaneLength * (renderDistanceMultiplier * entityShadowDistanceMultiplier);
			culler = getOrCreateTileEntityCuller(distance);
			culler.setPosition(cameraX, cameraY, cameraZ);
		}

        GL11.glPushMatrix();
        MODELVIEW_BUFFER.clear().rewind();
        modelView.peek().getModel().get(MODELVIEW_BUFFER);
        GL11.glLoadMatrixf(MODELVIEW_BUFFER);

        // The shadow pass comes before the world's own block entities, which is where the dispatcher gets this
        // frame's world, textures and camera; in a world's first frame it has none yet
        final Minecraft mc = Minecraft.getMinecraft();
        TileEntityRendererDispatcher.instance.prepare(mc.world, mc.getTextureManager(), mc.fontRenderer, mc.getRenderViewEntity(), mc.objectMouseOver, partialTicks);

        GbufferPrograms.beginBlockEntities();
        GbufferPrograms.setBlockEntityDefaults();
        TileEntityRendererDispatcher.instance.preDrawBatch();
        BlockEntityBatcher.begin();
        try {
            for (TileEntity tileEntity : visibleTileEntities) {
                if (hasEntityFrustum && isCulled(culler, tileEntity)) {
                    continue;
                }
                renderTileEntity(tileEntity, cameraX, cameraY, cameraZ, partialTicks);

                shadowTileEntities++;
            }
            for (TileEntity tileEntity : globalTileEntities) {
                if (hasEntityFrustum && isCulled(culler, tileEntity)) {
                    continue;
                }
                renderTileEntity(tileEntity, cameraX, cameraY, cameraZ, partialTicks);

                shadowTileEntities++;
            }
        } finally {
            BlockEntityBatcher.end();
        }
        TileEntityRendererDispatcher.instance.drawBatch(0);
        GbufferPrograms.endBlockEntities();
        GLStateManager.glPopMatrix();

		renderedShadowTileEntities = shadowTileEntities;

		profiler.endSection();
	}

	public void renderShadows(EntityRenderer levelRenderer, Camera playerCamera) {
        final Minecraft mc = Minecraft.getMinecraft();
        final RenderGlobal rg = mc.renderGlobal;

        // We have to re-query this each frame since this changes based on whether the profiler is active
		// If the profiler is inactive, it will return InactiveProfiler.INSTANCE
		this.profiler = Minecraft.getMinecraft().profiler;

		profiler.endStartSection("shadows");
		ACTIVE = true;
		CURRENT_TARGETS = this.targets;
		final AlphaState alphaBefore = GLStateManager.getAlphaState();

		// NB: We store the previous player buffers in order to be able to allow mods rendering entities in the shadow pass (Flywheel) to use the shadow buffers instead.
        // TODO: Render
//		RenderBuffers playerBuffers = levelRenderer.getRenderBuffers();
//		levelRenderer.setRenderBuffers(buffers);

		visibleTileEntities.clear();
		globalTileEntities.clear();

		// Create our camera
		final MatrixStack modelView = getShadowModelView();
		MODELVIEW.set(modelView.peek().getModel());

        final Matrix4f shadowProjection;
		if (this.fov != null) {
			// If FOV is not null, the pack wants a perspective based projection matrix. (This is to support legacy packs)
            shadowProjection = ShadowMatrices.createPerspectiveMatrix(this.fov);
		} else {
            shadowProjection = ShadowMatrices.createOrthoMatrix(halfPlaneLength);
		}

		PROJECTION.set(shadowProjection);

		profiler.startSection("terrain_setup");

		if (levelRenderer instanceof CullingDataCache) {
			((CullingDataCache) levelRenderer).saveState();
		}

		profiler.startSection("initialize frustum");

		// Devilica: the last frame's shadow search may still be reading the terrain frustum, which is set anew here
		CeleritasWorldRenderer.instance().getRenderSectionManager().finishAllGraphUpdates();

		terrainFrustumHolder = createShadowFrustum(renderDistanceMultiplier, terrainFrustumHolder, terrainFrustumCaches);

		// Use the player/entity position for shadow rendering
		final Vector3d entityPos = playerCamera.getEntityPos();
		final double entityX = entityPos.x;
		final double entityY = entityPos.y;
		final double entityZ = entityPos.z;

		// Center the frustum on the player position
		terrainFrustumHolder.getFrustum().setPosition(entityX, entityY, entityZ);

		profiler.endSection();

		// Always schedule a terrain update
		// TODO: Only schedule a terrain update if the sun / moon is moving, or the shadow map camera moved.
		// We have to ensure that we don't regenerate clouds every frame, since that's what needsUpdate ends up doing.
		// This took up to 10% of the frame time before we applied this fix! That's really bad!
//		boolean regenerateClouds = levelRenderer.shouldRegenerateClouds();
//		((LevelRenderer) levelRenderer).needsUpdate();
//		levelRenderer.setShouldRegenerateClouds(regenerateClouds);

		// Devilica: Celeritas's own shadow search, as Celeritas's modern Iris does it. The player's viewport comes from a
		// frustum on the camera's clipping helper, which renderWorldPass has already set up when shadows render.
		final CeleritasWorldRenderer celeritas = CeleritasWorldRenderer.instance();
		final Frustum playerFrustum = new Frustum();
		playerFrustum.setPosition(entityX, entityY, entityZ);
		final Viewport playerViewport = ((ViewportProvider) playerFrustum).sodium$createViewport();
		celeritas.setChunkRenderMatricesOverride(PROJECTION, MODELVIEW);
		celeritas.getRenderSectionManager().markGraphDirty();

		RenderDevice.enterManagedCode();
		try {
			celeritas.setupShadowTerrain(playerViewport,
				((ViewportProvider) terrainFrustumHolder.getFrustum()).sodium$createViewport(),
				CeleritasWorldRenderer.captureCameraState(playerCamera.getPartialTicks()),
				shadowFrame++, GameModeUtil.isSpectator());
		} finally {
			RenderDevice.exitManagedCode();
		}
		celeritas.forEachVisibleBlockEntity(visibleTileEntities::add);

		profiler.endStartSection("terrain");

		setupGlState(PROJECTION);

		// Render all opaque terrain unless pack requests not to
		if (shouldRenderTerrain) {
            mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            drawTerrainLayer(celeritas, BlockRenderLayer.SOLID, entityX, entityY, entityZ);
            drawTerrainLayer(celeritas, BlockRenderLayer.CUTOUT_MIPPED, entityX, entityY, entityZ);
            drawTerrainLayer(celeritas, BlockRenderLayer.CUTOUT, entityX, entityY, entityZ);
            // Devilica: Celeritas unbinds its terrain program, and entities drawn without the pack's shadow program
            // miss its distortion, so they land small and off place in the shadow map
            rebindShadowProgram.run();
		}

		// Reset viewport in case terrain rendering changed it
		GLStateManager.glViewport(0, 0, resolution, resolution);
		// Devilica: terrain leaves texture unit 1 active, where entities would then bind their textures while the pack
		// samples unit 0 (the block atlas), dropping whatever lands on clear atlas space (a player's left arm and leg)
		GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);

		profiler.endStartSection("entities");

		// Get the current tick delta. Normally this is the same as client.getTickDelta(), but when the game is paused,
		// it is set to a fixed value.
		final float tickDelta = CapturedRenderingState.INSTANCE.getTickDelta();

		// Create a constrained shadow frustum for entities to avoid rendering faraway entities in the shadow pass,
		// if the shader pack has requested it. Otherwise, use the same frustum as for terrain.
		boolean hasEntityFrustum = false;

		if (entityShadowDistanceMultiplier == 1.0F || entityShadowDistanceMultiplier < 0.0F) {
			entityFrustumHolder.setInfo(terrainFrustumHolder.getFrustum(), terrainFrustumHolder.getDistanceInfo(), terrainFrustumHolder.getCullingInfo());
		} else {
			hasEntityFrustum = true;
			entityFrustumHolder = createShadowFrustum(renderDistanceMultiplier * entityShadowDistanceMultiplier, entityFrustumHolder, entityFrustumCaches);
		}

		final ICamera entityShadowFrustum = entityFrustumHolder.getFrustum();
		entityShadowFrustum.setPosition(entityX, entityY, entityZ);

		// Render nearby entities

		if (shouldRenderEntities) {
			renderEntities(levelRenderer, entityShadowFrustum, null, modelView, entityX, entityY, entityZ, tickDelta);
		} else if (shouldRenderPlayer) {
			renderPlayerEntity(levelRenderer, entityShadowFrustum, null, modelView, entityX, entityY, entityZ, tickDelta);
		}

		if (shouldRenderBlockEntities) {
			renderTileEntities(null, modelView, entityX, entityY, entityZ, tickDelta, hasEntityFrustum);
		}

		profiler.endStartSection("draw entities");

		// NB: Don't try to draw the translucent parts of entities afterwards. It'll cause problems since some
		// shader packs assume that everything drawn afterwards is actually translucent and should cast a colored
		// shadow...

		copyPreTranslucentDepth();

		profiler.endStartSection("translucent terrain");

		// TODO (Iris): Prevent these calls from scheduling translucent sorting...
		// It doesn't matter a ton, since this just means that they won't be sorted in the getNormal rendering pass.
		// Just something to watch out for, however...
		if (shouldRenderTranslucent) {
            // Devilica: the alpha test the main pass draws translucents with; the frame's 0.5 dropped stained glass from
            // the shadow map unless a held item had lowered it while the player was drawn
            GLStateManager.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            drawTerrainLayer(celeritas, BlockRenderLayer.TRANSLUCENT, entityX, entityY, entityZ);
		}
		celeritas.setChunkRenderMatricesOverride(null, null);

		// Note: Apparently tripwire isn't rendered in the shadow pass.
		// worldRenderer.invokeRenderType(RenderType.getTripwire(), modelView, cameraX, cameraY, cameraZ);

//		if (renderBuffersExt != null) {
//			renderBuffersExt.endLevelRendering();
//		}

		profiler.endStartSection("generate mipmaps");

		generateMipmaps();

		profiler.endStartSection("restore gl state");

		restoreGlState();
		// Devilica: entities (a held item) and the translucent pass change the alpha test; the main pass expects its own
		GLStateManager.glAlphaFunc(alphaBefore.getFunction(), alphaBefore.getReference());

		if (levelRenderer instanceof CullingDataCache) {
			((CullingDataCache) levelRenderer).restoreState();
		}

		profiler.endStartSection("shadowcomp");

		if (compositeRenderer != null) compositeRenderer.renderAll();

		ACTIVE = false;
		CURRENT_TARGETS = null;
		profiler.endSection();
		profiler.endStartSection("updatechunks");
	}

	public void addDebugText(List<String> messages) {
		messages.add("[" + Iris.MODNAME + " - Shadow Pass]");
		messages.add("  Shadow Maps: " + debugStringOverall);
		messages.add("  Shadow Distance Terrain: " + terrainFrustumHolder.getDistanceInfo() + " Entity: " + entityFrustumHolder.getDistanceInfo());
		messages.add("  Shadow Culling Terrain: " + terrainFrustumHolder.getCullingInfo() + " Entity: " + entityFrustumHolder.getCullingInfo());
		messages.add("  Shadow Terrain: " + CeleritasWorldRenderer.instance().getChunksDebugString() + (shouldRenderTerrain ? "" : " (no terrain) ") + (shouldRenderTranslucent ? "" : "(no translucent)"));
		messages.add("  Shadow Entities: " + getEntitiesDebugString());
		messages.add("  Shadow Block Entities: " + getTileEntitiesDebugString());

//		if (buffers instanceof DrawCallTrackingRenderBuffers drawCallTracker && (shouldRenderEntities || shouldRenderPlayer)) {
//            messages.add("[" + Iris.MODNAME + "] Shadow Entity Batching: " + BatchingDebugMessageHelper.getDebugMessage(drawCallTracker));
//		}
	}

	private String getEntitiesDebugString() {
		return (shouldRenderEntities || shouldRenderPlayer) ? (renderedShadowEntities + "/" + Minecraft.getMinecraft().world.loadedEntityList.size()) : "disabled by pack";
	}

	private String getTileEntitiesDebugString() {
		return shouldRenderBlockEntities ? (renderedShadowTileEntities + "/" + Minecraft.getMinecraft().world.loadedTileEntityList.size()) : "disabled by pack";
	}

	private static class MipmapPass {
		private final int texture;
		private final int targetFilteringMode;

		public MipmapPass(int texture, int targetFilteringMode) {
			this.texture = texture;
			this.targetFilteringMode = targetFilteringMode;
		}

		public int getTexture() {
			return texture;
		}

		public int getTargetFilteringMode() {
			return targetFilteringMode;
		}
	}
}
