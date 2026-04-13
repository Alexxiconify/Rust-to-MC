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
import net.minecraft.text.Text;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModMenuIntegration implements ModMenuApi {

    private static final String DETECTED     = "§aDETECTED";
    private static final String NOT_FOUND    = "§7NOT FOUND";

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            if (!NativeBridge.isReady()) return null;
            RustMCConfig cfg = RustMC.CONFIG;
            return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Rust to MC Config"))
                .category(buildStatusCategory())
                .category(buildFeaturesCategory(cfg))
                .category(buildMathCategory(cfg))
                .category(buildModCompatCategory(cfg))
                .category(buildDevCategory(cfg))
                .category(buildMetricsCategory())
                .category(buildBlameCategory())
                .save(RustMC::saveConfig)
                .build()
                .generateScreen(parent);
        };
    }

    private ConfigCategory buildStatusCategory() {
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
                .build())
            .option(buildDetectOption("Sodium",   () -> ModBridge.SODIUM))
            .option(buildDetectOption("Iris",     () -> ModBridge.IRIS))
            .option(buildDetectOption("Lithium",  () -> ModBridge.LITHIUM))
            .option(buildDetectOption("C2ME",     () -> ModBridge.C2ME))
            .option(buildDetectOption("BBE",      () -> ModBridge.BETTERBLOCKENTITIES))
            .option(buildDetectOption("EMF/ETF",  () -> ModBridge.ENTITY_MODEL_FEATURES || ModBridge.ENTITY_TEXTURE_FEATURES))
            .option(buildDetectOption("EntityCulling", () -> ModBridge.ENTITYCULLING))
            .option(buildDetectOption("ImmediatelyFast", () -> ModBridge.IMMEDIATELYFAST))
            .option(buildDetectOption("Distant Horizons", () -> ModBridge.DISTANT_HORIZONS))
            .build();
    }

    private Option<Boolean> buildDetectOption(String name, Supplier<Boolean> isDetected) {
        return Option.<Boolean>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal("Detection status for " + name + ".")))
            .binding(true, isDetected, val -> {})
            .controller(opt -> BooleanControllerBuilder.create(opt)
                .formatValue(val -> Text.literal(Boolean.TRUE.equals(isDetected.get()) ? DETECTED : NOT_FOUND)))
            .build();
    }

    private ConfigCategory buildMathCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Math Optimizations"))
            .tooltip(Text.literal("Toggle individual Rust-backed math replacements."))
            .option(buildBooleanOption("Fast Sine",
                "Replaces MathHelper.sin() with a LUT (lookup table).",
                cfg::isUseNativeSine, v -> cfg.setUseNativeSine(v != null && v)))
            .option(buildBooleanOption("Fast Cosine",
                "Replaces MathHelper.cos() with a LUT.",
                cfg::isUseNativeCos, v -> cfg.setUseNativeCos(v != null && v)))
            .option(buildBooleanOption("Native Sqrt",
                "Replaces MathHelper.sqrt() cast path.",
                cfg::isUseNativeSqrt, v -> cfg.setUseNativeSqrt(v != null && v)))
            .option(buildBooleanOption("Fast Inv-Sqrt",
                "Replaces MathHelper.fastInvSqrt() with Quake III algorithm.",
                cfg::isUseNativeInvSqrt, v -> cfg.setUseNativeInvSqrt(v != null && v)))
            .option(buildBooleanOption("Native Atan2",
                "Replaces MathHelper.atan2().", cfg::isUseNativeAtan2, v -> cfg.setUseNativeAtan2(v != null && v)))
            .option(buildBooleanOption("Native Floor",
                "Replaces MathHelper.floor() with bitwise cast.", cfg::isUseNativeFloor, v -> cfg.setUseNativeFloor(v != null && v)))
            .option(buildBooleanOption("Native Noise (World Gen)",
                "Replaces SimplexNoiseSampler with Rust Simplex, seeded by world seed.",
                cfg::isUseNativeNoise, v -> cfg.setUseNativeNoise(v != null && v)))
            .build();
    }

    private ConfigCategory buildFeaturesCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Native Features"))
            .tooltip(Text.literal("Toggle individual Rust-backed feature hooks."))
            .option(buildBooleanOption("Native Compression",
                "Replaces packet Zlib compression with Rust zlib-ng encoder.",
                cfg::isUseNativeCompression, v -> cfg.setUseNativeCompression(v != null && v)))
            .option(buildBooleanOption("Native Lighting (Experimental)",
                "Hooks lighting engine for Rust-parallel updates. Automatically bypasses conflicts.",
                cfg::isUseNativeLighting, v -> cfg.setUseNativeLighting(v != null && v)))
            .option(buildBooleanOption("Lock Culling to Player Body (Debug)",
                "If ON, culling follows your physical body. If OFF (Default), culling follows your camera (lens).\nUseful for debugging frustum leaks with Freecam.",
                cfg::isLockCullingToPlayer, v -> cfg.setLockCullingToPlayer(v != null && v)))
            .option(buildBooleanOption("DNS Cache (Server Pings)",
                "Caches DNS lookups for 5 minutes via Rust to speed up server list pings.",
                cfg::isEnableDnsCache, v -> cfg.setEnableDnsCache(v != null && v)))
            .option(buildBooleanOption("Native Metrics HUD",
                "Shows native performance metrics. Toggle with F3 + R.",
                cfg::isEnableNativeMetricsHud, v -> cfg.setEnableNativeMetricsHud(v != null && v)))
            .build();
    }

    private ConfigCategory buildModCompatCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Mod Compatibility"))
            .tooltip(Text.literal("Toggle individual optimization features and mod compatibility hooks."))
            .option(buildBooleanOption("Particle Distance Culling",
                "Skip rendering particles beyond view distance threshold.",
                cfg::isEnableParticleCulling, v -> cfg.setEnableParticleCulling(v != null && v)))
            .option(buildBooleanOption("Expand Chunk Builder Threads",
                "Use more CPU cores for chunk building (yields to Sodium if present).",
                cfg::isEnableChunkBuilderExpand, v -> cfg.setEnableChunkBuilderExpand(v != null && v)))
            .option(buildBooleanOption("TickSync Compatibility",
                "Tick-smoothing integration. Yields to TickSync mod when installed.",
                cfg::isEnableTickSyncCompat, v -> cfg.setEnableTickSyncCompat(v != null && v)))
            .option(buildBooleanOption("BBE Compatibility",
                "Better Block Entities compat — distance-cull block entities when BBE is absent.",
                cfg::isEnableBBECompat, v -> cfg.setEnableBBECompat(v != null && v)))
            .option(buildBooleanOption("EMF/ETF Compatibility",
                "Entity Model/Texture Features — tighten particle distance culling when heavy models active.\n" +
                "EMF: custom entity models. ETF: random/biome entity textures.",
                cfg::isEnableEMFCompat, v -> cfg.setEnableEMFCompat(v != null && v)))
            .option(buildBooleanOption("ETF Texture Compatibility",
                "Entity Texture Features — enable tighter culling for biome/random entity textures.",
                cfg::isEnableETFCompat, v -> cfg.setEnableETFCompat(v != null && v)))
            .option(buildBooleanOption("ImmediatelyFast Compatibility",
                                       """
                                         Works in concert with ImmediatelyFast's batched rendering.
                                         When enabled, we use a more generous particle cutoff since IF makes draws cheaper,
                                         and skip our own batching hints to let IF drive the draw pipeline.""" ,
                cfg::isEnableImmediatelyFastCompat, v -> cfg.setEnableImmediatelyFastCompat(v != null && v)))
            .option(buildBooleanOption("AppleSkin Compatibility",
                "AppleSkin HUD overlay compat — ensures our overlays don't conflict with AppleSkin's saturation display.",
                cfg::isEnableAppleSkinCompat, v -> cfg.setEnableAppleSkinCompat(v != null && v)))
            .option(buildBooleanOption("EntityCulling Compatibility",
                "Yields entity distance culling to EntityCulling mod when installed.",
                cfg::isEnableEntityCullingCompat, v -> cfg.setEnableEntityCullingCompat(v != null && v)))
            .option(buildBooleanOption("Client Redstone Skip",
                "Skip client-side redstone neighbor updates (server handles logic).",
                cfg::isEnableClientRedstoneSkip, v -> cfg.setEnableClientRedstoneSkip(v != null && v)))
            .build();
    }

    private ConfigCategory buildDevCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Developer"))
            .option(buildBooleanOption("Silence Startup Logs",
                "Filters repetitive INFO-level startup spam from other mods.\nWARN and ERROR messages are never suppressed.",
                cfg::isSilenceLogs, v -> cfg.setSilenceLogs(v != null && v)))
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


    private ConfigCategory buildMetricsCategory() {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Native Metrics"))
            .tooltip(Text.literal("Live telemetry from the Rust native core."))
            .option(Option.<Long>createBuilder()
                .name(Text.literal("Total JNI Calls"))
                .description(OptionDescription.of(Text.literal("Lifetime count of Java → Rust calls via the NativeBridge.")))
                .binding(0L, () -> NativeBridge.getMetrics(false)[0], val -> {})
                .controller(opt -> dev.isxander.yacl3.api.controller.LongFieldControllerBuilder.create(opt)
                    .formatValue(val -> Text.literal("§e" + NativeBridge.getMetrics(false)[0])))
                .build())
            .option(Option.<Long>createBuilder()
                .name(Text.literal("Lighting Updates"))
                .description(OptionDescription.of(Text.literal("Total 3D voxel light updates processed by SIMD/Parallel Rust kernels.")))
                .binding(0L, () -> NativeBridge.getMetrics(false)[1], val -> {})
                .controller(opt -> dev.isxander.yacl3.api.controller.LongFieldControllerBuilder.create(opt)
                    .formatValue(val -> Text.literal("§a" + NativeBridge.getMetrics(false)[1])))
                .build())
            .option(Option.<Long>createBuilder()
                .name(Text.literal("Frustum Tests"))
                .description(OptionDescription.of(Text.literal("Total AABB visibility tests performed in SIMD. High value indicates active entity/DH culling.")))
                .binding(0L, () -> NativeBridge.getMetrics(false)[2], val -> {})
                .controller(opt -> dev.isxander.yacl3.api.controller.LongFieldControllerBuilder.create(opt)
                    .formatValue(val -> Text.literal("§b" + NativeBridge.getMetrics(false)[2])))
                .build())
            .build();
    }

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

    // Color code for mixin timing entries.
    private static String mixinBlameColor(long ms) {
        if (ms > 100) return "§c";
        if (ms > 30) return "§e";
        return "§a";
    }

    // Builds an ASCII bar like [████████░░░░] for the given percentage.
    private static String buildAsciiBar(float pct) {
        int filled = Math.clamp(Math.round(pct / 100f * 20), 0, 20);
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