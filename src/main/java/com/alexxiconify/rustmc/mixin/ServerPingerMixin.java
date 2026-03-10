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
 * When the server pinger starts connecting, we pre-warm the system DNS cache by
 * resolving the hostname through Rust (which caches results for 5 minutes).
 * This eliminates repeated DNS lookups when the server list refreshes.
 */
@Mixin(MultiplayerServerListPinger.class)
public class ServerPingerMixin {

    @Inject(method = "add", at = @At("HEAD"), require = 0)
    private void prewarmDns( ServerInfo entry , Runnable saver , Runnable pingCallback , NetworkingBackend backend , CallbackInfo ci ) {
        if (!NativeBridge.isReady() || entry == null || entry.address == null) return;

        String address = entry.address;
        // Strip port if present for DNS resolution
        String hostname = address.contains(":") ? address.substring(0, address.lastIndexOf(':')) : address;

        // Fire-and-forget DNS pre-warm on a virtual thread
        Thread.ofVirtual().name("rustmc-dns-prewarm").start(() -> {
            try {
                String ip = NativeBridge.dnsResolve(hostname);
                if (ip != null) {
                    RustMC.LOGGER.debug("[Rust-MC] DNS pre-warmed {} → {}", hostname, ip);
                }
            } catch (Exception ignored) {
                // DNS prewarm is best-effort
            }
        });
    }
}