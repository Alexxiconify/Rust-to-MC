package com.alexxiconify.rustmc;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.LogEvent;

public class PreLaunchHandler implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            config.addFilter(new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    if (event == null || event.getMessage() == null) return Result.NEUTRAL;
                    
                    String content = event.getMessage().getFormattedMessage();
                    Level level = event.getLevel();
                    
                    // Filter out common startup spam from various mods
                    if (content.contains("Loading") && content.contains("mod") && !content.contains("rust-mc") && level.isLessSpecificThan(Level.WARN)) {
                        return Result.DENY;
                    }
                    
                    // Filter out specific known spammy mods or patterns
                    if ((content.contains("Checking for updates") || content.contains("Incompatible with") || content.contains("Redirecting Mixin")) && level.isLessSpecificThan(Level.WARN)) {
                        return Result.DENY;
                    }
                    
                    return Result.NEUTRAL;
                }
            });
            ctx.updateLoggers();
        } catch (Exception | NoClassDefFoundError e) {
            // Log4j might not be fully initialized or visible yet
        } 
    }
}
