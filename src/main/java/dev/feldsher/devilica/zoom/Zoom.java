/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.zoom;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * OptiFine's zoom: hold the key to look through a spyglass, the mouse wheel changes how far. Angelica's version, with a
 * glide in and out and the cinematic camera while zoomed; from Zume, the key as a toggle, keys to zoom in and out, a
 * floor for the mouse sensitivity, and in third person the camera moving closer or further instead.
 */
public final class Zoom {

    public static final String KEY_CATEGORY = "key.categories.devilica";

    public static final float ZOOM_MIN = 1.0F;
    public static final float ZOOM_MAX = 64.0F;
    public static final float ZOOM_STEP = 1.2F;
    public static final float THIRD_PERSON_MIN_DISTANCE = 0.5F;
    /** The vanilla third-person camera distance, which a third-person zoom of 1 keeps */
    private static final float THIRD_PERSON_DISTANCE = 4.0F;
    /** Zoom steps a second while a zoom in or out key is held */
    private static final float KEY_STEPS_PER_SECOND = 6.0F;
    private static final float SNAP_EPSILON = 0.001F;

    public static final KeyBinding KEY = new KeyBinding("devilica.key.zoom", Keyboard.KEY_Z, KEY_CATEGORY);
    public static final KeyBinding KEY_IN = new KeyBinding("devilica.key.zoom_in", Keyboard.KEY_NONE, KEY_CATEGORY);
    public static final KeyBinding KEY_OUT = new KeyBinding("devilica.key.zoom_out", Keyboard.KEY_NONE, KEY_CATEGORY);

    /** How far the zoom goes while it is on */
    private static float zoom = 1.0F;
    /** Where the glide is now, 1 when not zoomed */
    private static float currentZoom = 1.0F;
    private static long lastFrameNanos;
    private static boolean zoomedIn;
    /** Whether the zoom moves the third-person camera instead of narrowing the view */
    private static boolean thirdPerson;

    private Zoom() {
    }

    public static void init() {
        ClientRegistry.registerKeyBinding(KEY);
        ClientRegistry.registerKeyBinding(KEY_IN);
        ClientRegistry.registerKeyBinding(KEY_OUT);
        MinecraftForge.EVENT_BUS.register(new Zoom());
    }

    public static boolean isZoomedIn() {
        return zoomedIn;
    }

    /** How much the view is narrowed now, 1 when it is not */
    public static float viewZoom() {
        return thirdPerson ? 1.0F : currentZoom;
    }

    /** The third-person camera's distance, from the one vanilla would keep */
    public static double cameraDistance(double distance) {
        if (!thirdPerson || currentZoom == 1.0F) {
            return distance;
        }
        return MathHelper.clamp(distance / currentZoom, THIRD_PERSON_MIN_DISTANCE, Math.max(THIRD_PERSON_MIN_DISTANCE, DevilicaOptions.get().zoom.thirdPersonMax));
    }

    /** What the mouse sensitivity is multiplied by now */
    public static float sensitivityFactor() {
        if (!zoomedIn || thirdPerson) {
            return 1.0F;
        }
        final float floor = DevilicaOptions.get().zoom.sensitivityFloor / 100.0F;
        return Math.max(floor, 1.0F / (1.0F + 0.2F * (zoom - ZOOM_MIN)));
    }

    /** Whether the mouse wheel zooms now instead of changing the hotbar slot */
    public static boolean scrolls() {
        return zoomedIn && DevilicaOptions.get().zoom.scroll;
    }

    public static boolean useCinematicCamera() {
        return zoomedIn && !thirdPerson && DevilicaOptions.get().zoom.cinematic;
    }

    public static void scroll(int wheel) {
        if (wheel != 0) {
            setZoom((float) (zoom * Math.pow(ZOOM_STEP, Integer.signum(wheel))));
        }
    }

    private static void setZoom(float value) {
        if (thirdPerson) {
            zoom = MathHelper.clamp(value, THIRD_PERSON_DISTANCE / Math.max(1, DevilicaOptions.get().zoom.thirdPersonMax), THIRD_PERSON_DISTANCE / THIRD_PERSON_MIN_DISTANCE);
        } else {
            zoom = MathHelper.clamp(value, ZOOM_MIN, ZOOM_MAX);
        }
    }

    private static boolean wantsThirdPerson() {
        return Minecraft.getMinecraft().gameSettings.thirdPersonView != 0 && DevilicaOptions.get().zoom.thirdPersonMax > 0;
    }

    private static boolean toggles() {
        final DevilicaOptions.ZoomSettings settings = DevilicaOptions.get().zoom;
        return Minecraft.getMinecraft().gameSettings.thirdPersonView != 0 ? settings.thirdPersonToggle : settings.toggle;
    }

    private static void start() {
        zoomedIn = true;
        thirdPerson = wantsThirdPerson();
        // The third-person camera starts where it is, and the wheel or the keys move it
        setZoom(thirdPerson ? 1.0F : DevilicaOptions.get().zoom.defaultZoom);
    }

    private static void stop() {
        zoomedIn = false;
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer != null) {
            // The cinematic camera's filters would keep drifting after it is turned off
            mc.entityRenderer.mouseFilterXAxis.reset();
            mc.entityRenderer.mouseFilterYAxis.reset();
        }
    }

    private static void checkKey() {
        if (KEY.getKeyCode() == 0 || Minecraft.getMinecraft().currentScreen != null) {
            return;
        }
        if (toggles()) {
            while (KEY.isPressed()) {
                if (zoomedIn) {
                    stop();
                } else {
                    start();
                }
            }
            return;
        }
        while (KEY.isPressed()) {
            // Dropped: held, the key's state is what counts, and old presses would toggle once the mode changes
        }
        if (KEY.isKeyDown()) {
            if (!zoomedIn) {
                start();
            }
        } else if (zoomedIn) {
            stop();
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        checkKey();
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        // Fired for mouse movement too; only buttons can be the key
        if (Mouse.getEventButton() >= 0) {
            checkKey();
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        final long now = System.nanoTime();
        // A long frame (a pause, the window in the background) should not snap the zoom
        final float dt = lastFrameNanos == 0L ? 0.0F : Math.min((now - lastFrameNanos) / 1.0e9F, 0.1F);
        lastFrameNanos = now;

        // Released while a screen was open, or the view switched between first and third person
        if (zoomedIn && (!toggles() && !KEY.isKeyDown() || thirdPerson != wantsThirdPerson())) {
            stop();
        }
        if (zoomedIn && Minecraft.getMinecraft().currentScreen == null) {
            final int direction = (KEY_IN.isKeyDown() ? 1 : 0) - (KEY_OUT.isKeyDown() ? 1 : 0);
            if (direction != 0) {
                setZoom((float) (zoom * Math.pow(ZOOM_STEP, direction * dt * KEY_STEPS_PER_SECOND)));
            }
        }
        updateGlide(dt);
    }

    private static void updateGlide(float dt) {
        final float target = zoomedIn ? zoom : 1.0F;
        if (dt <= 0.0F || !DevilicaOptions.get().zoom.smooth) {
            currentZoom = target;
            return;
        }
        currentZoom += (target - currentZoom) * (1.0F - (float) Math.exp(-dt * DevilicaOptions.get().zoom.smoothSpeed));
        if (Math.abs(target - currentZoom) < SNAP_EPSILON) {
            currentZoom = target;
        }
    }
}
