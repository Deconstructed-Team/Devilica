/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Animated textures stop where the animation settings switch them off (they keep their current frame). */
@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteAnimationMixin {

    @Unique
    private static final byte devilica$OTHER = 1;
    @Unique
    private static final byte devilica$WATER = 2;
    @Unique
    private static final byte devilica$LAVA = 3;
    @Unique
    private static final byte devilica$FIRE = 4;
    @Unique
    private static final byte devilica$PORTAL = 5;

    @Shadow
    public abstract String getIconName();

    @Unique
    private byte devilica$kind;

    @Inject(method = "updateAnimation", at = @At("HEAD"), cancellable = true)
    private void devilica$animationSetting(CallbackInfo ci) {
        byte kind = this.devilica$kind;
        if (kind == 0) {
            this.devilica$kind = kind = devilica$classify(this.getIconName());
        }
        final DevilicaOptions.AnimationSettings settings = DevilicaOptions.get().animations;
        final boolean animate = switch (kind) {
            case devilica$WATER -> settings.water;
            case devilica$LAVA -> settings.lava;
            case devilica$FIRE -> settings.fire;
            case devilica$PORTAL -> settings.portal;
            default -> settings.textures;
        };
        if (!animate) {
            ci.cancel();
        }
    }

    @Unique
    private static byte devilica$classify(String name) {
        if (name == null) {
            return devilica$OTHER;
        }
        if (name.equals("minecraft:blocks/water_still") || name.equals("minecraft:blocks/water_flow")) {
            return devilica$WATER;
        }
        if (name.equals("minecraft:blocks/lava_still") || name.equals("minecraft:blocks/lava_flow")) {
            return devilica$LAVA;
        }
        if (name.startsWith("minecraft:blocks/fire_layer_")) {
            return devilica$FIRE;
        }
        if (name.equals("minecraft:blocks/portal")) {
            return devilica$PORTAL;
        }
        return devilica$OTHER;
    }
}
