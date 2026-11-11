/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.lights;

import dev.feldsher.devilica.dynamiclights.DynamicLightSource;
import dev.feldsher.devilica.dynamiclights.DynamicLights;
import dev.feldsher.devilica.dynamiclights.LightTracker;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every client entity checks each tick how much light it gives off. */
@Mixin(Entity.class)
public abstract class EntityMixin implements DynamicLightSource {

    @Shadow
    public World world;

    @Unique
    private LightTracker devilica$lightTracker;

    @Override
    public LightTracker devilica$getLightTracker() {
        return this.devilica$lightTracker;
    }

    @Override
    public LightTracker devilica$getOrCreateLightTracker() {
        if (this.devilica$lightTracker == null) {
            this.devilica$lightTracker = new LightTracker();
        }
        return this.devilica$lightTracker;
    }

    @Inject(method = "onEntityUpdate", at = @At("TAIL"))
    private void devilica$tickLight(CallbackInfo ci) {
        if (this.world != null && this.world.isRemote) {
            DynamicLights.tick((Entity) (Object) this);
        }
    }
}
