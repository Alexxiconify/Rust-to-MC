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
import com.alexxiconify.rustmc.util.BlameLog;
import org.jspecify.annotations.NonNull;

public class PreLaunchHandler implements PreLaunchEntrypoint {
    public static final Logger LOGGER = LogManager.getLogger("Rust-MC/PreLaunch");

    // Stage flags + timestamps set by the Log4j appender as game-load milestones are logged
    static volatile boolean stageDatafixer = false;
    static volatile boolean stageResources = false;
    static volatile boolean stageSound     = false;
    static volatile boolean stageGameReady = false;
    static volatile boolean stageModInit   = false;
    static volatile long tsDatafixer = 0;
    static volatile long tsResources = 0;
    static volatile long tsSound = 0;
    static volatile long tsModInit = 0;

    @Override
    public void onPreLaunch() {
        BlameLog.begin("PreLaunch / JVM Bootstrap");
        configureParallelism();
        installLiveAppender(); // must be before ELB thread so it captures events immediately
        if (isWindows() && FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            com.iafenvoy.elb.gui.PreLaunchWindow.display();
            startModLoadingProgressThread();
        }
        installSpamFilter();
        // End PreLaunch phase immediately — the next phase (Mixin Application) runs
        // outside our control until the log appender detects Datafixer Bootstrap.
        BlameLog.begin("Mixin Application / Class Loading");
    }

    // ─── Parallelism Config ─────────────────────────────────────────────────

    private static void configureParallelism() {
        int cores   = Runtime.getRuntime().availableProcessors();
        int workers = Math.max(1, cores - 1);
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(workers));

        // Speed up class loading — use parallel class loading if available
        System.setProperty("java.lang.ClassLoader.parallelLockMap", "true");

