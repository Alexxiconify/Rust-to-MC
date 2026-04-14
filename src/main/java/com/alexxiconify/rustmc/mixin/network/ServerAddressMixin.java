package com.alexxiconify.rustmc.mixin.network;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.util.DnsCacheUtil;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Intercepts address parsing to prewarm Rust DNS cache for server ping/connect paths.
@Mixin(ServerAddress.class)
public class ServerAddressMixin {
    private ServerAddressMixin() {}

    @Inject(method = "parse", at = @At("HEAD"), require = 0)
    private static void onParse(String address, CallbackInfoReturnable<ServerAddress> cir) {
        if (!DnsCacheUtil.isDnsCacheEnabled()) return;
        // Pre-warm DNS in background — the actual connection will benefit from the warmed system DNS cache or our Rust-side cache
        String hostname = DnsCacheUtil.extractResolvableHostname(address);
        // Only resolve hostnames, not raw IPs
        if (!hostname.isEmpty()) {
            NativeBridge.dnsResolve(hostname);
        }
    }
}