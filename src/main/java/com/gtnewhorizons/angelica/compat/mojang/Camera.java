package com.gtnewhorizons.angelica.compat.mojang;

import net.minecraft.util.math.BlockPos;
import com.gtnewhorizons.angelica.rendering.RenderingState;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * Singleton camera instance that tracks the current camera position and orientation.
 */
@Getter
public class Camera {
    public static final Camera INSTANCE = new Camera();

    private final Vector3d pos = new Vector3d();
    private final Vector3d entityPos = new Vector3d();
    private final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
    private final Vector3f offset = new Vector3f();
    private final Matrix4f inverseModelView = new Matrix4f();

    private float pitch;
    private float yaw;
    private Entity entity;
    private boolean thirdPerson;
    private float partialTicks;

    private Camera() {
    }

    /**
     * Updates the camera position and orientation based on the current entity and partial ticks.
     *
     * @param entity The entity to track (usually the render view entity)
     * @param partialTicks The partial tick time for interpolation
     */
    public void update(Entity entity, float partialTicks) {
        this.partialTicks = partialTicks;
        this.entity = entity;

        // Entity position (interpolated, at its feet). 1.12 draws the world relative to the feet and puts the eye height
        // into the modelview (orientCamera), so this is the origin gbufferModelView works from: cameraPosition and the
        // shadow pass must use it too, or shadows are looked up an eye height above where they were drawn.
        final double entityX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        final double entityY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        final double entityZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        final double eyeHeight = entity.getEyeHeight();

        entityPos.set(entityX, entityY, entityZ);

        thirdPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView > 0;

        final double camX, camY, camZ;
        offset.set(0, 0, 0);

        if (thirdPerson) {
            // Third person: use inverse modelview to find camera offset
            inverseModelView.set(RenderingState.INSTANCE.getModelViewMatrix()).invert();
            inverseModelView.transformPosition(offset);
        }
        camX = entityX + offset.x;
        camY = entityY + eyeHeight + offset.y;
        camZ = entityZ + offset.z;

        pos.set(camX, camY, camZ);
        blockPos.setPos(MathHelper.floor(camX), MathHelper.floor(camY), MathHelper.floor(camZ));
        pitch = entity.rotationPitch;
        yaw = entity.rotationYaw;
    }
}
