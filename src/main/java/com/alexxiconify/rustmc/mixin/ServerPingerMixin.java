package com.alexxiconify.rustmc.mixin;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.util.DnsCacheUtil;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.NetworkingBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
 //  Optimizes server list ping times by pre-resolving DNS via the Rust cached resolver.
 //  Each server gets its own platform daemon thread for maximum parallelism — all DNS lookups
 //  happen concurrently instead of sequentially.
@Mixin(MultiplayerServerListPinger.class)
public class ServerPingerMixin {
    @Inject(method = "add", at = @At("HEAD"), require = 0)
    private void prewarmDns( ServerInfo entry , Runnable saver , Runnable pingCallback , NetworkingBackend backend , CallbackInfo ci ) {
        if (!DnsCacheUtil.isDnsCacheEnabled()) return;
        if (entry == null || entry.address == null || entry.address.isEmpty()) return;
        String hostname = DnsCacheUtil.extractResolvableHostname(entry.address);
        if (hostname.isEmpty()) return;
        // Fire-and-forget DNS pre-warm on a platform daemon thread — each server resolves in parallel
        Thread.ofPlatform().daemon(true).name("rustmc-dns-" + hostname).start(() -> {
            try {
                NativeBridge.dnsResolve(hostname);
            } catch (Exception ignored) {
                // DNS prewarm is best-effort
            }
        });
    }
}