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
    static volatile boolean stageWindowOpen = false;
    static volatile long tsDatafixer = 0;
    static volatile long tsResources = 0;
    static volatile long tsSound = 0;
    static volatile long tsModInit = 0;
    static volatile long tsWindowOpen = 0;
    @Override
    public void onPreLaunch() {
        BlameLog.begin("PreLaunch / JVM Bootstrap");
        configureParallelism();
        triggerEarlyNativeLoad();
        installLiveAppender(); // must be before ELB thread so it captures events immediately
        if (isWindows() && FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            PreLaunchWindow.display();
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
        java.security.Security.setProperty("networkaddress.cache.ttl", "300");
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "10");
        // JIT compilation threshold: compile methods after fewer invocations for faster warmup
        // Default is 10000; lowering to 2000 makes hot methods compile ~5x sooner
        System.setProperty("sun.java2d.opengl", "false"); // Avoid Swing GL init overhead for ELB
        System.setProperty("java.awt.headless", "false");  // Allow ELB Swing window
        // Hint Datafixer to use parallel execution for schema building
        System.setProperty("datafixer.parallelism", String.valueOf(workers));
        LOGGER.info("[Rust-MC] ForkJoin parallelism → {} (of {} cores), DNS cache TTL 300s", workers, cores);
    }
    // ─── Early Native Library Load ──────────────────────────────────────────
    //
     // Triggers NativeBridge's static initializer on a platform daemon thread so the native
     // library extraction + System.load() overlaps with mixin application.
     // By the time onInitialize() runs, the library is already loaded.
    private static void triggerEarlyNativeLoad() {
        Thread.ofPlatform().daemon(true).name("rustmc-native-preload").start(() -> {
            try {
                // Touching NativeBridge.class triggers its static {} block which loads the .dll/.so
                Class.forName("com.alexxiconify.rustmc.NativeBridge");
            } catch (Exception e) {
                LOGGER.warn("[Rust-MC] Early native load failed (will retry at init): {}", e.getMessage());
            }
        });
    }
    // ─── Live Log4j Appender ────────────────────────────────────────────────
    //
     // Attaches a lightweight appender to the root logger.
     // It increments atomic counters as Fabric logs mixin / mod loading events.
     // This is far more reliable than reading the log file because it fires on
     // the logging thread in real time, before log4j flushes to disk.
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
    //
     // Detects loading phases from log messages and records them in BlameLog.
     // Delegates to focused helpers to keep cognitive complexity low.
    private static void detectStage(String msg) {
        if (detectPerModEntrypoint(msg)) return;
        if (detectEarlyPhases(msg)) return;
        detectMajorMilestones(msg);
    }
    //
     // Detects per-mod entrypoint init and mod initializing messages.
     // Fabric logs: "Invoking entrypoint 'main' for mod 'sodium'" etc.
     // Also detects "[ModName] Initializing..." patterns.
    private static boolean detectPerModEntrypoint(String msg) {
        if (stageGameReady) return false; // Stop tracking after game is ready
        if (msg.startsWith("Invoking entrypoint")) {
            int idx = msg.indexOf("for mod '");
            if (idx < 0) return false;
            String rest = msg.substring(idx + 9);
            int end = rest.indexOf('\'');
            String modId = end > 0 ? rest.substring(0, end) : rest;
            BlameLog.begin("Entrypoint: " + modId);
            return true;
        }
        // Track mod init messages like "[Rust-MC] Initializing..." or "[Sodium] Initializing"
        // Don't create a new phase for every "Initializing" — just extend the current one
        return false;
    }
    //Detects Fabric mod discovery, mixin bootstrap, entrypoint init, and environment setup. // /
    private static boolean detectEarlyPhases(String msg) {
        if (stageGameReady) return false;
        if (!stageModInit && msg.contains("Loading") && msg.contains("mods")) {
            stageModInit = true;
            tsModInit = System.currentTimeMillis();
            BlameLog.begin("Fabric Mod Discovery");
            return true;
        }
        if (stageModInit && !stageDatafixer && (msg.contains("Loaded") || msg.contains("loaded")) && msg.contains("mod")) {
            BlameLog.begin("Post-Mod-Init Wiring");
            return true;
        }
        if (msg.contains("SpongePowered MIXIN") || msg.contains("Mixin subsystem")) {
            BlameLog.begin("Mixin Bootstrap");
            return true;
        }
        if (msg.contains("Mixin Environment") || msg.contains("mixin.transformer")) {
            BlameLog.begin("Mixin Environment Setup");
            return true;
        }
        // Track the big gap between DFU and resource loading — this is mod entrypoint init
        if (stageDatafixer && !stageResources && msg.contains("HTTP server")) {
            BlameLog.begin("Mod Entrypoint Init");
            return true;
        }
        return false;
    }
    //Detects the major milestones: Datafixer, Resources, Sound, Window, Game Ready. // /
    private static void detectMajorMilestones(String msg) {
        if (detectDatafixer(msg)) return;
        if (detectResources(msg)) return;
        if (detectSound(msg)) return;
        if (detectWindow(msg)) return;
        detectGameReady(msg);
    }
    private static boolean detectDatafixer(String msg) {
        if (stageDatafixer) return false;
        // Specific DFU patterns only — "Bootstrap" alone is too generic
        if (msg.contains("Datafixer optimizations") || msg.contains("DataFixerUpper")
                || msg.contains("DFU schema build") || msg.contains("datafixer")
                || (msg.contains("Datafixer") && msg.contains("took"))) {
            stageDatafixer = true;
            tsDatafixer = System.currentTimeMillis();
            BlameLog.begin("Datafixer / Registry Bootstrap");
            return true;
        }
        return false;
    }
    private static boolean detectResources(String msg) {
        if (stageResources) return false;
        if (msg.contains("Reloading ResourceManager") || msg.contains("resource packs")
                || msg.contains("Resource reload") || msg.contains("Loading resource")) {
            stageResources = true;
            tsResources = System.currentTimeMillis();
            BlameLog.begin("Resource Loading");
            return true;
        }
        return false;
    }
    private static boolean detectSound(String msg) {
        if (stageSound) return false;
        if (msg.contains("Sound engine") || msg.contains("sound system")
                || msg.contains("OpenAL") || msg.contains("Sound Physics")) {
            stageSound = true;
            tsSound = System.currentTimeMillis();
            BlameLog.begin("Sound & Asset Stitching");
            return true;
        }
        return false;
    }
    private static boolean detectWindow(String msg) {
        if (stageWindowOpen) return false;
        if (msg.contains("GLFW") || msg.contains("Backend library")
                || msg.contains("GL version") || msg.contains("Renderer:")
                || msg.contains("OpenGL")) {
            stageWindowOpen = true;
            tsWindowOpen = System.currentTimeMillis();
            BlameLog.begin("Window / GL Init");
            return true;
        }
        return false;
    }
    private static void detectGameReady(String msg) {
        if (stageGameReady) return;
        if (msg.contains("Game took") || msg.contains("game started")) {
            stageGameReady = true;
            BlameLog.begin("Game Ready");
            BlameLog.end();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[Rust-MC] {}", BlameLog.summary());
            }
        }
    }
    // ─── ELB Progress Thread ────────────────────────────────────────────────
    //
     // Drives the ELB bar using live Log4j event counters (not file I/O).
     // <p>
     // Phase   0-20%: preLaunch / mod discovery (time-ramped)
     // Phase  20-80%: mixin transforms (MIXIN_EVENTS counter)
     // Phase  80-95%: mod init ("Loaded N mods" seen)
     // Phase  95-99%: game world prepare
    private static void startModLoadingProgressThread() {
        Thread.ofPlatform().daemon(true).name("rustmc-elb-progress").start(() -> {
            int modCount = FabricLoader.getInstance().getAllMods().size();
            long startMs = System.currentTimeMillis();
            int last = -1;
            long deadline = startMs + 180_000L;
            while (System.currentTimeMillis() < deadline && !stageGameReady) {
                int progress = computeProgress(startMs);
                if (progress != last) {
                    last = progress;
                    PreLaunchWindow.updateProgress(progress, progressLabel(progress, modCount));
                }
                java.util.concurrent.locks.LockSupport.parkNanos(100_000_000L); // 100ms — smooth ELB bar
                if (Thread.currentThread().isInterrupted()) return;
            }
            // Push to 100% when game is ready so the user sees completion
            if (stageGameReady) {
                PreLaunchWindow.updateProgress(100, "Done!");
            }
        });
    }
    //
     // Dual-strategy progress: time-based baseline + milestone jumps.
     // <p>
     // Time baseline: advances smoothly from 3% to 97% over ~40s so the bar NEVER stalls.
     // Milestone jumps: when a log-detected stage fires, ensure progress is at least
     // the expected percentage for that stage (prevents going backwards).
     // <p>
     // Expected ~35s total:
     //   ~0-12s  (0-35%): mixin application / class loading
     //  ~12-18s (35-55%): datafixer / registry bootstrap
     //  ~18-25s (55-72%): resource loading
     //  ~25-30s (72-85%): sound + asset stitching
     //  ~30-35s (85-97%): window init + splash screen
    private static int computeProgress(long startMs) {
        long elapsed = System.currentTimeMillis() - startMs;
        // Time-based baseline: logarithmic curve that starts fast and slows down
        // Reaches ~35% at 12s, ~55% at 18s, ~72% at 25s, ~90% at 35s, ~97% at 45s
        // Formula: 97 / (1 - e^(-elapsed/15000))
        double timePct = 97.0 / (1.0 - Math.exp(-elapsed / 15000.0));
        int timeProgress = Math.clamp((long) timePct, 3, 97);
        // Milestone-based minimum: if a stage has been detected, ensure we're at least there
        int milestoneMin = 0;
        if (stageGameReady)  milestoneMin = 95;
        else if (stageWindowOpen) milestoneMin = Math.max(85, timeRamp(85, 95, tsWindowOpen, 8_000));
        else if (stageSound)      milestoneMin = Math.max(72, timeRamp(72, 85, tsSound, 5_000));
        else if (stageResources)  milestoneMin = Math.max(55, timeRamp(55, 72, tsResources, 10_000));
        else if (stageDatafixer)  milestoneMin = Math.max(35, timeRamp(35, 55, tsDatafixer, 18_000));
        // Return the higher of time-based and milestone-based
        return Math.clamp(Math.max(timeProgress, milestoneMin), 3, 99);
    }
    //Linearly interpolates from lo to hi over durationMs after the current stage started. // /
    private static int timeRamp(int lo, int hi, long stageStartMs, long durationMs) {
        long elapsed = System.currentTimeMillis() - stageStartMs;
        return lo + (int) Math.min((hi - lo - 1), ((long) hi - (long) lo) / elapsed / durationMs);
    }
    private static String progressLabel(int pct, int modCount) {
        // Prefer milestone-based labels when detected
        if (stageGameReady)  return "Starting game...";
        if (stageWindowOpen) return "Initializing window & OpenGL...";
        if (stageSound)      return "Loading sounds & assets...";
        if (stageResources)  return "Loading resource packs...";
        if (stageDatafixer)  return "Building datafixer schemas...";
        if (stageModInit)    return "Applying mixins — " + modCount + " mods...";
        // Fallback to percentage-based labels
        if (pct >= 85) return "Initializing subsystems...";
        if (pct >= 55) return "Loading assets...";
        if (pct >= 35) return "Building registries...";
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