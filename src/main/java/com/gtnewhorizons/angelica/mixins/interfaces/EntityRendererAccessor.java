package com.gtnewhorizons.angelica.mixins.interfaces;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.EntityLivingBase;

public interface EntityRendererAccessor {
    float invokeGetNightVisionBrightness(EntityLivingBase entity, float partialTicks);

    /**
     * Get the lightmap texture for shader binding.
     */
    DynamicTexture getLightmapTexture();
}
