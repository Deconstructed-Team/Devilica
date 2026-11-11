/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.lights;

import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.dynamiclights.PrimedLight;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.monster.EntityCreeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** A swelling creeper glows at level 10, or up to it with its flashing. */
@Mixin(EntityCreeper.class)
public abstract class EntityCreeperMixin implements PrimedLight {

    @Shadow
    public abstract float getCreeperFlashIntensity(float partialTicks);

    @Override
    public int devilica$getPrimedLuminance() {
        final float flash = this.getCreeperFlashIntensity(Minecraft.getMinecraft().getRenderPartialTicks());
        if (flash <= 0.001F) {
            return 0;
        }
        return switch (DevilicaOptions.get().dynamicLights.creepers) {
            case OFF -> 0;
            case SIMPLE -> 10;
            case FANCY -> (int) (flash * 10.0F);
        };
    }
}
