package com.alexxiconify.rustmc.config;
import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.BlameLog;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import net.minecraft.text.Text;
import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;
public class ModMenuIntegration implements ModMenuApi {
    private static final String YES = "§aYES";
    private static final String NO  = "§7NO";
    private static final long METRICS_REFRESH_INTERVAL_MS = 100L;
    private static long lastMetricsRefreshMs;
    private static long[] cachedMetrics = new long[] { 0L, 0L, 0L };
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            RustMCConfig cfg = RustMC.CONFIG;
            return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Rust to MC Config"))
                .category(buildStatusCategory())
                .category(buildUnifiedConfigCategory(cfg))
                .category(buildBlameCategory())
                .save(RustMC::saveConfig)
                .build()
                .generateScreen(parent);
        };
    }
    private ConfigCategory buildStatusCategory() {
        refreshMetricsCache();
        return ConfigCategory.createBuilder()
            .name(Text.literal("Status"))
            .tooltip(Text.literal("Runtime status of the Rust native library."))
            .option(Option.<Boolean>createBuilder()
                .name(Text.literal("Native Core"))
                .description(OptionDescription.of(Text.literal(
                    "Shows whether the Rust native library loaded correctly.\n" +
                    "If FAILED, all optimizations fall back to vanilla Java.")))
                .binding(true, NativeBridge::isReady, val -> {})
                .controller(opt -> BooleanControllerBuilder.create(opt)
                    .formatValue(val -> Text.literal(NativeBridge.isReady() ? "§aREADY" : "§cFAILED")))
                .available(false)
                .build())
            .option(buildDetectOption("Sodium Detected",   () -> ModBridge.SODIUM))
            .option(buildDetectOption("Iris Detected",     () -> ModBridge.IRIS))
            .option(buildDetectOption("Lithium Detected",  () -> ModBridge.LITHIUM))
            .option(buildDetectOption("C2ME Detected",     () -> ModBridge.C2ME))
            .option(buildDetectOption("BBE Detected",      () -> ModBridge.BETTERBLOCKENTITIES))
            .option(buildDetectOption("EMF Detected",      () -> ModBridge.ENTITY_MODEL_FEATURES))
            .option(buildDetectOption("ETF Detected",      () -> ModBridge.ENTITY_TEXTURE_FEATURES))
            .option(buildDetectOption("EntityCulling Detected", () -> ModBridge.ENTITYCULLING))
            .option(buildDetectOption("TickSync Detected", () -> ModBridge.TICK_SYNC))
            .option(buildDetectOption("Oxidizium Detected", () -> ModBridge.OXIDIZIUM))
            .option(buildDetectOption("ImmediatelyFast Detected", () -> ModBridge.IMMEDIATELYFAST))
            .option(buildDetectOption("Distant Horizons Detected", () -> ModBridge.DISTANT_HORIZONS))
            .option(buildDetectOption("AppleSkin Detected",       () -> ModBridge.APPLESKIN))
            .option(Option.<Long>createBuilder()
                .name(Text.literal("Total JNI Calls"))
                .description(OptionDescription.of(Text.literal("Lifetime count of Java -> Rust calls via the NativeBridge.")))
                .binding(0L, () -> getCachedMetric(0), val -> {})
                .controller(opt -> dev.isxander.yacl3.api.controller.LongFieldControllerBuilder.create(opt)
                    .formatValue(val -> Text.literal("§e" + getCachedMetric(0))))
                .available(false)
                .build())
            .option(Option.<Long>createBuilder()
                .name(Text.literal("Lighting Updates"))
                .description(OptionDescription.of(Text.literal("Total 3D voxel light updates processed by Rust.")))
                .binding(0L, () -> getCachedMetric(1), val -> {})
                .controller(opt -> dev.isxander.yacl3.api.controller.LongFieldControllerBuilder.create(opt)
                    .formatValue(val -> Text.literal("§a" + getCachedMetric(1))))
                .available(false)
                .build())
            .option(Option.<Long>createBuilder()
                .name(Text.literal("Frustum Tests"))
                .description(OptionDescription.of(Text.literal("Total AABB visibility tests performed by Rust.")))
                .binding(0L, () -> getCachedMetric(2), val -> {})
                .controller(opt -> dev.isxander.yacl3.api.controller.LongFieldControllerBuilder.create(opt)
                    .formatValue(val -> Text.literal("§b" + getCachedMetric(2))))
                .available(false)
                .build())
            .option(Option.<String>createBuilder()
                .name(Text.literal("Frustum Frame Stats"))
                .description(OptionDescription.of(Text.literal("Current frame counters for Rust frustum checks, visible results, and culls.")))
                .binding("checks=0 visible=0 culled=0", ModMenuIntegration::getFrustumFrameStatsText, val -> {})
                .controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create)
                .available(false)
                .build())
            .build();
    }

    private static String getFrustumFrameStatsText() {
        int[] frameStats = NativeBridge.getLastFrustumFrameCounters();
        if (frameStats.length < 3) {
            return "checks=0 visible=0 culled=0";
        }
        return "checks=%d visible=%d culled=%d".formatted(frameStats[0], frameStats[1], frameStats[2]);
    }

    private static long getCachedMetric(int index) {
        refreshMetricsCache();
        if (index < 0 || index >= cachedMetrics.length) return 0L;
        return cachedMetrics[index];
    }

    private static void refreshMetricsCache() {
        long now = System.currentTimeMillis();
        if (now - lastMetricsRefreshMs < METRICS_REFRESH_INTERVAL_MS && cachedMetrics.length >= 3) {
            return;
        }
        long[] metrics = NativeBridge.getMetrics(false);
        if (metrics.length >= 3) {
            cachedMetrics = metrics;
        } else {
            cachedMetrics = new long[] { 0L, 0L, 0L };
        }
        lastMetricsRefreshMs = now;
    }

    private ConfigCategory buildUnifiedConfigCategory(RustMCConfig cfg) {
        com.iafenvoy.elb.config.ElbConfig elb = com.iafenvoy.elb.config.ElbConfig.getInstance();
        ConfigCategory.Builder builder = ConfigCategory.createBuilder()
            .name(Text.literal("Config"))
            .tooltip(Text.literal("All user-configurable settings in one tab."));
        addMathAndNoiseOptions(builder, cfg);
        addNativeFeatureOptions(builder, cfg);
        addCompatibilityOptions(builder, cfg);
        addBridgeOptions(builder, cfg);
        addLoadingScreenOptions(builder, cfg);
        addEarlyLoadingBarOptions(builder, elb);
        addDeveloperOptions(builder, cfg);
        return builder.build();
    }

    private void addMathAndNoiseOptions(ConfigCategory.Builder builder, RustMCConfig cfg) {
        builder
            .option(buildSectionHeader("Math and Noise", "Active math/noise hooks."))
            .option(buildBooleanOption("Native Noise (World Gen)",
                "Replaces SimplexNoiseSampler with Rust Simplex, seeded by world seed.\nDisabled automatically when C2ME Bridge is ON.",
                cfg::isUseNativeNoise, v -> cfg.setUseNativeNoise(v != null && v)));
    }

    private void addNativeFeatureOptions(ConfigCategory.Builder builder, RustMCConfig cfg) {
        builder
            .option(buildSectionHeader("Native Features", "Core native feature toggles."))
            .option(buildBooleanOption("Native Compression",
                "Replaces packet Zlib compression with Rust zlib-ng encoder.",
                cfg::isUseNativeCompression, v -> cfg.setUseNativeCompression(v != null && v)))
            .option(buildBooleanOption("Native Lighting (Experimental)",
                "Hooks lighting engine for Rust-parallel updates.\nDisabled when Sodium/Starlight/C2ME/Iris Bridge is ON.",
                cfg::isUseNativeLighting, v -> cfg.setUseNativeLighting(v != null && v)))
            .option(buildBooleanOption("Native Pathfinding (Experimental)",
                "Rust A// pre-computes mob path distances; cancels vanilla only when mob is at target.\nDisabled when Lithium Bridge is ON.",
                cfg::isUseNativePathfinding, v -> cfg.setUseNativePathfinding(v != null && v)))
            .option(buildBooleanOption("Native Culling (Fixes Dripstone)",
                "Prevents aggressive face culling on 3D Dripstone (VanillaTweaks).",
                cfg::isUseNativeCulling, v -> cfg.setUseNativeCulling(v != null && v)))
            .option(buildBooleanOption("Native Commands (Experimental)",
                "Passes server commands to Rust before Brigadier. Currently a no-op — leave OFF.",
                cfg::isUseNativeCommands, v -> cfg.setUseNativeCommands(v != null && v)))
            .option(buildBooleanOption("Native Metrics HUD",
                "Shows native performance metrics. Toggle with F3 + R.",
                cfg::isEnableNativeMetricsHud, v -> cfg.setEnableNativeMetricsHud(v != null && v)))
            .option(buildBooleanOption("DNS Cache (Server Pings)",
                "Caches DNS lookups for 5 minutes via Rust to speed up server list pings.\nCached entries: " + NativeBridge.dnsCacheSize(),
                cfg::isEnableDnsCache, v -> cfg.setEnableDnsCache(v != null && v)));
    }

    private void addCompatibilityOptions(ConfigCategory.Builder builder, RustMCConfig cfg) {
        builder
            .option(buildSectionHeader("Mod Compatibility", "Compatibility toggles for other mods."))
            .option(buildBooleanOption("Particle Distance Culling",
                "Skip rendering particles beyond view distance threshold.",
                cfg::isEnableParticleCulling, v -> cfg.setEnableParticleCulling(v != null && v)))
            .option(buildBooleanOption("Expand Chunk Builder Threads",
                "Use more CPU cores for chunk building (yields to Sodium if present).",
                cfg::isEnableChunkBuilderExpand, v -> cfg.setEnableChunkBuilderExpand(v != null && v)))
            .option(buildBooleanOption("TickSync Compatibility",
                "Tick-smoothing integration. Yields to TickSync mod when installed.",
                cfg::isEnableTickSyncCompat, v -> cfg.setEnableTickSyncCompat(v != null && v)))
            .option(buildBooleanOption("EMF/ETF Compatibility",
                "Entity Model/Texture Features compatibility.",
                cfg::isEnableEMFCompat, v -> cfg.setEnableEMFCompat(v != null && v)))
            .option(buildBooleanOption("ETF Texture Compatibility",
                "Entity Texture Features compatibility.",
                cfg::isEnableETFCompat, v -> cfg.setEnableETFCompat(v != null && v)))
            .option(buildBooleanOption("ImmediatelyFast Compatibility",
                "Works with ImmediatelyFast's batched rendering and adapts particle cutoff.",
                cfg::isEnableImmediatelyFastCompat, v -> cfg.setEnableImmediatelyFastCompat(v != null && v)))
            .option(buildBooleanOption("AppleSkin Compatibility",
                "AppleSkin HUD overlay compatibility.",
                cfg::isEnableAppleSkinCompat, v -> cfg.setEnableAppleSkinCompat(v != null && v)))
            .option(buildBooleanOption("EntityCulling Compatibility",
                "Yields entity distance culling to EntityCulling mod when installed.",
                cfg::isEnableEntityCullingCompat, v -> cfg.setEnableEntityCullingCompat(v != null && v)))
            .option(buildBooleanOption("Client Redstone Skip",
                "Skip client-side redstone neighbor updates (server handles logic).",
                cfg::isEnableClientRedstoneSkip, v -> cfg.setEnableClientRedstoneSkip(v != null && v)))

            .option(buildSectionHeader("DH Cave Culling Debug", "Controls how DH cave culling decides reference Y and optional logs."))
            .option(buildBooleanOption("Enable DH Cave Culling",
                "When ON, DH LOD loading is disabled below surface threshold.",
                cfg::isEnableDhCaveCulling, v -> cfg.setEnableDhCaveCulling(v != null && v)))
            .option(buildBooleanOption("Use Player Position For DH Culling",
                "When ON, cave culling uses player Y instead of camera Y (useful with freecam).",
                cfg::isUsePlayerPosForDhCulling, v -> cfg.setUsePlayerPosForDhCulling(v != null && v)))
            .option(buildBooleanOption("DH Culling Debug Log",
                "Logs whether each DH section was culled and which Y source was used.",
                cfg::isEnableDhCullingDebugLog, v -> cfg.setEnableDhCullingDebugLog(v != null && v)));
    }

    private void addBridgeOptions(ConfigCategory.Builder builder, RustMCConfig cfg) {
        builder
            .option(buildSectionHeader("Mod Bridges", "Subsystem ownership handoff to other mods."))
            .option(buildBooleanOption("Sodium Bridge", "Defer rendering-related math to Sodium when present.",
                cfg::isBridgeSodium, v -> cfg.setBridgeSodium(v != null && v)))
            .option(buildBooleanOption("Starlight Bridge", "Disable native lighting when Starlight is installed.",
                cfg::isBridgeStarlight, v -> cfg.setBridgeStarlight(v != null && v)))
            .option(buildBooleanOption("C2ME Bridge", "Disable native math/noise/lighting hooks when C2ME is installed.",
                cfg::isBridgeC2ME, v -> cfg.setBridgeC2ME(v != null && v)))
            .option(buildBooleanOption("Iris Bridge", "Disable native lighting hook when Iris is installed.",
                cfg::isBridgeIris, v -> cfg.setBridgeIris(v != null && v)))
            .option(buildBooleanOption("Lithium Bridge", "Disable native pathfinding when Lithium is installed.",
                cfg::isBridgeLithium, v -> cfg.setBridgeLithium(v != null && v)));
    }

    private void addLoadingScreenOptions(ConfigCategory.Builder builder, RustMCConfig cfg) {
        builder
            .option(buildSectionHeader("Loading Screen Colors", "Customize in-game loading overlay colors."))
            .option(buildBooleanOption("Enable Fast Loading Screen",
                "Enables the Rust-MC loading overlay (RAM bar, mod count, dark background).",
                cfg::isUseFastLoadingScreen, v -> cfg.setUseFastLoadingScreen(v != null && v)))
            .option(buildColorOption("Bar Background", "Dark track color behind the RAM bar.", () -> new Color(cfg.getLoadingBarBgColor(), true), c -> cfg.setLoadingBarBgColor(c.getRGB())))
            .option(buildColorOption("Bar Low (< 60%)", "Color when RAM usage is below 60%.", () -> new Color(cfg.getLoadingBarLowColor(), true), c -> cfg.setLoadingBarLowColor(c.getRGB())))
            .option(buildColorOption("Bar Mid (60-80%)", "Color when RAM usage is 60-80%.", () -> new Color(cfg.getLoadingBarMidColor(), true), c -> cfg.setLoadingBarMidColor(c.getRGB())))
            .option(buildColorOption("Bar High (> 80%)", "Color when RAM usage exceeds 80%.", () -> new Color(cfg.getLoadingBarHighColor(), true), c -> cfg.setLoadingBarHighColor(c.getRGB())))
            .option(buildColorOption("RAM Label Text", "Color of the RAM MB / MB text.", () -> new Color(cfg.getLoadingBarTextColor(), true), c -> cfg.setLoadingBarTextColor(c.getRGB())))
            .option(buildColorOption("Mod Count Text", "Color of the Rust-MC mod count label.", () -> new Color(cfg.getLoadingBarSubtextColor(), true), c -> cfg.setLoadingBarSubtextColor(c.getRGB())));
    }

    private void addEarlyLoadingBarOptions(ConfigCategory.Builder builder, com.iafenvoy.elb.config.ElbConfig elb) {
        builder
            .option(buildSectionHeader("Early Loading Bar", "Pre-launch Swing window appearance."))
            .option(Option.<String>createBuilder().name(Text.literal("Window Title")).description(OptionDescription.of(Text.literal("Title text. Use %version% for MC version."))).binding("Early Loading Bar %version%", elb::getBarTitle, elb::setBarTitle).controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create).build())
            .option(Option.<String>createBuilder().name(Text.literal("Loading Message")).description(OptionDescription.of(Text.literal("Text shown below the progress bars."))).binding("Loading Minecraft %version%...", elb::getBarMessage, elb::setBarMessage).controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create).build())
            .option(Option.<String>createBuilder().name(Text.literal("Logo Path")).description(OptionDescription.of(Text.literal("Absolute path to a .png / .jpg logo (optional)."))).binding("", elb::getLogoPath, elb::setLogoPath).controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create).build())
            .option(buildColorOptionSwing("Memory Bar Color", "Color of the RAM usage bar in the ELB window.", () -> parseSwingColor(elb.getMemoryBarColor(), java.awt.Color.RED), c -> elb.setMemoryBarColor(String.valueOf(c.getRGB()))))
            .option(buildColorOptionSwing("Mod Bar Color", "Color of the mod loading progress bar in the ELB window.", () -> parseSwingColor(elb.getMessageBarColor(), java.awt.Color.MAGENTA), c -> elb.setMessageBarColor(String.valueOf(c.getRGB()))));
    }

    private void addDeveloperOptions(ConfigCategory.Builder builder, RustMCConfig cfg) {
        builder
            .option(buildSectionHeader("Developer", "Developer and logging options."))
            .option(buildBooleanOption("Silence Startup Logs",
                "Filters repetitive INFO-level startup spam from other mods. WARN and ERROR are never suppressed.",
                cfg::isSilenceLogs, v -> cfg.setSilenceLogs(v != null && v)));
    }
    private Option<Boolean> buildDetectOption(String name, Supplier<Boolean> isDetected) {
        return Option.<Boolean>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal("Whether " + name.replace(" Detected", "") + " is installed.")))
            .binding(true, isDetected, val -> {})
            .controller(opt -> BooleanControllerBuilder.create(opt)
                .formatValue(val -> Text.literal(Boolean.TRUE.equals(isDetected.get()) ? YES : NO)))
            .available(false)
            .build();
    }

    // --- Helpers ---
    private Option<Boolean> buildSectionHeader(String title, String desc) {
        return Option.<Boolean>createBuilder()
            .name(Text.literal("-- " + title + " --"))
            .description(OptionDescription.of(Text.literal(desc)))
            .binding(true, () -> true, v -> {})
            .controller(opt -> BooleanControllerBuilder.create(opt)
                .formatValue(v -> Text.literal("§7section")))
            .available(false)
            .build();
    }

    private Option<Boolean> buildBooleanOption(String name, String desc, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal(desc)))
            .binding(true, getter, setter)
            .controller(BooleanControllerBuilder::create)
            .build();
    }
    private Option<Color> buildColorOption(String name, String desc, Supplier<Color> getter, Consumer<Color> setter) {
        return Option.<Color>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal(desc)))
            .binding(getter.get(), getter, setter)
            .controller(ColorControllerBuilder::create)
            .build();
    }
    //Same as buildColorOption but for Swing java.awt.Color (ELB colors). // /
    private Option<Color> buildColorOptionSwing(String name, String desc, Supplier<Color> getter, Consumer<Color> setter) {
        return buildColorOption(name, desc, getter, setter);
    }
    private static java.awt.Color parseSwingColor(String val, java.awt.Color fallback) {
        try { return new java.awt.Color(Integer.parseInt(val)); } catch (Exception e) { return fallback; }
    }
    // ── Blame Chart ─────────────────────────────────────────────────────────
    private static final String PCT_FORMAT = "%.1f%%";
    @SuppressWarnings("java:S3776")
    private ConfigCategory buildBlameCategory() {
        var builder = ConfigCategory.createBuilder()
            .name(Text.literal("Blame Chart"))
            .tooltip(Text.literal("Loading phase timings from launcher start to game-ready."));
        java.util.List<BlameLog.Entry> entries = BlameLog.getEntriesWithGaps();
        long tracked = BlameLog.trackedMs();
        long wallClock = BlameLog.wallClockMs();
        if (entries.isEmpty()) {
            builder.option(Option.<Boolean>createBuilder()
                .name(Text.literal("No data yet"))
                .description(OptionDescription.of(Text.literal(
                    "Blame data is recorded during startup.\n" +
                    "Close this screen and re-open after the game finishes loading.")))
                .binding(false, () -> false, v -> {})
                .controller(opt -> BooleanControllerBuilder.create(opt)
                    .formatValue(v -> Text.literal("§7—")))
                .available(false)
                .build());
        } else {
            addTotalSummary(builder, tracked, wallClock);
            addPhaseEntries(builder, entries, wallClock);
            addMixinBreakdown(builder);
        }
        return builder.build();
    }
    private static void addTotalSummary(ConfigCategory.Builder builder, long tracked, long wallClock) {
        long untracked = wallClock - tracked;
        builder.option(Option.<Boolean>createBuilder()
            .name(Text.literal("Wall Clock (JVM → Game Ready)"))
            .description(OptionDescription.of(Text.literal(
                "Total wall-clock time from JVM start to game ready: " +
                wallClock + "ms (" + String.format("%.1f", wallClock / 1000.0) + "s)\n\n" +
                "Tracked phases: " + tracked + "ms\n" +
                (untracked > 500 ? "Untracked gaps (JVM overhead, GC, etc.): " + untracked + "ms" : "Minimal untracked time."))))
            .binding(true, () -> true, v -> {})
            .controller(opt -> BooleanControllerBuilder.create(opt)
                .formatValue(v -> Text.literal("§e" + String.format("%.1fs", wallClock / 1000.0))))
            .available(false)
            .build());
        if (untracked > 500) {
            builder.option(Option.<Boolean>createBuilder()
                .name(Text.literal("  ⚠ Untracked Time"))
                .description(OptionDescription.of(Text.literal(
                    "Time not attributed to any tracked phase: " + untracked + "ms\n\n" +
                    "This includes JVM class verification, GC pauses, driver init,\n" +
                    "GLFW/GL context creation, and white-screen time before rendering starts.")))
                .binding(true, () -> true, v -> {})
                .controller(opt -> BooleanControllerBuilder.create(opt)
                    .formatValue(v -> Text.literal("§c" + untracked + "ms")))
                .available(false)
                .build());
        }
    }
    private static void addPhaseEntries(ConfigCategory.Builder builder, java.util.List<BlameLog.Entry> entries, long wallClock) {
        for (BlameLog.Entry entry : entries) {
            long dur = entry.durationMs();
            float pct = wallClock > 0 ? (float) dur / wallClock * 100f : 0;
            String bar = buildAsciiBar(pct);
            boolean isGap = entry.phase().startsWith("⚠");
            String color = isGap ? "§c" : blameColor(dur);
            String assessment = isGap
                ? """
                  This time is NOT attributed to any tracked phase.
                  Causes: JVM class verification, GC pauses, driver init,
                  GLFW/GL context setup, or phases we don't detect yet."""
                : blameAssessment(dur);
            builder.option(Option.<Boolean>createBuilder()
                .name(Text.literal(entry.phase()))
                .description(OptionDescription.of(Text.literal(
                    "Duration: " + dur + "ms (" + String.format(PCT_FORMAT, pct) + " of wall clock)\n\n" +
                    bar + "\n\n" + assessment)))
                .binding(true, () -> true, v -> {})
                .controller(opt -> BooleanControllerBuilder.create(opt)
                    .formatValue(v -> Text.literal(color + dur + "ms")))
                .available(false)
                .build());
        }
    }
    private static void addMixinBreakdown(ConfigCategory.Builder builder) {
        java.util.Map<String, Long> mixinTimings = com.alexxiconify.rustmc.MixinManager.getGroupTimings();
        if (mixinTimings.isEmpty()) return;
        long mixinTotalNs = mixinTimings.values().stream().mapToLong(Long::longValue).sum();
        long mixinTotalMs = mixinTotalNs / 1_000_000;
        builder.option(Option.<Boolean>createBuilder()
            .name(Text.literal("── Mixin Breakdown ──"))
            .description(OptionDescription.of(Text.literal(
                "Per-group mixin application time (" + mixinTotalMs + "ms total).\n" +
                "Shows how long each category of mixins took to apply during class loading.")))
            .binding(true, () -> true, v -> {})
            .controller(opt -> BooleanControllerBuilder.create(opt)
                .formatValue(v -> Text.literal("§d" + mixinTotalMs + "ms")))
            .available(false)
            .build());
        java.util.List<java.util.Map.Entry<String, Long>> sorted = new java.util.ArrayList<>(mixinTimings.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        for (java.util.Map.Entry<String, Long> mEntry : sorted) {
            long ms = mEntry.getValue() / 1_000_000;
            float mPct = mixinTotalMs > 0 ? (float) ms / mixinTotalMs * 100f : 0;
            String mBar = buildAsciiBar(mPct);
            String mColor = mixinBlameColor(ms);
            builder.option(Option.<Boolean>createBuilder()
                .name(Text.literal("  " + mEntry.getKey()))
                .description(OptionDescription.of(Text.literal(
                    "Mixin application time: " + ms + "ms (" + String.format(PCT_FORMAT, mPct) + " of mixin total)\n\n" + mBar)))
                .binding(true, () -> true, v -> {})
                .controller(opt -> BooleanControllerBuilder.create(opt)
                    .formatValue(v -> Text.literal(mColor + ms + "ms")))
                .available(false)
                .build());
        }
    }
    //Color code for mixin timing entries. // /
    private static String mixinBlameColor(long ms) {
        if (ms > 100) return "§c";
        if (ms > 30) return "§e";
        return "§a";
    }
    //Builds an ASCII bar like [████████░░░░] for the given percentage. // /
    private static String buildAsciiBar(float pct) {
        int filled = Math.clamp(Math.round(pct / 5f), 0, 20);
        return "[" + "█".repeat(filled) + "░".repeat(20 - filled) + "] " +
               String.format(PCT_FORMAT, pct);
    }
    private static String blameColor(long durationMs) {
        if (durationMs > 5000) return "§c";
        if (durationMs > 2000) return "§e";
        return "§a";
    }
    private static String blameAssessment(long durationMs) {
        if (durationMs > 5000) return "⚠ This phase is slow and may be a bottleneck.";
        if (durationMs > 2000) return "This phase took moderate time.";
        return "This phase loaded quickly.";
    }
}