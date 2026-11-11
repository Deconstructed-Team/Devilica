package com.gtnewhorizons.angelica.config;

import net.minecraftforge.common.config.Config;

/**
 * Devilica: the Angelica options Iris reads, as a Forge config ({@code config/devilica.cfg}). Angelica's own class
 * holds every Angelica module's switch through GTNHLib's config system; only the fields Iris uses are kept, with
 * Angelica's defaults.
 */
@Config(modid = "devilica", category = "shaders")
public final class AngelicaConfig {

    @Config.Ignore
    public static boolean enableCeleritas = true;

    @Config.Comment("Enable Iris shaders")
    @Config.RequiresMcRestart
    public static boolean enableIris = true;

    @Config.Comment("Define IS_IRIS in shader macros")
    public static boolean defineIsIris = true;

    @Config.Comment("Register Iris's hardcoded custom uniforms. May help some shader packs")
    public static boolean enableHardcodedCustomUniforms = false;

    @Config.Comment("Dump PBR atlases for debugging")
    public static boolean enablePBRDebug = false;

    @Config.Comment("Use total world time instead of the time of day, so shader animations keep playing when the daylight cycle is off")
    public static boolean useTotalWorldTime = false;

    @Config.Ignore
    public static boolean speedupAnimations = false;

    @Config.Ignore
    public static boolean disableF3Additions = false;

    private AngelicaConfig() {
    }
}
