package com.alexxiconify.rustmc.config;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModMenuIntegration implements ModMenuApi {
    private static final String YES = "§aYES";
    private static final String NO  = "§7NO";
    private static final long METRICS_REFRESH_INTERVAL_MS = 100L;
    private static long lastMetricsRefreshMs;
    private static long[] cachedMetrics = new long[] { 0L, 0L, 0L, 0L, 0L };

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            RustMC.Config cfg = RustMC.CONFIG;
            return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Rust to MC"))
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Settings"))
                    .tooltip(Text.literal("Status and Configuration"))
                    .options(buildStatusOptions())
                    .options(buildUnifiedOptions(cfg))
                    .build())
                .category(buildBlameCategory())
                .save(RustMC::saveConfig)
                .build()
                .generateScreen(parent);
        };
    }

    private List<Option<?>> buildStatusOptions() {
        refreshMetricsCache();
        return List.of(
            Option.<Boolean>createBuilder()
                .name(Text.literal("Native Core Status"))
                .description(OptionDescription.of(Text.literal("READY: Rust library loaded.\nFAILED: Fallback to Java.")))
                .binding(true, NativeBridge::isReady, val -> {})
                .controller(opt -> BooleanControllerBuilder.create(opt)
                    .formatValue(val -> Text.literal(NativeBridge.isReady() ? "§aREADY" : "§cFAILED")))
                .available(false).build(),
            Option.<String>createBuilder()
                .name(Text.literal("Lighting Hook"))
                .binding("unknown", ModMenuIntegration::getLightingStatusText, val -> {})
                .controller(StringControllerBuilder::create)
                .available(false).build(),
            buildDetectOption("Sodium", () -> ModBridge.SODIUM),
            buildDetectOption("Iris", () -> ModBridge.IRIS),
            Option.<String>createBuilder()
                .name(Text.literal("Session Metrics"))
                .description(OptionDescription.of(Text.literal("Total JNI calls / Lighting updates / Chunk ingest")))
                .binding("", () -> "%s / %s / %s".formatted(metricText(0), metricText(1), metricText(3)), val -> {})
                .controller(StringControllerBuilder::create)
                .available(false).build()
        );
    }

    private static String getLightingStatusText() {
        if (!NativeBridge.isReady()) return "§cDISABLED (Native Not Found)";
        return RustMC.CONFIG.isUseNativeLighting() ? "§aACTIVE" : "§7INACTIVE (RustMC.Config)";
    }

    private static long getCachedMetric(int index) {
        refreshMetricsCache();
        if (index < 0 || index >= cachedMetrics.length) return 0L;
        return cachedMetrics[index];
    }

    private static String metricText(int index) {
        return Long.toString(getCachedMetric(index));
    }

    private static void refreshMetricsCache() {
        long now = System.currentTimeMillis();
        if (now - lastMetricsRefreshMs < METRICS_REFRESH_INTERVAL_MS) {
            return;
        }
        long[] metrics = NativeBridge.getMetrics(false);
        if (metrics.length >= 5) {
            cachedMetrics = metrics;
        } else if (metrics.length >= 3) {
            cachedMetrics = new long[] { metrics[0], metrics[1], metrics[2], 0L, 0L };
        } else {
            cachedMetrics = new long[] { 0L, 0L, 0L, 0L, 0L };
        }
        lastMetricsRefreshMs = now;
    }

    private List<Option<?>> buildUnifiedOptions(RustMC.Config cfg) {
        List<Option<?>> options = new ArrayList<>();
        
        options.add(buildSectionHeader("Native Optimization", "Core native features."));
        options.add(Option.<RustMC.Config.HardwarePreset>createBuilder()
            .name(Text.literal("Hardware Preset"))
            .binding(RustMC.Config.HardwarePreset.MID_RANGE, cfg::getHardwarePreset, cfg::setHardwarePreset)
            .controller(EnumControllerBuilder::create)
            .build());
        options.add(buildBooleanOption("Native Lighting", "Use Rust for lighting updates.", cfg::isUseNativeLighting, v -> cfg.setUseNativeLighting(v != null && v)));
        options.add(buildBooleanOption("DNS Cache", "Speeds up server list pings.", cfg::isEnableDnsCache, v -> cfg.setEnableDnsCache(v != null && v)));
        options.add(Option.<RustMC.Config.DiagnosticMode>createBuilder()
            .name(Text.literal("Diagnostic HUD"))
            .binding(RustMC.Config.DiagnosticMode.HIDDEN, cfg::getDiagnosticMode, v -> cfg.setDiagnosticMode(v != null ? v : RustMC.Config.DiagnosticMode.HIDDEN))
            .controller(EnumControllerBuilder::create)
            .build());
        options.add(buildBooleanOption("Sparkline Graph", "Show frame-time graph on HUD.", cfg::isEnableSparklineGraph, v -> cfg.setEnableSparklineGraph(v != null && v)));

        options.add(buildSectionHeader("Compat & Visuals", "Mod compatibility and visuals."));
        options.add(buildBooleanOption("Fast Load Screen", "Custom loading screen overlay.", cfg::isUseFastLoadingScreen, v -> cfg.setUseFastLoadingScreen(v != null && v)));
        options.add(buildBooleanOption("Particle Culling", "Cull distant particles.", cfg::isEnableParticleCulling, v -> cfg.setEnableParticleCulling(v != null && v)));
        options.add(buildBooleanOption("Sodium Bridge", "Enhanced Sodium integration.", cfg::isBridgeSodium, v -> cfg.setBridgeSodium(v != null && v)));
        options.add(buildBooleanOption("DH Cave Culling", "Cull DH LODs in caves.", cfg::isEnableDhCaveCulling, v -> cfg.setEnableDhCaveCulling(v != null && v)));
        options.add(buildBooleanOption("Silence Logs", "Suppress startup spam.", cfg::isSilenceLogs, v -> cfg.setSilenceLogs(v != null && v)));
        
        return options;
    }

    private Option<Boolean> buildDetectOption(String name, Supplier<Boolean> isDetected) {
        return Option.<Boolean>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal("Whether " + name + " is installed.")))
            .binding(true, isDetected, val -> {})
            .controller(opt -> BooleanControllerBuilder.create(opt)
                .formatValue(val -> Text.literal(Boolean.TRUE.equals(isDetected.get()) ? YES : NO)))
            .available(false)
            .build();
    }

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

    // ── Blame Chart ─────────────────────────────────────────────────────────
    private static final String PCT_FORMAT = "%.1f%%";
    private ConfigCategory buildBlameCategory() {
        var builder = ConfigCategory.createBuilder()
            .name(Text.literal("Blame Chart"))
            .tooltip(Text.literal("Loading phase timings from launcher start to game-ready."));

        List<RustMC.RustMC.BlameLog.Entry> entries = RustMC.RustMC.BlameLog.getEntriesWithGaps();
        if (entries.isEmpty()) {
            addNoDataOption(builder);
        } else {
            long wallClock = RustMC.RustMC.BlameLog.wallClockMs();
            addTotalSummary(builder, RustMC.RustMC.BlameLog.trackedMs(), wallClock);
            addPhaseEntries(builder, entries, wallClock);
            addMixinBreakdown(builder);
        }
        return builder.build();
    }

    private void addNoDataOption(ConfigCategory.Builder builder) {
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

    private static void addPhaseEntries(ConfigCategory.Builder builder, List<RustMC.RustMC.BlameLog.Entry> entries, long wallClock) {
        for (RustMC.RustMC.BlameLog.Entry entry : entries) {
            long dur = entry.durationMs();
            float pct = wallClock > 0 ? (float) dur / wallClock * 100f : 0;
            String bar = buildAsciiBar(pct);
            boolean isGap = entry.phase().startsWith("⚠");
            String color = isGap ? "§c" : blameColor(dur);
            String assessment = isGap
                ? "Untracked time: JVM overhead, driver init, or undetected phases."
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
        Map<String, Long> mixinTimings = com.alexxiconify.rustmc.MixinManager.getGroupTimings();
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
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(mixinTimings.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        for (Map.Entry<String, Long> mEntry : sorted) {
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

    private static String mixinBlameColor(long ms) {
        if (ms > 100) return "§c";
        if (ms > 30) return "§e";
        return "§a";
    }

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




