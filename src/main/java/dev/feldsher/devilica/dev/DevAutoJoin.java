/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.dev;

import java.io.File;

import dev.feldsher.devilica.Devilica;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Dev runs only: with {@code -Ddevilica.autojoin=<world folder>} the client opens that singleplayer world the first
 * time the main menu shows, creating a creative one with a fixed seed if it is missing, so a client driven by tools
 * is in a world without anyone clicking through the menus.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Devilica.MOD_ID)
public final class DevAutoJoin {

    private static final long SEED = 1122L;

    private static boolean done;

    private DevAutoJoin() {
    }

    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        String world = System.getProperty("devilica.autojoin");
        if (done || world == null || world.isEmpty() || !(event.getGui() instanceof GuiMainMenu)) {
            return;
        }
        done = true;
        Minecraft mc = Minecraft.getMinecraft();
        boolean exists = new File(mc.gameDir, "saves/" + world + "/level.dat").isFile();
        WorldSettings settings = exists ? null
            : new WorldSettings(SEED, GameType.CREATIVE, true, false, WorldType.DEFAULT).enableCommands();
        mc.addScheduledTask(() -> mc.launchIntegratedServer(world, world, settings));
    }
}
