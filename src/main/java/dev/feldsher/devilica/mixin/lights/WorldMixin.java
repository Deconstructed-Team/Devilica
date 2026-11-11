/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.lights;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.feldsher.devilica.dynamiclights.DynamicLights;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In the client world, entities, block entities, particles and the held item take their light through
 * {@code getCombinedLight}, so they are lit by dynamic lights there; an entity that leaves takes its light along.
 */
@Mixin(World.class)
public abstract class WorldMixin {

    @Shadow @Final
    public boolean isRemote;

    @ModifyReturnValue(method = "getCombinedLight", at = @At("RETURN"))
    private int devilica$dynamicLight(int packedLight, BlockPos pos, int lightValue) {
        return this.isRemote ? DynamicLights.withDynamicLight(DynamicLights.current(), pos.getX(), pos.getY(), pos.getZ(), packedLight) : packedLight;
    }

    @Inject(method = "onEntityRemoved", at = @At("HEAD"))
    private void devilica$removeLight(Entity entity, CallbackInfo ci) {
        if (this.isRemote) {
            DynamicLights.remove(entity);
        }
    }
}
