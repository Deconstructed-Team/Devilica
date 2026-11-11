/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.gtnewhorizons.angelica.glsm.texture.TextureTracker;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Iris tracks which texture object each GL texture belongs to (for {@code atlasSize} and PBR atlases). Every
 * {@code TextureUtil.allocateTexture} deletes the name it was given first, which untracks the texture that was just
 * tracked; so a texture is tracked again whenever its ID is asked for and the tracker has lost or reassigned it.
 */
@Mixin(AbstractTexture.class)
public abstract class AbstractTextureMixin {

    @Inject(method = "getGlTextureId", at = @At("RETURN"))
    private void devilica$trackTexture(CallbackInfoReturnable<Integer> cir) {
        final int id = cir.getReturnValueI();
        if (id != -1 && TextureTracker.INSTANCE.getTexture(id) != (Object) this) {
            TextureTracker.INSTANCE.trackTexture(id, (AbstractTexture) (Object) this);
        }
    }
}
