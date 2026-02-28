package com.alexxiconify.rustmc;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PreLaunchHandler implements PreLaunchEntrypoint {
    public static final Logger LOGGER = LogManager.getLogger("Rust-MC/PreLaunch");

    @Override
    public void onPreLaunch() {
        configureParallelism();
        if (isWindows() && FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            com.iafenvoy.elb.gui.PreLaunchWindow.display();
            startModLoadingProgressThread();
        }
        installLogFilter();
    }

    /**
     * Maximises JVM-level parallelism before Minecraft starts.
     * Sets ForkJoinPool common pool size to (cores - 1) so chunk loading,
     * parallel stream ops, and Fabric's class loading all use more cores.
     * One core is left free for the render/main thread.
     */
    private static void configureParallelism() {
        int cores = Runtime.getRuntime().availableProcessors();
        int workers = Math.max(1, cores - 1);
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
            String.valueOf(workers));
        // Parallel class loading (reduces startup time on multicore CPUs)
        System.setProperty("jdk.classFileVersionDelegation", "true");
        LOGGER.info("[Rust-MC] ForkJoin parallelism set to {} (of {} cores)", workers, cores);
    }

    private static void installLogFilter() {
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

    /**
     * Virtual thread that reads latest.log to count loaded mods and drives the
     * ELB progress bar from 0 to 100% instead of staying stuck at 0%.
     */
    private static void startModLoadingProgressThread() {
        Thread.ofVirtual().name("rustmc-elb-progress").start(() -> {
            Path logFile = Path.of("logs/latest.log");
            int modCount = FabricLoader.getInstance().getAllMods().size();
            AtomicInteger lastProgress = new AtomicInteger(-1);
            long deadline = System.currentTimeMillis() + 90_000L;

            while (System.currentTimeMillis() < deadline) {
                int progress = readModProgress(logFile, modCount);
                if (progress != lastProgress.getAndSet(progress)) {
                    com.iafenvoy.elb.gui.PreLaunchWindow.updateProgress(progress,
                        "Loading mods... " + (progress * modCount / 100) + " / " + modCount);
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    private static int readModProgress(Path logFile, int modCount) {
        if (!Files.exists(logFile) || modCount == 0) return 0;
        try {
            List<String> lines = Files.readAllLines(logFile);
            int loaded = parseLoadedMods(lines);
            return Math.min(100, loaded * 100 / modCount);
        } catch (IOException ignored) { // log file locked or unreadable; skip tick
            return 0;
        }
    }

    private static int parseLoadedMods(List<String> lines) {
        int loaded = 0;
        for (String line : lines) {
            if (!line.contains("Loading") || !line.contains("mod") || !line.contains("(s)")) continue;
            loaded = extractNumber(line, loaded);
        }
        return loaded;
    }

    private static int extractNumber(String line, int fallback) {
        String[] parts = line.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i + 1].startsWith("mod")) {
                try {
                    return Integer.parseInt(parts[i].replaceAll("\\D", ""));
                } catch (NumberFormatException ignored) { // non-numeric; skip
                }
            }
        }
        return fallback;
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

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
