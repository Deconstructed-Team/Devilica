/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.lights;

import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.dynamiclights.DynamicLights;
import dev.feldsher.devilica.dynamiclights.PrimedLight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lit TNT glows at level 10, or up to it as its fuse burns; its tick does not run the base entity tick. */
@Mixin(EntityTNTPrimed.class)
public abstract class EntityTNTPrimedMixin implements PrimedLight {

    @Shadow
    public abstract int getFuse();

    /** The longest fuse seen, as the fuse it was lit with (the server sends it after the entity is made) */
    @Unique
    private int devilica$litFuse = 1;

    @Override
    public int devilica$getPrimedLuminance() {
        final int fuse = this.getFuse();
        this.devilica$litFuse = Math.max(this.devilica$litFuse, fuse);
        return switch (DevilicaOptions.get().dynamicLights.tnt) {
            case OFF -> 0;
            case SIMPLE -> 10;
            case FANCY -> {
                final float left = Math.max(0.0F, (float) fuse / this.devilica$litFuse);
                yield (int) (10.0F - left * left * 10.0F);
            }
        };
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void devilica$tickLight(CallbackInfo ci) {
        final Entity self = (Entity) (Object) this;
        if (self.world.isRemote) {
            DynamicLights.tick(self);
        }
    }
}
