/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.config;

import com.google.common.collect.ImmutableList;
import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.client.ParticleShares;
import dev.feldsher.devilica.colors.CustomColors;
import dev.feldsher.devilica.ctm.BetterGrassMode;
import dev.feldsher.devilica.dynamiclights.DynamicLights;
import dev.feldsher.devilica.dynamiclights.DynamicLightsMode;
import dev.feldsher.devilica.dynamiclights.ExplosiveLightMode;
import dev.feldsher.devilica.hud.AdaptiveVsync;
import dev.feldsher.devilica.hud.OverlayCorner;
import dev.feldsher.devilica.hud.TextContrast;
import dev.feldsher.devilica.mixin.features.RenderGlobalAccessor;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.structure.OptionFlag;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpact;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionPage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Devilica's page in the video settings. */
public final class DevilicaOptionPages {

    public static final OptionIdentifier<Void> PAGE = OptionIdentifier.create(Devilica.MOD_ID, "devilica");
    public static final OptionIdentifier<Void> DYNAMIC_LIGHTS = OptionIdentifier.create(Devilica.MOD_ID, "dynamic_lights_group");
    public static final OptionIdentifier<Void> ZOOM = OptionIdentifier.create(Devilica.MOD_ID, "zoom_group");
    public static final OptionIdentifier<Void> TEXTURES = OptionIdentifier.create(Devilica.MOD_ID, "textures_group");
    public static final OptionIdentifier<Void> HUD = OptionIdentifier.create(Devilica.MOD_ID, "hud_group");
    public static final OptionIdentifier<Void> SHADERS = OptionIdentifier.create(Devilica.MOD_ID, "shaders_group");
    public static final OptionIdentifier<Void> DETAILS_PAGE = OptionIdentifier.create(Devilica.MOD_ID, "details");
    public static final OptionIdentifier<Void> SKY = OptionIdentifier.create(Devilica.MOD_ID, "sky_group");
    public static final OptionIdentifier<Void> WEATHER = OptionIdentifier.create(Devilica.MOD_ID, "weather_group");
    public static final OptionIdentifier<Void> COLORS = OptionIdentifier.create(Devilica.MOD_ID, "colors_group");
    public static final OptionIdentifier<Void> PARTICLE_CLASSES_PAGE = OptionIdentifier.create(Devilica.MOD_ID, "particle_classes");
    public static final OptionIdentifier<Void> PARTICLE_CLASSES = OptionIdentifier.create(Devilica.MOD_ID, "particle_classes_group");
    public static final OptionIdentifier<Void> LIGHT_SOURCES_PAGE = OptionIdentifier.create(Devilica.MOD_ID, "light_sources");
    public static final OptionIdentifier<Void> LIGHT_SOURCES = OptionIdentifier.create(Devilica.MOD_ID, "light_sources_group");
    public static final OptionIdentifier<Void> RENDER_PAGE = OptionIdentifier.create(Devilica.MOD_ID, "render");
    public static final OptionIdentifier<Void> RENDER_ENTITIES = OptionIdentifier.create(Devilica.MOD_ID, "render_entities_group");
    public static final OptionIdentifier<Void> RENDER_BLOCK_ENTITIES = OptionIdentifier.create(Devilica.MOD_ID, "render_block_entities_group");
    public static final OptionIdentifier<Void> OVERLAY_PAGE = OptionIdentifier.create(Devilica.MOD_ID, "overlay");
    public static final OptionIdentifier<Void> OVERLAY_INFO = OptionIdentifier.create(Devilica.MOD_ID, "overlay_info_group");
    public static final OptionIdentifier<Void> OVERLAY_SCREEN = OptionIdentifier.create(Devilica.MOD_ID, "overlay_screen_group");
    public static final OptionIdentifier<Void> OVERLAY_TOASTS = OptionIdentifier.create(Devilica.MOD_ID, "overlay_toasts_group");
    public static final OptionIdentifier<Void> ANIMATIONS_PAGE = OptionIdentifier.create(Devilica.MOD_ID, "animations");
    public static final OptionIdentifier<Void> TEXTURE_ANIMATIONS = OptionIdentifier.create(Devilica.MOD_ID, "texture_animations");
    public static final OptionIdentifier<Void> PARTICLES = OptionIdentifier.create(Devilica.MOD_ID, "particles_group");

    private DevilicaOptionPages() {
    }

