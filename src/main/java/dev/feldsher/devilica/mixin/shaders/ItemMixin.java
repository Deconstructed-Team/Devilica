/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import net.irisshaders.iris.api.v0.item.IrisItemLightProvider;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;

/** Every item gives off the light of its block for the held light uniforms, unless it overrides it. */
@Mixin(Item.class)
public abstract class ItemMixin implements IrisItemLightProvider {
}
