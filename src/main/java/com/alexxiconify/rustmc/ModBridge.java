package com.alexxiconify.rustmc;
import net.fabricmc.loader.api.FabricLoader;
    // Returns true if another mod completely owns the lighting threading model, and we cannot safely intervene.
@SuppressWarnings({"unused", "java:S125"}) // API surface + intentional commented-out mod stubs
public class ModBridge {
    // ── Core Performance Mods (actively used in ownership checks) ───────────
    public static final boolean SODIUM      = isMod("sodium");
    public static final boolean STARLIGHT   = isMod("starlight");
    public static final boolean C2ME        = isMod("c2me");
    public static final boolean IRIS        = isMod("iris");
    public static final boolean LITHIUM     = isMod("lithium");
    public static final boolean SCALABLELUX = isMod("scalablelux");
    public static final boolean MOONRISE    = isMod("moonrise");
    public static final boolean VMP         = isMod("vmp");
    public static final boolean FERRITECORE = isMod("ferritecore");
    public static final boolean MODERNFIX   = isMod("modernfix");
    public static final boolean SERVERCORE  = isMod("servercore");
    public static final boolean OXIDIZIUM   = isMod("oxidizium");
    public static final boolean MORECULLING = isMod("moreculling");
    // ── Networking Mods (used in isNetworkingOwned) ─────────────────────────
    public static final boolean RAKNETIFY     = isMod("raknetify");
    public static final boolean VIAFABRICPLUS = isMod("viafabricplus");
    public static final boolean PACKETFIXER   = isMod("packetfixer");
    public static final boolean AUTHME        = isMod("authme");
    // ── Compat Mods (actively used in mixin conditions / render hooks) ──────
    public static final boolean DISTANT_HORIZONS       = isMod("distanthorizons");
    public static final boolean BETTERBLOCKENTITIES    = isMod("betterblockentities");
    public static final boolean ENTITY_MODEL_FEATURES  = isMod("entity_model_features");
    public static final boolean ENTITY_TEXTURE_FEATURES = isMod("entity_texture_features");
    public static final boolean ENTITYCULLING          = isMod("entityculling");
    public static final boolean IMMEDIATELYFAST        = isMod("immediatelyfast");
    public static final boolean TICK_SYNC              = isMod("tick-sync");
    public static final boolean APPLESKIN              = isMod("appleskin");
    // ── HUD / Map Mods (actively used in render hooks) ──────────────────────
    public static final boolean MINIHUD      = isMod("minihud");
    public static final boolean LIGHTY       = isMod("lighty");
    public static final boolean XAEROPLUS    = isMod("xaeroplus");
    public static final boolean GNETUM       = isMod("gnetum");
    public static final boolean REESES_SODIUM = isMod("reeses-sodium-options");
    public static final boolean SODIUM_EXTRA = isMod("sodium-extra");
    // ── Interaction Mods (used in isInteractionOwned) ───────────────────────
    public static final boolean TWEAKEROO    = isMod("tweakeroo");
    public static final boolean LITEMATICA   = isMod("litematica");
    // ════════════════════════════════════════════════════════════════════════
    // Detected but not yet wired — uncomment when adding specific support.
    // ════════════════════════════════════════════════════════════════════════
    // public static final boolean INDIUM = isMod("indium");
    // public static final boolean BADOPTIMIZATIONS = isMod("badoptimizations");
    // public static final boolean FREECAM = isMod("freecam");
    // public static final boolean ACCURATEBLOCKPLACEMENT = isMod("accurateblockplacement");
    // public static final boolean AMBIENTENVIRONMENT = isMod("ambientenvironment");
    // public static final boolean ARCHITECTURY = isMod("architectury");
    // public static final boolean AUDIO_ENGINE_TWEAKS = isMod("audio_engine_tweaks");
    // public static final boolean BALM = isMod("balm");
    // public static final boolean BETTERADVANCEMENTS = isMod("betteradvancements");
    // public static final boolean BETTERBIOMEBLEND = isMod("betterbiomeblend");
    // public static final boolean BETTERF3 = isMod("betterf3");
    // public static final boolean BETTERMOUNTHUD = isMod("bettermounthud");
    // public static final boolean BETTERSTATS = isMod("betterstats");
    // public static final boolean BETTERTAB = isMod("bettertab");
    // public static final boolean CAPES = isMod("capes");
    // public static final boolean CBBG = isMod("cbbg");
    // public static final boolean CHAT_HEADS = isMod("chat_heads");
    // public static final boolean CHATPATCHES = isMod("chatpatches");
    // public static final boolean CLEARWATERLAVA = isMod("clearwaterlava");
    // public static final boolean CLICKTHROUGH = isMod("clickthrough");
    // public static final boolean CLIENTHINGS = isMod("clienthings");
    // public static final boolean CLIENTSORT = isMod("clientsort");
    // public static final boolean CLIENTTWEAKS = isMod("clienttweaks");
    // public static final boolean CLOTH_CONFIG = isMod("cloth-config");
    // public static final boolean CLUMPS = isMod("clumps");
    // public static final boolean COLLECTIVE = isMod("collective");
    // public static final boolean COLLISIONFIX = isMod("collisionfix");
    // public static final boolean COMBINEDPACKS = isMod("combinedpacks");
    // public static final boolean COMMANDKEYS = isMod("commandkeys");
    // public static final boolean CONFIG_EDITOR = isMod("config_editor");
    // public static final boolean CONFIGURABLE = isMod("configurable");
    // public static final boolean CONTINUITY = isMod("continuity");
    // public static final boolean CONTROLLING = isMod("controlling");
    // public static final boolean CRASH_ASSISTANT = isMod("crash_assistant");
    // public static final boolean CREATIVECORE = isMod("creativecore");
    // public static final boolean DARKGRAPH = isMod("darkgraph");
    // public static final boolean DEBUGIFY = isMod("debugify");
    // public static final boolean DETAILABRECONST = isMod("detailabreconst");
    // public static final boolean DHMI = isMod("dhmi");
    // public static final boolean DURABILITYTOOLTIP = isMod("durabilitytooltip");
    // public static final boolean DURABILITYVIEWER = isMod("durabilityviewer");
    // public static final boolean DYNAMIC_FPS = isMod("dynamic_fps");
    // public static final boolean EARLY_LOADING_BAR = isMod("early_loading_bar");
    // public static final boolean EMOJITYPE = isMod("emojitype");
    // public static final boolean ENCHANTMENTLEVELCAPINDICATOR = isMod("enchantmentlevelcapindicator");
    // public static final boolean ENTITY_VIEW_DISTANCE = isMod("entity-view-distance");
    // public static final boolean ESSENTIAL_CONTAINER = isMod("essential-container");
    // public static final boolean EXTREMESOUNDMUFFLER = isMod("extremesoundmuffler");
    // public static final boolean FABRIC_API = isMod("fabric-api");
    // public static final boolean FABRIC_LANGUAGE_KOTLIN = isMod("fabric-language-kotlin");
    // public static final boolean FABRICLOADER = isMod("fabricloader");
    // public static final boolean FALLINGLEAVES = isMod("fallingleaves");
    // public static final boolean FASTIPPING = isMod("fastipping");
    // public static final boolean FIX_MC_STATS = isMod("fix-mc-stats");
    // public static final boolean FORCECLOSEWORLDLOADINGSCREEN = isMod("forcecloseworldloadingscreen");
    // public static final boolean FORGECONFIGAPIPORT = isMod("forgeconfigapiport");
    // public static final boolean FUSION = isMod("fusion");
    // public static final boolean FZZY_CONFIG = isMod("fzzy_config");
    // public static final boolean HELD_ITEM_INFO = isMod("held-item-info");
    // public static final boolean IXERIS = isMod("ixeris");
    // public static final boolean JADE = isMod("jade");
    // public static final boolean JEB = isMod("jeb");
    // public static final boolean LAMBDYNLIGHTS = isMod("lambdynlights");
    // public static final boolean LIBJF = isMod("libjf");
    // public static final boolean LITEMATICA_PRINTER = isMod("litematica_printer");
    // public static final boolean LOCATOR_HEADS = isMod("locator-heads");
    // public static final boolean LOGARITHMIC_VOLUME_CONTROL = isMod("logarithmic-volume-control");
    // public static final boolean MALILIB = isMod("malilib");
    // public static final boolean MAPLINK = isMod("maplink");
    // public static final boolean MCQOY = isMod("mcqoy");
    // public static final boolean MIXINTRACE = isMod("mixintrace");
    // public static final boolean MODMENU = isMod("modmenu");
    // public static final boolean MOREMOUSETWEAKS = isMod("moremousetweaks");
    // public static final boolean MOUSETWEAKS = isMod("mousetweaks");
    // public static final boolean MTFD = isMod("mtfd");
    // public static final boolean NO_TELEMETRY = isMod("no-telemetry");
    // public static final boolean NOCHATREPORTS = isMod("nochatreports");
    // public static final boolean NOPACKCOMPATCHECK = isMod("nopackcompatcheck");
    // public static final boolean NOREPORTBUTTON = isMod("noreportbutton");
    // public static final boolean NOTENOUGHANIMATIONS = isMod("notenoughanimations");
    // public static final boolean NOTENOUGHCRASHES = isMod("notenoughcrashes");
    // public static final boolean OBFUSCATION_IMPROVER = isMod("obfuscation_improver");
    // public static final boolean OPTIPAINTING = isMod("optipainting");
    // public static final boolean PARTICLE_VISOR = isMod("particle-visor");
    // public static final boolean PARTICLE_CORE = isMod("particle_core");
    // public static final boolean PARTICLERAIN = isMod("particlerain");
    // public static final boolean PCHF = isMod("pchf");
    // public static final boolean PLACEHOLDER_API = isMod("placeholder-api");
    // public static final boolean PRESENCEFOOTSTEPS = isMod("presencefootsteps");
    // public static final boolean PUZZLESLIB = isMod("puzzleslib");
    // public static final boolean QUICK_PACK = isMod("quick-pack");
    // public static final boolean RESOURCIFY = isMod("resourcify");
    // public static final boolean RIDINGMOUSEFIX = isMod("ridingmousefix");
    // public static final boolean RRLS = isMod("rrls");
    // public static final boolean RSLS = isMod("rsls");
    // public static final boolean SCIOPHOBIA = isMod("sciophobia");
    // public static final boolean SEARCHABLES = isMod("searchables");
    // public static final boolean SECRET_ITEMS_TAB = isMod("secret-items-tab");
    // public static final boolean SEEDMAPPER = isMod("seedmapper");
    // public static final boolean SERVERLISTFIX = isMod("serverlistfix");
    // public static final boolean SERVERPINGERFIXER = isMod("serverpingerfixer");
    // public static final boolean SHIELDFIXES = isMod("shieldfixes");
    // public static final boolean SHIELDSTATUS = isMod("shieldstatus");
    // public static final boolean SKINLAYERS3D = isMod("skinlayers3d");
    // public static final boolean SMART_PARTICLES = isMod("smart_particles");
    // public static final boolean SMOOTHMAPS = isMod("smoothmaps");
    // public static final boolean SMOOTHTEXTUREFIX = isMod("smoothtexturefix");
    // public static final boolean SODIUM_FULLBRIGHT = isMod("sodium-fullbright");
    // public static final boolean SOUND_PHYSICS_REMASTERED = isMod("sound_physics_remastered");
    // public static final boolean SPARK = isMod("spark");
    // public static final boolean STACK_TO_NEARBY_CHESTS = isMod("stack-to-nearby-chests");
    // public static final boolean STACKDEOBFUSCATOR = isMod("stackdeobfuscator");
    // public static final boolean STATUS_EFFECT_BARS = isMod("status-effect-bars");
    // public static final boolean STFU = isMod("stfu");
    // public static final boolean SUBSTRATE = isMod("substrate");
    // public static final boolean SUPERMARTIJN642CONFIGLIB = isMod("supermartijn642configlib");
    // public static final boolean SWITCHEROO = isMod("switcheroo");
    // public static final boolean TCDCOMMONS = isMod("tcdcommons");
    // public static final boolean TOOLTIPSCROLL = isMod("tooltipscroll");
    // public static final boolean TRANSLUCENT_GLASS = isMod("translucent-glass");
    // public static final boolean TWEAKERMORE = isMod("tweakermore");
    // public static final boolean UKULIB = isMod("ukulib");
    // public static final boolean VERTIGO = isMod("vertigo");
    // public static final boolean VISUALITY = isMod("visuality");
    // public static final boolean WALKSYLIB = isMod("walksylib");
    // public static final boolean WI_ZOOM = isMod("wi_zoom");
    // public static final boolean WILDFIRE_GENDER = isMod("wildfire_gender");
    // public static final boolean XAEROMINIMAP = isMod("xaerominimap");
    // public static final boolean XAEROWORLDMAP = isMod("xaeroworldmap");
    // public static final boolean XAEROZOOMOUT = isMod("xaerozoomout");
    // public static final boolean YEETUSEXPERIMENTUS = isMod("yeetusexperimentus");
    // public static final boolean YET_ANOTHER_CONFIG_LIB_V3 = isMod("yet_another_config_lib_v3");
    // public static final boolean SERVER_TRANSLATIONS_API = isMod("server_translations_api");
    private ModBridge() {}
    private static boolean isMod(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
    // ── Initialization ──────────────────────────────────────────────────────
    public static void initialize() {
        RustMC.LOGGER.info("[Rust-MC] Mod ecosystem detected: Sodium={}, Lithium={}, C2ME={}, Iris={}, " +
                        "ScalableLux={}, Ferritecore={}, Oxidizium={}, ImmediatelyFast={}, DH={}",
                SODIUM, LITHIUM, C2ME, IRIS, SCALABLELUX, FERRITECORE, OXIDIZIUM, IMMEDIATELYFAST, DISTANT_HORIZONS);
    }
    // ── Ownership Checks ────────────────────────────────────────────────────
    //
     // Returns true if another mod completely owns the lighting threading model, and
     // we cannot safely intervene. We now allow intervention for Lux and Sodium
     // where we have specific Rust-based sub-functions.
    public static boolean isLightingOwned() {
        // Starlight is very intrusive; we usually yield unless we have a specific hook.
        // For ScalableLux and Sodium, we can co-optimize.
        return !STARLIGHT || RustMC.CONFIG.isExperimentalCoexistEnabled ( );
    }
    // Returns true if DH is present and handles its own high-performance lighting.
    public static boolean isDhLightingActive() {
        return DISTANT_HORIZONS;
    }
    // Returns true when C2ME or similar controls math/noise so we should skip our hooks.
    public static boolean isMathOwned() {
        return ( !C2ME && !MOONRISE && !MODERNFIX && !FERRITECORE && !SERVERCORE && !LITHIUM )
          || !RustMC.CONFIG.isBridgeC2ME ( );
    }
    // Returns true when Lithium or similar controls' pathfinding.
    public static boolean isPathfindingOwned() {
        return (LITHIUM || MOONRISE || VMP || SERVERCORE)
                && RustMC.CONFIG.isBridgeLithium();
    }
    // Returns true when a specialized networking mod controls packet flow.
    public static boolean isNetworkingOwned() {
        return !RAKNETIFY && !VIAFABRICPLUS && !PACKETFIXER && !AUTHME && !SERVERCORE;
    }
    // Returns true when another mod's frustum would conflict with our Rust frustum.
    public static boolean isFrustumOwned() {
        return (SODIUM && !RustMC.CONFIG.isBridgeSodium()) || MORECULLING;
    }
    // Returns true if interaction/raycasting logic is handled by other mods.
    public static boolean isInteractionOwned() {
        return TWEAKEROO || LITEMATICA || MORECULLING;
    }
    //Gnetum owns HUD frame distribution — yield to it for HUD rendering. // /
    public static boolean isHudOwned() {
        return GNETUM;
    }
    //Reese's Sodium Options / Sodium Extra integrate visual configs in Sodium UI. // /
    public static boolean isVisualConfigIntegrated() {
        return REESES_SODIUM || SODIUM_EXTRA;
    }
}