        // Tune DNS cache TTL for Java's built-in resolver (seconds)
        // Default is 30s for positive, 10s for negative — we extend positive to reduce lookups
        java.security.Security.setProperty("networkaddress.cache.ttl", "300");
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "10");

        // Hint the JIT compiler to compile methods earlier for faster warmup
        System.setProperty("sun.java2d.opengl", "false"); // Avoid Swing GL init overhead for ELB
        System.setProperty("java.awt.headless", "false");  // Allow ELB Swing window

        LOGGER.info("[Rust-MC] ForkJoin parallelism → {} (of {} cores), DNS cache TTL 300s", workers, cores);
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

            AbstractAppender counter = getAbstractAppender ( );
            config.addAppender(counter);
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(counter);
            ctx.updateLoggers();
        } catch (Exception | NoClassDefFoundError ignored) { // Log4j not wired at preLaunch
        }
    }

    private static @NonNull AbstractAppender getAbstractAppender ( ) {
        @SuppressWarnings("java:S4830")
        AbstractAppender counter = new AbstractAppender("RustMCElbCounter", null,
                org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout(), true,
                org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                if (event == null || event.getMessage() == null) return;
                String msg = event.getMessage().getFormattedMessage();
                detectStage(msg);
            }
        };
        counter.start();
        return counter;
    }

    // ─── Stage Detection ───────────────────────────────────────────────────

    /**
     * Detects loading phases from log messages and records them in BlameLog.
     * Delegates to focused helpers to keep cognitive complexity low.
     */
    private static void detectStage(String msg) {
        if (detectPerModEntrypoint(msg)) return;
        if (detectEarlyPhases(msg)) return;
        detectMajorMilestones(msg);
    }

    /**
     * Detects per-mod entrypoint init.
     * Fabric logs: "Invoking entrypoint 'main' for mod 'sodium'" etc.
     */
    private static boolean detectPerModEntrypoint(String msg) {
        if (stageDatafixer) return false;
        if (!msg.startsWith("Invoking entrypoint")) return false;

        int idx = msg.indexOf("for mod '");
        if (idx < 0) return false;
        String rest = msg.substring(idx + 9);
        int end = rest.indexOf('\'');
        String modId = end > 0 ? rest.substring(0, end) : rest;
        BlameLog.begin("Entrypoint: " + modId);
        return true;
    }

    /** Detects Fabric mod discovery, mixin bootstrap, and environment setup. */
    private static boolean detectEarlyPhases(String msg) {
        if (stageDatafixer) return false;

        if (!stageModInit && msg.contains("Loading") && msg.contains("mods:")) {
            stageModInit = true;
            tsModInit = System.currentTimeMillis();
            BlameLog.begin("Fabric Mod Discovery");
            return true;
        }
        if (stageModInit && msg.contains("Loaded") && msg.contains("mod")) {
            BlameLog.begin("Post-Mod-Init Wiring");
            return true;
        }
        if (msg.contains("SpongePowered MIXIN")) {
            BlameLog.begin("Mixin Bootstrap");
            return true;
        }
        if (msg.contains("Mixin Environment")) {
            BlameLog.begin("Mixin Environment Setup");
            return true;
        }
        return false;
    }

    /** Detects the four major milestones: Datafixer, Resources, Sound, Game Ready. */
    private static void detectMajorMilestones(String msg) {
        if (!stageDatafixer && msg.contains("Datafixer Bootstrap")) {
            stageDatafixer = true;
            tsDatafixer = System.currentTimeMillis();
            BlameLog.begin("Datafixer Bootstrap");
        } else if (!stageResources && msg.startsWith("Reloading ResourceManager:")) {
            stageResources = true;
            tsResources = System.currentTimeMillis();
            BlameLog.begin("Resource Loading");
        } else if (!stageSound && msg.contains("Sound engine started")) {
            stageSound = true;
            tsSound = System.currentTimeMillis();
            BlameLog.begin("Sound & Asset Stitching");
        } else if (!stageGameReady && msg.contains("Game took")) {
            stageGameReady = true;
            BlameLog.begin("Game Ready");
            BlameLog.end();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[Rust-MC] {}", BlameLog.summary());
            }
        }
    }

    // ─── ELB Progress Thread ────────────────────────────────────────────────

    /**
     * Drives the ELB bar using live Log4j event counters (not file I/O).
     * <p>
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

            while (System.currentTimeMillis() < deadline && !stageGameReady) {
                int progress = computeProgress(startMs);
                if (progress != last) {
                    last = progress;
                    com.iafenvoy.elb.gui.PreLaunchWindow.updateProgress(progress, progressLabel(progress, modCount));
                }
                java.util.concurrent.locks.LockSupport.parkNanos(200_000_000L); // 200ms
                if (Thread.currentThread().isInterrupted()) return;
            }
            // Push to 100% when game is ready so the user sees completion
            if (stageGameReady) {
                com.iafenvoy.elb.gui.PreLaunchWindow.updateProgress(100, "Done!");
            }
        });
    }

    /**
     * Stage-based progress using log signals that fire AFTER the async appender starts.
     * All stages are time-capped so the bar always advances even on slow machines.
     *   0-40%  : JVM bootstrap / mixin application (time-ramped, caps at 40 before datafixer)
     *  40-65%  : Datafixer bootstrap (until stageResources)
     *  65-82%  : Resource pack loading (until stageSound)
     *  82-96%  : Asset stitching / sound init (until stageGameReady)
     *  96-99%  : Waiting for game loop to take over
     */
    private static int computeProgress(long startMs) {
        if (stageGameReady)  return 97;
        if (stageSound)      return timeRamp(82, 96, tsSound,     5_000);
        if (stageResources)  return timeRamp(65, 82, tsResources, 10_000);
        if (stageDatafixer)  return timeRamp(40, 65, tsDatafixer, 18_000);
        // Pre-datafixer ramp: 3% at t=0, advances ~3% per second, caps at 39
        long elapsed = System.currentTimeMillis() - startMs;
        return (int) Math.min(39, 3 + elapsed / 300);
    }

    /** Linearly interpolates from lo to hi over durationMs after the current stage started. */
    private static int timeRamp(int lo, int hi, long stageStartMs, long durationMs) {
        long elapsed = System.currentTimeMillis() - stageStartMs;
        return lo + (int) Math.min((hi - lo - 1), ((long) hi - (long) lo) * elapsed / durationMs);
    }

    private static String progressLabel(int pct, int modCount) {
        if (pct >= 96) return "Starting game...";
        if (pct >= 82) return "Loading sounds & assets...";
        if (pct >= 65) return "Loading resource packs...";
        if (pct >= 40) return "Building datafixer schemas...";
        if (pct >= 15) return "Applying mixins — " + modCount + " mods...";
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
        if ( !RustMC.CONFIG.isSilenceLogs() ) return false;
        if (level.isLessSpecificThan(Level.INFO)) {
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
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(java.util.Locale.ROOT).contains("windows");
    }
}