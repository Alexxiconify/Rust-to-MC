package com.alexxiconify.rustmc.mixin;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractLogger.class, remap = false)
public abstract class LoggingMixin {
    @Inject(method = "logMessage(Ljava/lang/String;Lorg/apache/logging/log4j/Level;Lorg/apache/logging/log4j/Marker;Lorg/apache/logging/log4j/message/Message;Ljava/lang/Throwable;)V", at = @At("HEAD"), cancellable = true)
    private void onLog(String fqcn, Level level, Marker marker, Message message, Throwable t, CallbackInfo ci) {
        if (level.isLessSpecificThan(Level.INFO)) return;

        String content = message.getFormattedMessage();
        
        // Filter out common startup spam from various mods
        if (content.contains("Loading") && content.contains("mod") && !content.contains("rust-mc") && level.isLessSpecificThan(Level.WARN)) {
            ci.cancel();
            return;
        }
        
        // Filter out specific known spammy mods or patterns
        if ((content.contains("Checking for updates") || content.contains("Incompatible with") || content.contains("Redirecting Mixin")) && level.isLessSpecificThan(Level.WARN)) {
            ci.cancel();
        }
    }
}
