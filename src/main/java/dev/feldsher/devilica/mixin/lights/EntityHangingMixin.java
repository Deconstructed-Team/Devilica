/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.lights;

import dev.feldsher.devilica.dynamiclights.DynamicLights;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item frames give off their item's light; hanging entities skip the update other entities light from. */
@Mixin(EntityHanging.class)
public abstract class EntityHangingMixin {

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void devilica$tickLight(CallbackInfo ci) {
        final Entity self = (Entity) (Object) this;
        if (self.world.isRemote) {
            DynamicLights.tick(self);
        }
    }
}
