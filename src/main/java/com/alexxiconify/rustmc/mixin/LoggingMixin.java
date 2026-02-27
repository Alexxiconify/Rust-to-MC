package com.alexxiconify.rustmc.mixin;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.alexxiconify.rustmc.RustMC;

@Mixin(value = AbstractLogger.class, remap = false)
public abstract class LoggingMixin {

    private static final String[] SPAM_PATTERNS = {
        "Checking for updates", "Incompatible with", "Redirecting Mixin",
        "Reference map", "Force-disabling mixin", "Force disabled MC-",
        "Quick reload listener", "Reloading texture", "No refMap loaded",
        "Mixin transformation of", "Critical injection failure"
    };

    @Inject(
        method = "logMessage(Ljava/lang/String;Lorg/apache/logging/log4j/Level;Lorg/apache/logging/log4j/Marker;Lorg/apache/logging/log4j/message/Message;Ljava/lang/Throwable;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onLog(String fqcn, Level level, Marker marker, Message message, Throwable t, CallbackInfo ci) {
        // Guard: CONFIG may be null very early in loading
        try {
            if (RustMC.CONFIG == null || !RustMC.CONFIG.isSilenceLogs()) return;
        } catch (Throwable ignored) {
            return;
        }

        // Only filter INFO and below (never suppress WARN/ERROR)
        if (level == null || !level.isLessSpecificThan(Level.WARN)) return;
        if (message == null) return;

        String content = message.getFormattedMessage();
        if (content == null) return;

        // Filter common noisy startup patterns from other mods
        if (content.contains("Loading") && content.contains("mod") && !content.contains("rust-mc")) {
            ci.cancel();
            return;
        }
        if (content.startsWith("\t- ")) {
            ci.cancel();
            return;
        }
        for (String pattern : SPAM_PATTERNS) {
            if (content.contains(pattern)) {
                ci.cancel();
                return;
            }
        }
    }
}
