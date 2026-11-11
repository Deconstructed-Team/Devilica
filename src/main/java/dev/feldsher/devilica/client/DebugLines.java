/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.client;

import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.cit.CustomItemTextures;
import dev.feldsher.devilica.colors.CustomColors;
import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.ctm.ConnectedTextures;
import dev.feldsher.devilica.dynamiclights.DynamicLights;
import dev.feldsher.devilica.randomentities.RandomEntities;
import dev.feldsher.devilica.sky.CustomSky;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Devilica's lines on the F3 screen: its version and what its features found. */
public final class DebugLines {

    public static final DebugLines INSTANCE = new DebugLines();

    private DebugLines() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onDebugText(RenderGameOverlayEvent.Text event) {
        if (!Minecraft.getMinecraft().gameSettings.showDebugInfo) {
            return;
        }
        final List<String> right = event.getRight();
        final DevilicaOptions options = DevilicaOptions.get();
        final List<String> lines = new ArrayList<>();
        lines.add("Devilica " + Devilica.VERSION);
        lines.add("Dynamic lights: " + (DynamicLights.isEnabled() ? DynamicLights.lightCount() + " lit, " + options.dynamicLights.mode.name().toLowerCase(Locale.ROOT) : "off"));
        lines.add("Connected textures: " + (options.connectedTextures ? ConnectedTextures.ruleCount() + " rules" : "off"));
        lines.add("Random entities: " + (options.randomEntities ? RandomEntities.textureCount() + " textures" : "off"));
        lines.add("Custom sky: " + (options.customSky ? CustomSky.worldCount() + " worlds" : "off"));
        lines.add("Custom item textures: " + (options.customItemTextures ? CustomItemTextures.itemCount() + " items" : "off"));
        lines.add("Custom colors: " + (options.customColors ? CustomColors.count() + " set" : "off"));
        lines.add("");
        // Above the terrain renderer's long list, which runs off small screens
        int at = right.size();
        for (int i = 0; i < right.size(); i++) {
            if (right.get(i).contains("Celeritas Renderer")) {
                at = i;
                break;
            }
        }
        right.addAll(at, lines);
    }
}
