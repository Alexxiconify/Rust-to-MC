package com.alexxiconify.rustmc.mixin.screen;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.DnsCacheUtil;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;
//
 //  When the multiplayer server list screen opens, batch-resolve all server hostnames
 //  in parallel via Rust's rayon-backed DNS resolver.  Pre-warms the DNS cache so
 //  individual server pings don't block on DNS lookups.
@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {
    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void batchPreResolveDnsOnInit(CallbackInfo ci) {
        triggerBatchResolve();
    }
    @Unique
    private static void triggerBatchResolve() {
        if (!DnsCacheUtil.isDnsCacheEnabled()) return;
        try {
            Thread.ofPlatform().name("rustmc-dns-batch-prewarm").daemon(true).start(MultiplayerScreenMixin::resolveAllServers);
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Failed to start DNS pre-resolve thread: {}", e.getMessage());
        }
    }
    @Unique
    private static void resolveAllServers() {
        try {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc == null) return;
            net.minecraft.client.option.ServerList serverList = new net.minecraft.client.option.ServerList(mc);
            serverList.loadFile();
            List<String> hostnames = collectHostnames(serverList);
            if (hostnames.isEmpty()) return;
            long start = System.nanoTime();
            String[] results = NativeBridge.dnsBatchResolve(hostnames.toArray(String[]::new));
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            int resolved = 0;
            for (String r : results) {
                if (r != null && !r.isEmpty()) resolved++;
            }
            RustMC.LOGGER.info("[Rust-MC] Batch DNS pre-resolved {}/{} servers in {}ms",
                    resolved, hostnames.size(), elapsed);
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] DNS batch pre-resolve failed: {}", e.getMessage());
        }
    }
    @Unique
    private static List<String> collectHostnames(net.minecraft.client.option.ServerList serverList) {
        List<String> hostnames = new ArrayList<>();
        for (int i = 0; i < serverList.size(); i++) {
            net.minecraft.client.network.ServerInfo info = serverList.get(i);
            if (info == null || info.address == null || info.address.isEmpty()) continue;
            String hostname = DnsCacheUtil.extractResolvableHostname(info.address);
            if (!hostname.isEmpty()) {
                hostnames.add(hostname);
            }
        }
        return hostnames;
    }
}