/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica;

import dev.feldsher.devilica.cit.CustomItemTextures;
import dev.feldsher.devilica.client.DebugLines;
import dev.feldsher.devilica.colors.CustomColors;
import dev.feldsher.devilica.ctm.ConnectedTextures;
import dev.feldsher.devilica.hud.InfoOverlay;
import dev.feldsher.devilica.randomentities.RandomEntities;
import dev.feldsher.devilica.sky.CustomSky;
import dev.feldsher.devilica.zoom.Zoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.coderbot.iris.Iris;
import net.coderbot.iris.client.IrisDebugScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import org.taumc.celeritas.CeleritasVintage;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = Devilica.MOD_ID, name = Devilica.MOD_NAME, version = Devilica.VERSION, clientSideOnly = true, acceptableRemoteVersions = "*")
public class Devilica {

    public static final String MOD_ID = "@{MOD_ID}";
    public static final String MOD_NAME = "@{MOD_NAME}";
    public static final String VERSION = "@{MOD_VERSION}";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    private final CeleritasVintage terrain = new CeleritasVintage();

    @Mod.EventHandler
    public void construct(FMLConstructionEvent event) {
        terrain.onConstruct(event);
        // The block atlas is first stitched while the game starts, before mods initialise
        MinecraftForge.EVENT_BUS.register(ConnectedTextures.INSTANCE);
        MinecraftForge.EVENT_BUS.register(CustomItemTextures.INSTANCE);
        MinecraftForge.EVENT_BUS.register(CustomColors.INSTANCE);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        terrain.onInit(event);

        // Iris: its key bindings, key handling and F3 lines
        Iris.INSTANCE.fmlInitEvent();
        MinecraftForge.EVENT_BUS.register(Iris.INSTANCE);
        MinecraftForge.EVENT_BUS.register(IrisDebugScreenHandler.INSTANCE);

        Zoom.init();
        MinecraftForge.EVENT_BUS.register(DebugLines.INSTANCE);
        MinecraftForge.EVENT_BUS.register(InfoOverlay.INSTANCE);
        final IReloadableResourceManager resources = (IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager();
        resources.registerReloadListener(RandomEntities.INSTANCE);
        resources.registerReloadListener(CustomSky.INSTANCE);
    }

}
