/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.ctm.BetterGrassMode;
import dev.feldsher.devilica.dynamiclights.DynamicLightsMode;
import dev.feldsher.devilica.dynamiclights.ExplosiveLightMode;
import dev.feldsher.devilica.hud.OverlayCorner;
import dev.feldsher.devilica.hud.TextContrast;
import dev.feldsher.devilica.leaves.LeavesMode;
import net.minecraftforge.fml.common.Loader;
import org.taumc.celeritas.api.options.structure.OptionStorage;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Devilica's own video settings, kept in {@code config/devilica-options.json} and shown in the video settings. */
public final class DevilicaOptions implements OptionStorage<DevilicaOptions> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "devilica-options.json";

    private static DevilicaOptions instance;

    public DynamicLightsSettings dynamicLights = new DynamicLightsSettings();
    public ZoomSettings zoom = new ZoomSettings();
    /** OptiFine's connected textures from resource packs */
    public boolean connectedTextures = true;
    /** OptiFine's random entities: mobs' other looks from resource packs */
    public boolean randomEntities = true;
    /** Resource packs' skies (OptiFine's and MCPatcher's {@code sky/world*}) */
    public boolean customSky = true;
    /** OptiFine's custom item textures: items' looks by name, enchantments, damage */
    public boolean customItemTextures = true;
    /** OptiFine's emissive textures: {@code _e} images glow at full light */
    public boolean emissiveTextures = true;
    /** OptiFine's custom colours: colour maps, fog, sky, lightmaps, text and potion colours from resource packs */
    public boolean customColors = true;
    /** OptiFine's natural textures: natural blocks turned and mirrored by position */
    public boolean naturalTextures = false;
    public BetterGrassMode betterGrass = BetterGrassMode.OFF;
    /** OptiFine's better snow: a snow layer under plants and fences standing in snow */
    public boolean betterSnow = false;
    /** The HUD drawn once a game tick and laid over each frame */
    public boolean hudCaching = false;
    /** Block entities with known models (chests) drawn together, one draw per texture */
    public boolean batchBlockEntities = true;
    /** With a shader pack, the world's resolution in percent of the window's, stretched to it */
    public int renderScale = 100;
    /** Stretch the lower resolution world with AMD's FSR 1 rather than bilinearly */
    public boolean fsr = true;
    /** FSR's sharpening, 0 to 100 */
    public int fsrSharpness = 80;
    public DetailsSettings details = new DetailsSettings();
    public AnimationSettings animations = new AnimationSettings();
    public RenderSettings render = new RenderSettings();
    public HudSettings hud = new HudSettings();
    public LeavesMode leaves = LeavesMode.DEFAULT;
    public ColoredLightSettings coloredLight = new ColoredLightSettings();

    /** Coloured block light without a shader pack */
    public static final class ColoredLightSettings {
        public boolean enabled = true;
        /**
         * The colour of each source's light, {@code #rrggbb}, by block ({@code minecraft:portal}) or state
         * ({@code minecraft:furnace[facing=north]}). Sources not here give the game's own light.
         */
        public Map<String, String> blockColors = new TreeMap<>(Map.ofEntries(
            Map.entry("minecraft:beacon", "#bff4ff"),
            Map.entry("minecraft:dragon_egg", "#b45aff"),
            Map.entry("minecraft:end_gateway", "#5ae0c8"),
            Map.entry("minecraft:end_portal", "#3ce0b4"),
            Map.entry("minecraft:end_rod", "#f2deff"),
            Map.entry("minecraft:ender_chest", "#3cffaa"),
            Map.entry("minecraft:flowing_lava", "#ff8c3c"),
            Map.entry("minecraft:lava", "#ff8c3c"),
            Map.entry("minecraft:lit_redstone_ore", "#ff3a28"),
            Map.entry("minecraft:magma", "#ff5a1e"),
            Map.entry("minecraft:portal", "#9a3cff"),
            Map.entry("minecraft:redstone_torch", "#ff2a14"),
            Map.entry("minecraft:sea_lantern", "#a8e4ff")));
    }

    /** The info overlay, F3, toasts and the swap interval, as Celeritas Extra had them */
    public static final class HudSettings {
        public boolean fps = false;
        /** The average and the 1% and 0.1% lows next to the FPS */
        public boolean fpsExtended = true;
        public boolean coordinates = false;
        public boolean memory = false;
        public OverlayCorner corner = OverlayCorner.TOP_LEFT;
        public TextContrast textContrast = TextContrast.SHADOW;
        /** F3's text is worked out again every this many ticks; 0 every frame, as vanilla */
        public int debugRefreshTicks = 1;
        public boolean toasts = true;
        public boolean advancementToasts = true;
        public boolean recipeToasts = true;
        public boolean systemToasts = true;
        /** With vsync, a late frame shows at once instead of waiting for the next refresh (swap interval -1) */
        public boolean adaptiveVsync = false;
    }

    public static final class DynamicLightsSettings {
        public DynamicLightsMode mode = DynamicLightsMode.OFF;
        /** Dynamic lights even under a shader pack, which usually lights held items itself */
        public boolean withShaders = false;
        /** The player's own light: what they hold and wear, and fire on them */
        public boolean self = true;
        /** Light from everything else: mobs, other players, dropped items, item frames */
        public boolean entities = true;
        public ExplosiveLightMode creepers = ExplosiveLightMode.FANCY;
        public ExplosiveLightMode tnt = ExplosiveLightMode.FANCY;
        /** Torches, lava, fire charges and blazes go out under water */
        public boolean waterSensitive = true;
        /** Light by entity class, listed as each first gives off light */
        public Map<String, Boolean> entityClasses = new TreeMap<>();
        /** Light levels of items that are not blocks (blocks give their own), by registry name */
        public Map<String, Integer> itemLights = new TreeMap<>(Map.of(
            "minecraft:blaze_powder", 10,
            "minecraft:blaze_rod", 10,
            "minecraft:fire_charge", 10,
            "minecraft:glowstone_dust", 8,
            "minecraft:lava_bucket", 15,
            "minecraft:nether_star", 8,
            "minecraft:prismarine_crystals", 8,
            "minecraft:spectral_arrow", 10));
    }

    public static final class ZoomSettings {
        /** Glide into and out of the zoom instead of jumping */
        public boolean smooth = true;
        /** How fast the glide is, 1 to 30 */
        public int smoothSpeed = 20;
        /** The smooth (cinematic) camera while zoomed in */
        public boolean cinematic = true;
        /** The mouse wheel zooms in and out while zoomed */
        public boolean scroll = true;
        /** How far the zoom goes at first, 2 to 16 times */
        public int defaultZoom = 4;
        /** While zoomed in, the mouse sensitivity drops with the zoom down to this percent of the usual */
        public int sensitivityFloor = 40;
        /** In first person the key switches the zoom on and off instead of being held */
        public boolean toggle = false;
        public boolean thirdPersonToggle = true;
        /** In third person the zoom moves the camera, up to this many blocks away; 0 narrows the view as in first person */
        public int thirdPersonMax = 15;
    }

    /** OptiFine's "Details": parts of the world one can switch off */
    public static final class DetailsSettings {
        public boolean sky = true;
        public boolean sun = true;
        public boolean moon = true;
        public boolean stars = true;
        /** Stars in the sky (vanilla 1500) */
        public int starCount = 1500;
        /** Grass, leaves and water take their biome's colour; off, the plains' everywhere */
        public boolean biomeColors = true;
        /** The sky takes its biome's colour; off, the plains' sky everywhere */
        public boolean biomeSkyColors = true;
        /** The fog darkens toward the bottom of the world */
        public boolean voidFog = true;
        /** Rain and snow falling */
        public boolean weather = true;
        /** The distance fog over the land (fog in water, lava and blindness stays) */
        public boolean fog = true;
        /** Where the fog starts, in percent of the view distance (vanilla 75) */
        public int fogStart = 75;
        /** Where the fog ends, in chunks; 0 at the view distance */
        public int fogDistance = 0;
        /** Clouds this many blocks higher */
        public int cloudHeight = 0;
        /** Clouds this many chunks out; 0 as vanilla */
        public int cloudDistance = 0;
        /** Cloud size, 1 to 16, 4 as vanilla */
        public int cloudScale = 4;
        /** Percent; 80 as vanilla */
        public int cloudOpacity = 80;
    }

    /** What Celeritas Extra let one switch off or draw nearer */
    public static final class RenderSettings {
        public boolean armorStands = true;
        public boolean paintings = true;
        public boolean itemFrames = true;
        public boolean itemFrameNames = true;
        /** Items in frames further than this many blocks away are not drawn; 0 draws them at any distance */
        public int itemFrameItemDistance = 0;
        public boolean playerNames = true;
        public boolean beaconBeams = true;
        /** The book over an enchanting table */
        public boolean enchantingBooks = true;
        /** The block a piston moves, while it moves */
        public boolean pistons = true;
        /** Maps in frames and sign text are not drawn when seen from behind */
        public boolean cullFromBehind = true;
        /** The post effect seen through a spectated creeper's, spider's or enderman's eyes */
        public boolean mobViewShaders = true;
        /** Entities further than this many blocks away are not drawn; 0 as vanilla */
        public int entityDistance = 0;
        /** Classes the entity distance leaves alone, a name or a package ending in {@code .*} */
        public List<String> entityDistanceExempt = new ArrayList<>(List.of("net.minecraft.entity.boss.EntityDragon"));
        /** Block entities further than this many blocks away are not drawn; 0 as vanilla */
        public int blockEntityDistance = 0;
        /** Classes the block entity distance leaves alone, a name or a package ending in {@code .*} */
        public List<String> blockEntityDistanceExempt = new ArrayList<>(List.of("net.minecraft.tileentity.TileEntityBeacon"));
    }

    /** OptiFine's "Animations": animated textures and particles one can switch off */
    public static final class AnimationSettings {
        public boolean water = true;
        public boolean lava = true;
        public boolean fire = true;
        public boolean portal = true;
        /** Every other animated texture */
        public boolean textures = true;

        public boolean rainSplash = true;
        public boolean blockBreaking = true;
        public boolean explosions = true;
        public boolean potions = true;
        public boolean drips = true;
        public boolean smoke = true;
        public boolean flames = true;
        public boolean portalParticles = true;
        public boolean waterParticles = true;
        public boolean redstone = true;
        public boolean voidParticles = true;

        /** Any particle at all */
        public boolean allParticles = true;
        /** Particle classes seen so far, by name, with the percent of them that spawn */
        public Map<String, Integer> particleClasses = new TreeMap<>();
    }

    public static DevilicaOptions get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    @Override
    public DevilicaOptions getData() {
        return this;
    }

    @Override
    public void save() {
        try (Writer writer = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            Devilica.LOGGER.error("Could not save {}", FILE_NAME, e);
        }
    }

    private static DevilicaOptions load() {
        final Path path = path();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                final DevilicaOptions options = GSON.fromJson(reader, DevilicaOptions.class);
                if (options != null) {
                    options.fillMissing();
                    return options;
                }
            } catch (Exception e) {
                Devilica.LOGGER.error("Could not read {}, using defaults", FILE_NAME, e);
            }
        }
        final DevilicaOptions options = new DevilicaOptions();
        options.save();
        return options;
    }

    /** Sections and values a file from an older version does not have */
    private void fillMissing() {
        if (this.dynamicLights == null) {
            this.dynamicLights = new DynamicLightsSettings();
        }
        if (this.dynamicLights.mode == null) {
            this.dynamicLights.mode = DynamicLightsMode.OFF;
        }
        if (this.dynamicLights.creepers == null) {
            this.dynamicLights.creepers = ExplosiveLightMode.FANCY;
        }
        if (this.dynamicLights.tnt == null) {
            this.dynamicLights.tnt = ExplosiveLightMode.FANCY;
        }
        if (this.dynamicLights.entityClasses == null) {
            this.dynamicLights.entityClasses = new TreeMap<>();
        }
        if (this.dynamicLights.itemLights == null) {
            this.dynamicLights.itemLights = new DynamicLightsSettings().itemLights;
        }
        if (this.zoom == null) {
            this.zoom = new ZoomSettings();
        }
        if (this.betterGrass == null) {
            this.betterGrass = BetterGrassMode.OFF;
        }
        if (this.details == null) {
            this.details = new DetailsSettings();
        }
        if (this.animations == null) {
            this.animations = new AnimationSettings();
        }
        if (this.animations.particleClasses == null) {
            this.animations.particleClasses = new TreeMap<>();
        }
        if (this.render == null) {
            this.render = new RenderSettings();
        }
        if (this.leaves == null) {
            this.leaves = LeavesMode.DEFAULT;
        }
        if (this.coloredLight == null) {
            this.coloredLight = new ColoredLightSettings();
        }
        if (this.coloredLight.blockColors == null) {
            this.coloredLight.blockColors = new ColoredLightSettings().blockColors;
        }
        if (this.hud == null) {
            this.hud = new HudSettings();
        }
        if (this.hud.corner == null) {
            this.hud.corner = OverlayCorner.TOP_LEFT;
        }
        if (this.hud.textContrast == null) {
            this.hud.textContrast = TextContrast.SHADOW;
        }
        if (this.render.entityDistanceExempt == null) {
            this.render.entityDistanceExempt = new ArrayList<>();
        }
        if (this.render.blockEntityDistanceExempt == null) {
            this.render.blockEntityDistanceExempt = new ArrayList<>();
        }
    }

    private static Path path() {
        return Loader.instance().getConfigDir().toPath().resolve(FILE_NAME);
    }
}
