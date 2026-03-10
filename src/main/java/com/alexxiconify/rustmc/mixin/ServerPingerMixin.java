package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.NetworkingBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optimizes server list ping times by pre-resolving DNS via the Rust cached resolver.
 * Each server gets its own virtual thread for maximum parallelism — all DNS lookups
 * happen concurrently instead of sequentially.
 */
@Mixin(MultiplayerServerListPinger.class)
public class ServerPingerMixin {

    @Inject(method = "add", at = @At("HEAD"), require = 0)
    private void prewarmDns( ServerInfo entry , Runnable saver , Runnable pingCallback , NetworkingBackend backend , CallbackInfo ci ) {
        if (!NativeBridge.isReady() || !RustMC.CONFIG.isEnableDnsCache()) return;
        if (entry == null || entry.address == null || entry.address.isEmpty()) return;

        String address = entry.address.trim();
        // Strip port if present for DNS resolution
        String hostname = address.contains(":") ? address.substring(0, address.lastIndexOf(':')) : address;
        if (hostname.isEmpty() || Character.isDigit(hostname.charAt(0))) return;

        // Fire-and-forget DNS pre-warm on a virtual thread — each server resolves in parallel
        Thread.ofVirtual().name("rustmc-dns-" + hostname).start(() -> {
            try {
                NativeBridge.dnsResolve(hostname);
            } catch (Exception ignored) {
                // DNS prewarm is best-effort
            }
        });
    }
}