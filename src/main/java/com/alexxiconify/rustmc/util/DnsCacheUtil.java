package com.alexxiconify.rustmc.util;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;

public final class DnsCacheUtil {
    private DnsCacheUtil() {
    }

    public static boolean isDnsCacheEnabled() {
        return NativeBridge.isReady() && RustMC.CONFIG.isEnableDnsCache();
    }

    public static void persistDnsCache(String reason) {
        if (!isDnsCacheEnabled()) {
            return;
        }
        try {
            NativeBridge.dnsCacheSave();
            RustMC.LOGGER.debug("[Rust-MC] DNS cache persisted on {}.", reason);
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] DNS cache persist failed on {}: {}", reason, e.getMessage());
        }
    }

    public static String extractResolvableHostname(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }
        String trimmed = address.trim();
        String hostname = trimmed.contains(":") ? trimmed.substring(0, trimmed.lastIndexOf(':')) : trimmed;
        if (hostname.isEmpty() || Character.isDigit(hostname.charAt(0))) {
            return "";
        }
        return hostname;
    }
}