package com.alexxiconify.rustmc;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;

public class PreLaunchHandler implements PreLaunchEntrypoint {
    public static final Logger LOGGER = LogManager.getLogger("Rust-MC/PreLaunch");

    // Stage flags + timestamps set by the Log4j appender as game-load milestones are logged
    static volatile boolean stageDatafixer = false;
    static volatile boolean stageResources = false;
    static volatile boolean stageSound     = false;
    static volatile boolean stageGameReady = false;
    static volatile long tsDatafixer = 0;
    static volatile long  tsResources = 0;
    static volatile long  tsSound = 0;

    @Override
    public void onPreLaunch() {
        configureParallelism();
        installLiveAppender(); // must be before ELB thread so it captures events immediately
        if (isWindows() && FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            com.iafenvoy.elb.gui.PreLaunchWindow.display();
            startModLoadingProgressThread();
        }
        installSpamFilter();
    }

    // ─── Parallelism Config ─────────────────────────────────────────────────

    private static void configureParallelism() {
        int cores   = Runtime.getRuntime().availableProcessors();
        int workers = Math.max(1, cores - 1);
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(workers));
        LOGGER.info("[Rust-MC] ForkJoin parallelism → {} (of {} cores)", workers, cores);
    }

    // ─── Live Log4j Appender ────────────────────────────────────────────────

    /**
     * Attaches a lightweight appender to the root logger.
     * It increments atomic counters as Fabric logs mixin / mod loading events.
     * This is far more reliable than reading the log file because it fires on
     * the logging thread in real time, before log4j flushes to disk.
     */
    private static void installLiveAppender() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();

            @SuppressWarnings("java:S4830")
            AbstractAppender counter = new AbstractAppender("RustMCElbCounter", null,
                    org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout(), true,
                    org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY) {
                @Override
                public void append(LogEvent event) {
                    if (event == null || event.getMessage() == null) return;
                    String msg = event.getMessage().getFormattedMessage();
                    // Stage signals visible after async appender starts (post preLaunch)
                    if (!stageDatafixer && msg.contains("Datafixer Bootstrap")) {
                        stageDatafixer = true; tsDatafixer = System.currentTimeMillis();
                    } else if (!stageResources && msg.startsWith("Reloading ResourceManager:")) {
                        stageResources = true; tsResources = System.currentTimeMillis();
                    } else if (!stageSound && msg.contains("Sound engine started")) {
                        stageSound = true; tsSound = System.currentTimeMillis();
                    } else if (!stageGameReady && msg.contains("Game took")) {
                        stageGameReady = true;
                    }
                }
            };
            counter.start();
            config.addAppender(counter);
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(counter);
            ctx.updateLoggers();
        } catch (Exception | NoClassDefFoundError ignored) { // Log4j not wired at preLaunch
        }
    }

    // ─── ELB Progress Thread ────────────────────────────────────────────────

    /**
     * Drives the ELB bar using live Log4j event counters (not file I/O).
     *
     * Phase   0-20%: preLaunch / mod discovery (time-ramped)
     * Phase  20-80%: mixin transforms (MIXIN_EVENTS counter)
     * Phase  80-95%: mod init ("Loaded N mods" seen)
     * Phase  95-99%: game world prepare
     */
    private static void startModLoadingProgressThread() {
        Thread.ofVirtual().name("rustmc-elb-progress").start(() -> {
            int modCount = FabricLoader.getInstance().getAllMods().size();
            long startMs = System.currentTimeMillis();
            int last = -1;
            long deadline = startMs + 180_000L;

            while (System.currentTimeMillis() < deadline) {
                int progress = computeProgress(startMs);
                if (progress != last) {
                    last = progress;
                    com.iafenvoy.elb.gui.PreLaunchWindow.updateProgress(progress, progressLabel(progress, modCount));
                }
                try { Thread.sleep(200); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        });
    }

    /**
     * Stage-based progress using log signals that fire AFTER the async appender starts.
     * All stages are also time-capped so the bar always advances even on slow machines.
     *   0-25%  : Datafixer / bootstrap (done when stageDatafixer seen, or ~7 s elapsed)
     *  25-60%  : Mixin application / class loading (until stageResources)
     *  60-80%  : Resource pack loading (until stageSound)
     *  80-95%  : Asset stitching / sound init (until stageGameReady)
     *  95-99%  : Waiting for game loop to take over
     */
    private static int computeProgress(long startMs) {
        if (stageGameReady)  return 96;
        if (stageSound)      return timeRamp(80, 95, tsSound,     5_000);
        if (stageResources)  return timeRamp(60, 80, tsResources, 8_000);
        if (stageDatafixer)  return timeRamp(25, 60, tsDatafixer, 15_000);
        long elapsed = System.currentTimeMillis() - startMs;
        return (int) Math.min(24, 5 + elapsed / 417);
    }

    /** Linearly interpolates from lo to hi over durationMs after the current stage started. */
    private static int timeRamp(int lo, int hi, long stageStartMs, long durationMs) {
        long elapsed = System.currentTimeMillis() - stageStartMs;
        return lo + (int) Math.min(hi - lo - 1, (hi - lo) * elapsed / durationMs);
    }

    private static String progressLabel(int pct, int modCount) {
        if (pct >= 95) return "Starting...";
        if (pct >= 80) return "Loading sounds & assets...";
        if (pct >= 60) return "Loading resource packs...";
        if (pct >= 25) return "Applying mixins \u2014 " + modCount + " mods...";
        return "Bootstrapping JVM...";
    }

    // ─── Spam Filter ────────────────────────────────────────────────────────

    private static void installSpamFilter() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            AbstractFilter filter = new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    if (event == null || event.getMessage() == null) return Result.NEUTRAL;
                    return shouldFilter(event.getMessage().getFormattedMessage(), event.getLevel())
                        ? Result.DENY : Result.NEUTRAL;
                }
            };
            filter.start();
            // Cast to AbstractAppender — the Appender interface has no addFilter
            config.getAppenders().forEach((name, appender) -> {
                if (!name.equals("RustMCElbCounter") && appender instanceof org.apache.logging.log4j.core.appender.AbstractAppender aa) {
                    aa.addFilter(filter);
                }
            });
            ctx.updateLoggers();
        } catch (Exception | NoClassDefFoundError ignored) { // Log4j not wired at preLaunch
        }
    }

    private static final String[] SPAM_PATTERNS = {
        "Checking for updates", "Incompatible with", "Redirecting Mixin",
        "Reference map", "Force-disabling mixin", "Force disabled MC-",
        "Quick reload listener", "Reloading texture", "No refMap loaded",
        "Mixin transformation of"
    };

    private static boolean shouldFilter(String content, Level level) {
        if (RustMC.CONFIG == null || !RustMC.CONFIG.isSilenceLogs()) return false;
        if (level.isLessSpecificThan(Level.WARN)) {
            if (content.contains("Loading") && content.contains("mod") && !content.contains("rust-mc")) return true;
            if (content.startsWith("\t- ")) return true;
            for (String pattern : SPAM_PATTERNS) {
                if (content.contains(pattern)) return true;
            }
        }
        return false;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
