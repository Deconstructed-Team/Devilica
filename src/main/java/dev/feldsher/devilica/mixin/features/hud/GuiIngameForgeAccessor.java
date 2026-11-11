/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.hud;

import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GuiIngameForge.class, remap = false)
public interface GuiIngameForgeAccessor {

    @Invoker("renderCrosshairs")
    void devilica$renderCrosshairs(float partialTicks);
}
