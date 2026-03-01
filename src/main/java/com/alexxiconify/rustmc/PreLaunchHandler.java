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

import java.util.concurrent.atomic.AtomicInteger;

public class PreLaunchHandler implements PreLaunchEntrypoint {
    public static final Logger LOGGER = LogManager.getLogger("Rust-MC/PreLaunch");

    // Live counters updated by the Log4j appender in real time
    static final AtomicInteger MIXIN_EVENTS   = new AtomicInteger(0);
    static final AtomicInteger LOADED_EVENTS  = new AtomicInteger(0); // "Loaded X mods"
    static volatile boolean    gameInitSeen = false;

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

            @SuppressWarnings("java:S4830") // SonarLint false positive on anonymous Log4j class
            AbstractAppender counter = new AbstractAppender("RustMCElbCounter", null,
                    org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout(), true,
                    org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY) {
                @Override
                public void append(LogEvent event) {
                    if (event == null || event.getMessage() == null) return;
                    String msg = event.getMessage().getFormattedMessage();
                    if (msg.contains("[Mixin]") || msg.contains("Applying mixin") || msg.contains("mixin.")) {
                        MIXIN_EVENTS.incrementAndGet();
                    } else if (msg.contains("Loaded ") && msg.contains("mod")) {
                        LOADED_EVENTS.incrementAndGet();
                    } else if (msg.contains("Preparing spawn area") || msg.contains("Time elapsed:")) {
                        gameInitSeen = true;
                    }
                }
            };
            counter.start();
            config.addAppender(counter);
            // Cast to core Logger to access the single-arg addAppender overload
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(counter);
            ctx.updateLoggers();
        } catch (Exception | NoClassDefFoundError ignored) { // Log4j not yet wired; skip
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
            // Each mod on average fires ~3 mixin log lines; use that as expected max
            int expectedMixins = Math.max(1, modCount * 3);
            long startMs = System.currentTimeMillis();
            AtomicInteger last = new AtomicInteger(-1);
            long deadline = startMs + 120_000L;

            while (System.currentTimeMillis() < deadline) {
                int progress = computeProgress(expectedMixins, startMs);
                if (progress != last.getAndSet(progress)) {
                    String label = progressLabel(progress, modCount);
                    com.iafenvoy.elb.gui.PreLaunchWindow.updateProgress(progress, label);
                }
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    private static int computeProgress(int expectedMixins, long startMs) {
        if (gameInitSeen)           return 95;
        if (LOADED_EVENTS.get() > 0) return 82;

        int mixins = MIXIN_EVENTS.get();
        if (mixins > 0) {
            // Scale 20→80% based on mixin count vs expected
            return 20 + (int) Math.min(60, (long) mixins * 60 / expectedMixins);
        }
        // Time-based ramp 0→20% in first ~30 s (before first mixin event)
        long elapsed = System.currentTimeMillis() - startMs;
        return (int) Math.min(18, elapsed / 1666);
    }

    private static String progressLabel(int pct, int modCount) {
        if (pct >= 95) return "Starting world...";
        if (pct >= 82) return "Initializing " + modCount + " mods...";
        if (pct >= 20) return "Applying mixin transforms... (" + MIXIN_EVENTS.get() + ")";
        return "Discovering mods... (" + modCount + " found)";
    }

    // ─── Spam Filter ────────────────────────────────────────────────────────

    private static void installSpamFilter() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            config.addFilter(new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    if (event == null || event.getMessage() == null) return Result.NEUTRAL;
                    return shouldFilter(event.getMessage().getFormattedMessage(), event.getLevel())
                        ? Result.DENY : Result.NEUTRAL;
                }
            });
            ctx.updateLoggers();
        } catch (Exception | NoClassDefFoundError ignored) { // Log4j not yet fully initialized
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