    public static OptionPage details() {
        final DevilicaOptions options = DevilicaOptions.get();

        final OptionGroup sky = OptionGroup.createBuilder()
                .setId(SKY)
                .add(toggle(options, "sky", (o, v) -> o.details.sky = v, o -> o.details.sky))
                .add(toggle(options, "sun", (o, v) -> o.details.sun = v, o -> o.details.sun))
                .add(toggle(options, "moon", (o, v) -> o.details.moon = v, o -> o.details.moon))
                .add(toggle(options, "stars", (o, v) -> o.details.stars = v, o -> o.details.stars))
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "star_count", int.class))
                        .setName(TextComponent.translatable("devilica.options.star_count"))
                        .setTooltip(TextComponent.translatable("devilica.options.star_count.tooltip"))
                        .setControl(option -> new SliderControl(option, 500, 32000, 500, ControlValueFormatter.number()))
                        .setBinding((opts, value) -> {
                            if (opts.details.starCount != value) {
                                opts.details.starCount = value;
                                if (Minecraft.getMinecraft().renderGlobal != null) {
                                    ((RenderGlobalAccessor) Minecraft.getMinecraft().renderGlobal).devilica$generateStars();
                                }
                            }
                        }, opts -> opts.details.starCount)
                        .build())
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "cloud_height", int.class))
                        .setName(TextComponent.translatable("devilica.options.cloud_height"))
                        .setTooltip(TextComponent.translatable("devilica.options.cloud_height.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 128, 8, ControlValueFormatter.number()))
                        .setBinding((opts, value) -> opts.details.cloudHeight = value, opts -> opts.details.cloudHeight)
                        .build())
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "cloud_distance", int.class))
                        .setName(TextComponent.translatable("devilica.options.cloud_distance"))
                        .setTooltip(TextComponent.translatable("devilica.options.cloud_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 64, 2, chunksOrDefault()))
                        .setBinding((opts, value) -> opts.details.cloudDistance = value, opts -> opts.details.cloudDistance)
                        .build())
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "cloud_scale", int.class))
                        .setName(TextComponent.translatable("devilica.options.cloud_scale"))
                        .setTooltip(TextComponent.translatable("devilica.options.cloud_scale.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 16, 1, value -> TextComponent.literal(value / 4.0F + "x")))
                        .setBinding((opts, value) -> opts.details.cloudScale = value, opts -> opts.details.cloudScale)
                        .build())
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "cloud_opacity", int.class))
                        .setName(TextComponent.translatable("devilica.options.cloud_opacity"))
                        .setTooltip(TextComponent.translatable("devilica.options.cloud_opacity.tooltip"))
                        .setControl(option -> new SliderControl(option, 10, 100, 5, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.details.cloudOpacity = value, opts -> opts.details.cloudOpacity)
                        .build())
                .build();

        final OptionGroup weather = OptionGroup.createBuilder()
                .setId(WEATHER)
                .add(toggle(options, "weather", (o, v) -> o.details.weather = v, o -> o.details.weather))
                .add(toggle(options, "fog", (o, v) -> o.details.fog = v, o -> o.details.fog))
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "fog_start", int.class))
                        .setName(TextComponent.translatable("devilica.options.fog_start"))
                        .setTooltip(TextComponent.translatable("devilica.options.fog_start.tooltip"))
                        .setControl(option -> new SliderControl(option, 20, 95, 5, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.details.fogStart = value, opts -> opts.details.fogStart)
                        .build())
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "fog_distance", int.class))
                        .setName(TextComponent.translatable("devilica.options.fog_distance"))
                        .setTooltip(TextComponent.translatable("devilica.options.fog_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 32, 1, chunksOrDefault()))
                        .setBinding((opts, value) -> opts.details.fogDistance = value, opts -> opts.details.fogDistance)
                        .build())
                .add(toggle(options, "void_fog", (o, v) -> o.details.voidFog = v, o -> o.details.voidFog))
                .build();

        final OptionGroup colors = OptionGroup.createBuilder()
                .setId(COLORS)
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "biome_colors", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.biome_colors"))
                        .setTooltip(TextComponent.translatable("devilica.options.biome_colors.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.details.biomeColors = value, opts -> opts.details.biomeColors)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(toggle(options, "biome_sky_colors", (o, v) -> o.details.biomeSkyColors = v, o -> o.details.biomeSkyColors))
                .build();

        return new OptionPage(DETAILS_PAGE, TextComponent.translatable("devilica.options.pages.details"), ImmutableList.of(sky, weather, colors));
    }

    public static OptionPage animations() {
        final DevilicaOptions options = DevilicaOptions.get();

        final OptionGroup textures = OptionGroup.createBuilder()
                .setId(TEXTURE_ANIMATIONS)
                .add(toggle(options, "animate_water", (o, v) -> o.animations.water = v, o -> o.animations.water))
                .add(toggle(options, "animate_lava", (o, v) -> o.animations.lava = v, o -> o.animations.lava))
                .add(toggle(options, "animate_fire", (o, v) -> o.animations.fire = v, o -> o.animations.fire))
                .add(toggle(options, "animate_portal", (o, v) -> o.animations.portal = v, o -> o.animations.portal))
                .add(toggle(options, "animate_textures", (o, v) -> o.animations.textures = v, o -> o.animations.textures))
                .build();

        final OptionGroup particles = OptionGroup.createBuilder()
                .setId(PARTICLES)
                .add(toggle(options, "particles_all", (o, v) -> o.animations.allParticles = v, o -> o.animations.allParticles))
                .add(toggle(options, "particles_rain_splash", (o, v) -> o.animations.rainSplash = v, o -> o.animations.rainSplash))
                .add(toggle(options, "particles_block_breaking", (o, v) -> o.animations.blockBreaking = v, o -> o.animations.blockBreaking))
                .add(toggle(options, "particles_explosions", (o, v) -> o.animations.explosions = v, o -> o.animations.explosions))
                .add(toggle(options, "particles_potions", (o, v) -> o.animations.potions = v, o -> o.animations.potions))
                .add(toggle(options, "particles_drips", (o, v) -> o.animations.drips = v, o -> o.animations.drips))
                .add(toggle(options, "particles_smoke", (o, v) -> o.animations.smoke = v, o -> o.animations.smoke))
                .add(toggle(options, "particles_flames", (o, v) -> o.animations.flames = v, o -> o.animations.flames))
                .add(toggle(options, "particles_portal", (o, v) -> o.animations.portalParticles = v, o -> o.animations.portalParticles))
                .add(toggle(options, "particles_water", (o, v) -> o.animations.waterParticles = v, o -> o.animations.waterParticles))
                .add(toggle(options, "particles_redstone", (o, v) -> o.animations.redstone = v, o -> o.animations.redstone))
                .add(toggle(options, "particles_void", (o, v) -> o.animations.voidParticles = v, o -> o.animations.voidParticles))
                .build();

        return new OptionPage(ANIMATIONS_PAGE, TextComponent.translatable("devilica.options.pages.animations"), ImmutableList.of(textures, particles));
    }

    /** A slider for each particle class seen so far: the percent of them that spawn */
    public static OptionPage particleClasses() {
        final DevilicaOptions options = DevilicaOptions.get();
        ParticleShares.saveListed();

        final List<String> names = new ArrayList<>(options.animations.particleClasses.keySet());
        names.sort(Comparator.comparing((String name) -> name.substring(name.lastIndexOf('.') + 1)).thenComparing(Comparator.naturalOrder()));
        final OptionGroup.Builder group = OptionGroup.createBuilder().setId(PARTICLE_CLASSES);
        for (String name : names) {
            group.add(OptionImpl.createBuilder(int.class, options)
                    .setId(OptionIdentifier.create(Devilica.MOD_ID, "particle_class_" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"), int.class))
                    .setName(TextComponent.literal(name.substring(name.lastIndexOf('.') + 1)))
                    .setTooltip(TextComponent.translatable("devilica.options.particle_class.tooltip", name))
                    .setControl(option -> new SliderControl(option, 0, 100, 10, ControlValueFormatter.percentage()))
                    .setBinding((opts, value) -> opts.animations.particleClasses.put(name, value), opts -> opts.animations.particleClasses.getOrDefault(name, 100))
                    .build());
        }
        return new OptionPage(PARTICLE_CLASSES_PAGE, TextComponent.translatable("devilica.options.pages.particle_classes"),
            names.isEmpty() ? ImmutableList.of() : ImmutableList.of(group.build()));
    }

    /** A switch for each entity class seen giving off light so far */
    public static OptionPage lightSources() {
        final DevilicaOptions options = DevilicaOptions.get();
        DynamicLights.saveListed();

        final List<String> names = new ArrayList<>(options.dynamicLights.entityClasses.keySet());
        names.sort(Comparator.comparing((String name) -> name.substring(name.lastIndexOf('.') + 1)).thenComparing(Comparator.naturalOrder()));
        final OptionGroup.Builder group = OptionGroup.createBuilder().setId(LIGHT_SOURCES);
        for (String name : names) {
            group.add(OptionImpl.createBuilder(boolean.class, options)
                    .setId(OptionIdentifier.create(Devilica.MOD_ID, "light_source_" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"), boolean.class))
                    .setName(TextComponent.literal(name.substring(name.lastIndexOf('.') + 1)))
                    .setTooltip(TextComponent.translatable("devilica.options.light_source.tooltip", name))
                    .setControl(TickBoxControl::new)
                    .setBinding((opts, value) -> opts.dynamicLights.entityClasses.put(name, value), opts -> opts.dynamicLights.entityClasses.getOrDefault(name, true))
                    .build());
        }
        return new OptionPage(LIGHT_SOURCES_PAGE, TextComponent.translatable("devilica.options.pages.light_sources"),
            names.isEmpty() ? ImmutableList.of() : ImmutableList.of(group.build()));
    }

    public static OptionPage render() {
        final DevilicaOptions options = DevilicaOptions.get();

        final OptionGroup entities = OptionGroup.createBuilder()
                .setId(RENDER_ENTITIES)
                .add(toggle(options, "armor_stands", (o, v) -> o.render.armorStands = v, o -> o.render.armorStands))
                .add(toggle(options, "paintings", (o, v) -> o.render.paintings = v, o -> o.render.paintings))
                .add(toggle(options, "item_frames", (o, v) -> o.render.itemFrames = v, o -> o.render.itemFrames))
                .add(toggle(options, "item_frame_names", (o, v) -> o.render.itemFrameNames = v, o -> o.render.itemFrameNames))
                .add(distance(options, "item_frame_item_distance", (o, v) -> o.render.itemFrameItemDistance = v, o -> o.render.itemFrameItemDistance))
                .add(toggle(options, "player_names", (o, v) -> o.render.playerNames = v, o -> o.render.playerNames))
                .add(toggle(options, "mob_view_shaders", (o, v) -> o.render.mobViewShaders = v, o -> o.render.mobViewShaders))
                .add(distance(options, "entity_distance", (o, v) -> o.render.entityDistance = v, o -> o.render.entityDistance))
                .build();

        final OptionGroup blockEntities = OptionGroup.createBuilder()
                .setId(RENDER_BLOCK_ENTITIES)
                .add(toggle(options, "beacon_beams", (o, v) -> o.render.beaconBeams = v, o -> o.render.beaconBeams))
                .add(toggle(options, "enchanting_books", (o, v) -> o.render.enchantingBooks = v, o -> o.render.enchantingBooks))
                .add(toggle(options, "pistons", (o, v) -> o.render.pistons = v, o -> o.render.pistons))
                .add(toggle(options, "cull_from_behind", (o, v) -> o.render.cullFromBehind = v, o -> o.render.cullFromBehind))
                .add(distance(options, "block_entity_distance", (o, v) -> o.render.blockEntityDistance = v, o -> o.render.blockEntityDistance))
                .build();

        return new OptionPage(RENDER_PAGE, TextComponent.translatable("devilica.options.pages.render"), ImmutableList.of(entities, blockEntities));
    }

    public static OptionPage overlay() {
        final DevilicaOptions options = DevilicaOptions.get();

        final OptionGroup info = OptionGroup.createBuilder()
                .setId(OVERLAY_INFO)
                .add(toggle(options, "overlay_fps", (o, v) -> o.hud.fps = v, o -> o.hud.fps))
                .add(toggle(options, "overlay_fps_extended", (o, v) -> o.hud.fpsExtended = v, o -> o.hud.fpsExtended))
                .add(toggle(options, "overlay_coordinates", (o, v) -> o.hud.coordinates = v, o -> o.hud.coordinates))
                .add(toggle(options, "overlay_memory", (o, v) -> o.hud.memory = v, o -> o.hud.memory))
                .add(OptionImpl.createBuilder(OverlayCorner.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "overlay_corner", OverlayCorner.class))
                        .setName(TextComponent.translatable("devilica.options.overlay_corner"))
                        .setTooltip(TextComponent.translatable("devilica.options.overlay_corner.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, OverlayCorner.class))
                        .setBinding((o, v) -> o.hud.corner = v, o -> o.hud.corner)
                        .build())
                .add(OptionImpl.createBuilder(TextContrast.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "text_contrast", TextContrast.class))
                        .setName(TextComponent.translatable("devilica.options.text_contrast"))
                        .setTooltip(TextComponent.translatable("devilica.options.text_contrast.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, TextContrast.class))
                        .setBinding((o, v) -> o.hud.textContrast = v, o -> o.hud.textContrast)
                        .build())
                .build();

        final OptionGroup screen = OptionGroup.createBuilder()
                .setId(OVERLAY_SCREEN)
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "debug_refresh", int.class))
                        .setName(TextComponent.translatable("devilica.options.debug_refresh"))
                        .setTooltip(TextComponent.translatable("devilica.options.debug_refresh.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 20, 1,
                            value -> value == 0 ? TextComponent.translatable("devilica.options.every_frame") : TextComponent.translatable("devilica.options.milliseconds", value * 50)))
                        .setBinding((o, v) -> o.hud.debugRefreshTicks = v, o -> o.hud.debugRefreshTicks)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "adaptive_vsync", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.adaptive_vsync"))
                        .setTooltip(TextComponent.translatable("devilica.options.adaptive_vsync.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((o, v) -> {
                            o.hud.adaptiveVsync = v;
                            AdaptiveVsync.apply();
                        }, o -> o.hud.adaptiveVsync)
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .build();

        final OptionGroup toasts = OptionGroup.createBuilder()
                .setId(OVERLAY_TOASTS)
                .add(toggle(options, "toasts", (o, v) -> o.hud.toasts = v, o -> o.hud.toasts))
                .add(toggle(options, "advancement_toasts", (o, v) -> o.hud.advancementToasts = v, o -> o.hud.advancementToasts))
                .add(toggle(options, "recipe_toasts", (o, v) -> o.hud.recipeToasts = v, o -> o.hud.recipeToasts))
                .add(toggle(options, "system_toasts", (o, v) -> o.hud.systemToasts = v, o -> o.hud.systemToasts))
                .build();

        return new OptionPage(OVERLAY_PAGE, TextComponent.translatable("devilica.options.pages.overlay"), ImmutableList.of(info, screen, toasts));
    }

    private static OptionImpl<DevilicaOptions, ExplosiveLightMode> explosive(DevilicaOptions options, String name, BiConsumer<DevilicaOptions, ExplosiveLightMode> setter,
                                                                           Function<DevilicaOptions, ExplosiveLightMode> getter) {
        return OptionImpl.createBuilder(ExplosiveLightMode.class, options)
                .setId(OptionIdentifier.create(Devilica.MOD_ID, name, ExplosiveLightMode.class))
                .setName(TextComponent.translatable("devilica.options." + name))
                .setTooltip(TextComponent.translatable("devilica.options." + name + ".tooltip"))
                .setControl(option -> new CyclingControl<>(option, ExplosiveLightMode.class))
                .setBinding(setter, getter)
                .build();
    }

    /** A distance in blocks from 0 (as vanilla) to 256 */
    private static OptionImpl<DevilicaOptions, Integer> distance(DevilicaOptions options, String name, BiConsumer<DevilicaOptions, Integer> setter,
                                                                 Function<DevilicaOptions, Integer> getter) {
        return OptionImpl.createBuilder(int.class, options)
                .setId(OptionIdentifier.create(Devilica.MOD_ID, name, int.class))
                .setName(TextComponent.translatable("devilica.options." + name))
                .setTooltip(TextComponent.translatable("devilica.options." + name + ".tooltip"))
                .setControl(option -> new SliderControl(option, 0, 256, 8, blocksOrDefault()))
                .setBinding(setter, getter)
                .build();
    }

    private static ControlValueFormatter blocksOrDefault() {
        return value -> value == 0 ? TextComponent.translatable("devilica.options.distance_default") : TextComponent.translatable("devilica.options.distance_blocks", value);
    }

    private static ControlValueFormatter chunksOrDefault() {
        return value -> value == 0 ? TextComponent.translatable("devilica.options.distance_default") : TextComponent.translatable("devilica.options.distance_chunks", value);
    }

    /** An on/off option named {@code devilica.options.<name>}, with a tooltip under {@code .tooltip} */
    private static OptionImpl<DevilicaOptions, Boolean> toggle(DevilicaOptions options, String name, BiConsumer<DevilicaOptions, Boolean> setter,
                                                               Function<DevilicaOptions, Boolean> getter) {
        return OptionImpl.createBuilder(boolean.class, options)
                .setId(OptionIdentifier.create(Devilica.MOD_ID, name, boolean.class))
                .setName(TextComponent.translatable("devilica.options." + name))
                .setTooltip(TextComponent.translatable("devilica.options." + name + ".tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding(setter, getter)
                .build();
    }

    public static OptionPage page() {
        final DevilicaOptions options = DevilicaOptions.get();

        final OptionGroup dynamicLights = OptionGroup.createBuilder()
                .setId(DYNAMIC_LIGHTS)
                .add(OptionImpl.createBuilder(DynamicLightsMode.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "dynamic_lights_mode", DynamicLightsMode.class))
                        .setName(TextComponent.translatable("options.dynamic_lights"))
                        .setTooltip(TextComponent.translatable("options.dynamic_lights.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, DynamicLightsMode.class))
                        .setBinding((opts, value) -> opts.dynamicLights.mode = value, opts -> opts.dynamicLights.mode)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "dynamic_lights_with_shaders", boolean.class))
                        .setName(TextComponent.translatable("options.dynamic_lights_shader_force"))
                        .setTooltip(TextComponent.translatable("options.dynamic_lights_shader_force.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.dynamicLights.withShaders = value, opts -> opts.dynamicLights.withShaders)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(toggle(options, "dynamic_lights_self", (o, v) -> o.dynamicLights.self = v, o -> o.dynamicLights.self))
                .add(toggle(options, "dynamic_lights_entities", (o, v) -> o.dynamicLights.entities = v, o -> o.dynamicLights.entities))
                .add(explosive(options, "dynamic_lights_creepers", (o, v) -> o.dynamicLights.creepers = v, o -> o.dynamicLights.creepers))
                .add(explosive(options, "dynamic_lights_tnt", (o, v) -> o.dynamicLights.tnt = v, o -> o.dynamicLights.tnt))
                .add(toggle(options, "dynamic_lights_water", (o, v) -> o.dynamicLights.waterSensitive = v, o -> o.dynamicLights.waterSensitive))
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "colored_light", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.colored_light"))
                        .setTooltip(TextComponent.translatable("devilica.options.colored_light.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.coloredLight.enabled = value, opts -> opts.coloredLight.enabled)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build();

        final OptionGroup zoom = OptionGroup.createBuilder()
                .setId(ZOOM)
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "zoom_smooth", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.zoom_smooth"))
                        .setTooltip(TextComponent.translatable("devilica.options.zoom_smooth.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.zoom.smooth = value, opts -> opts.zoom.smooth)
                        .build())
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "zoom_smooth_speed", int.class))
                        .setName(TextComponent.translatable("devilica.options.zoom_smooth_speed"))
                        .setTooltip(TextComponent.translatable("devilica.options.zoom_smooth_speed.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 30, 1, ControlValueFormatter.number()))
                        .setBinding((opts, value) -> opts.zoom.smoothSpeed = value, opts -> opts.zoom.smoothSpeed)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "zoom_cinematic", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.zoom_cinematic"))
                        .setTooltip(TextComponent.translatable("devilica.options.zoom_cinematic.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.zoom.cinematic = value, opts -> opts.zoom.cinematic)
                        .build())
                .add(toggle(options, "zoom_scroll", (o, v) -> o.zoom.scroll = v, o -> o.zoom.scroll))
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "zoom_default", int.class))
                        .setName(TextComponent.translatable("devilica.options.zoom_default"))
                        .setTooltip(TextComponent.translatable("devilica.options.zoom_default.tooltip"))
                        .setControl(option -> new SliderControl(option, 2, 16, 1, value -> TextComponent.literal(value + "x")))
                        .setBinding((o, v) -> o.zoom.defaultZoom = v, o -> o.zoom.defaultZoom)
                        .build())
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "zoom_sensitivity_floor", int.class))
                        .setName(TextComponent.translatable("devilica.options.zoom_sensitivity_floor"))
                        .setTooltip(TextComponent.translatable("devilica.options.zoom_sensitivity_floor.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 100, 5, ControlValueFormatter.percentage()))
                        .setBinding((o, v) -> o.zoom.sensitivityFloor = v, o -> o.zoom.sensitivityFloor)
                        .build())
                .add(toggle(options, "zoom_toggle", (o, v) -> o.zoom.toggle = v, o -> o.zoom.toggle))
                .add(toggle(options, "zoom_third_person_toggle", (o, v) -> o.zoom.thirdPersonToggle = v, o -> o.zoom.thirdPersonToggle))
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "zoom_third_person_max", int.class))
                        .setName(TextComponent.translatable("devilica.options.zoom_third_person_max"))
                        .setTooltip(TextComponent.translatable("devilica.options.zoom_third_person_max.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 32, 1,
                            value -> value == 0 ? TextComponent.translatable("devilica.options.zoom_third_person_off") : TextComponent.translatable("devilica.options.distance_blocks", value)))
                        .setBinding((o, v) -> o.zoom.thirdPersonMax = v, o -> o.zoom.thirdPersonMax)
                        .build())
                .build();

        final OptionGroup textures = OptionGroup.createBuilder()
                .setId(TEXTURES)
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "connected_textures", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.connected_textures"))
                        .setTooltip(TextComponent.translatable("devilica.options.connected_textures.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.connectedTextures = value, opts -> opts.connectedTextures)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(toggle(options, "random_entities", (o, v) -> o.randomEntities = v, o -> o.randomEntities))
                .add(toggle(options, "custom_sky", (o, v) -> o.customSky = v, o -> o.customSky))
                .add(toggle(options, "custom_item_textures", (o, v) -> o.customItemTextures = v, o -> o.customItemTextures))
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "emissive_textures", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.emissive_textures"))
                        .setTooltip(TextComponent.translatable("devilica.options.emissive_textures.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.emissiveTextures = value, opts -> opts.emissiveTextures)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "custom_colors", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.custom_colors"))
                        .setTooltip(TextComponent.translatable("devilica.options.custom_colors.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.customColors = value;
                            CustomColors.applyFontCodes();
                        }, opts -> opts.customColors)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(BetterGrassMode.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "better_grass", BetterGrassMode.class))
                        .setName(TextComponent.translatable("devilica.options.better_grass"))
                        .setTooltip(TextComponent.translatable("devilica.options.better_grass.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, BetterGrassMode.class))
                        .setBinding((opts, value) -> opts.betterGrass = value, opts -> opts.betterGrass)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "better_snow", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.better_snow"))
                        .setTooltip(TextComponent.translatable("devilica.options.better_snow.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.betterSnow = value, opts -> opts.betterSnow)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "natural_textures", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.natural_textures"))
                        .setTooltip(TextComponent.translatable("devilica.options.natural_textures.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.naturalTextures = value, opts -> opts.naturalTextures)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build();

        final OptionGroup hud = OptionGroup.createBuilder()
                .setId(HUD)
                .add(OptionImpl.createBuilder(boolean.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "hud_caching", boolean.class))
                        .setName(TextComponent.translatable("devilica.options.hud_caching"))
                        .setTooltip(TextComponent.translatable("devilica.options.hud_caching.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.hudCaching = value, opts -> opts.hudCaching)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(toggle(options, "batch_block_entities", (o, v) -> o.batchBlockEntities = v, o -> o.batchBlockEntities))
                .build();

        final OptionGroup shaders = OptionGroup.createBuilder()
                .setId(SHADERS)
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "render_scale", int.class))
                        .setName(TextComponent.translatable("devilica.options.render_scale"))
                        .setTooltip(TextComponent.translatable("devilica.options.render_scale.tooltip"))
                        .setControl(option -> new SliderControl(option, 25, 100, 5, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.renderScale = value, opts -> opts.renderScale)
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(toggle(options, "fsr", (o, v) -> o.fsr = v, o -> o.fsr))
                .add(OptionImpl.createBuilder(int.class, options)
                        .setId(OptionIdentifier.create(Devilica.MOD_ID, "fsr_sharpness", int.class))
                        .setName(TextComponent.translatable("devilica.options.fsr_sharpness"))
                        .setTooltip(TextComponent.translatable("devilica.options.fsr_sharpness.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 100, 5, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.fsrSharpness = value, opts -> opts.fsrSharpness)
                        .build())
                .build();

        return new OptionPage(PAGE, TextComponent.literal(Devilica.MOD_NAME), ImmutableList.of(dynamicLights, zoom, textures, hud, shaders));
    }
}
