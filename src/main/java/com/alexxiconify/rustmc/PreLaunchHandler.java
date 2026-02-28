package com.alexxiconify.rustmc;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.LogEvent;

public class PreLaunchHandler implements PreLaunchEntrypoint {
    public static final Logger LOGGER = LogManager.getLogger("Rust-MC/PreLaunch");

    @Override
    public void onPreLaunch() {
        if (isWindows() && net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            com.iafenvoy.elb.gui.PreLaunchWindow.display();
        }
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            config.addFilter(new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    if (event == null || event.getMessage() == null) return Result.NEUTRAL;
                    return shouldFilter(event.getMessage().getFormattedMessage(), event.getLevel()) ? Result.DENY : Result.NEUTRAL;
                }
            });
            ctx.updateLoggers();
        } catch (Exception | NoClassDefFoundError e) {
            // Log4j might not be fully initialized or visible yet
        } 
    }

    private static final String[] SPAM_PATTERNS = {
        "Checking for updates", "Incompatible with", "Redirecting Mixin",
        "Reference map", "Force-disabling mixin", "Force disabled MC-",
        "Quick reload listener", "Reloading texture", "No refMap loaded",
        "Mixin transformation of", "Critical injection failure"
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
