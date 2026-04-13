package com.alexxiconify.rustmc.mixin;
import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
 //  Intercepts Minecraft's connection address resolution to use the Rust DNS cache.
 //  This avoids blocking the network thread on slow DNS lookups when connecting
 //  to or pinging servers. The Rust side caches results for 5 minutes with
 //  parallel batch resolution support via rayon.
@Mixin(ServerAddress.class)
public class ServerAddressMixin {
    private ServerAddressMixin() {}
    //
     // Before Minecraft resolves a ServerAddress, try to use our cached DNS.
     // This hooks the static parse method which is called for every server connection.
    @Inject(method = "parse", at = @At("HEAD"), require = 0)
    private static void onParse(String address, CallbackInfoReturnable<ServerAddress> cir) {
        if (!NativeBridge.isReady() || address == null) return;
        // Pre-warm DNS in background — the actual connection will benefit
        // from the warmed system DNS cache or our Rust-side cache
        String hostname = address.contains(":") ? address.substring(0, address.lastIndexOf(':')) : address;
        // Only resolve hostnames, not raw IPs
        if (!hostname.isEmpty() && !Character.isDigit(hostname.charAt(0))) {
            NativeBridge.dnsResolve(hostname);
        }
    }
}