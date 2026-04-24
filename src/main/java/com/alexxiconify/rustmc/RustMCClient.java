package com.alexxiconify.rustmc;
import com.alexxiconify.rustmc.config.RustMCConfig;
import com.alexxiconify.rustmc.util.DnsCacheUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
// Client-side initializer for Rust-MC. Registers keybinds for toggling overlays: F6 — Metrics HUD.  F8 — Frame-time sparkline graph
public class RustMCClient implements ClientModInitializer {
    private KeyBinding toggleNativeMetrics;
    private KeyBinding toggleFrameGraph;
    @Override
    public void onInitializeClient() {
        registerKeybinds();
        registerConnectionHooks();
        ClientTickEvents.END_CLIENT_TICK.register(this::handleKeybinds);
    }

    private static void registerConnectionHooks() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> DnsCacheUtil.persistDnsCache("join"));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DnsCacheUtil.persistDnsCache("disconnect"));
    }
    private void registerKeybinds() {
        // Use a stable alphanumeric namespace for category translation compatibility with Controlling.
        KeyBinding.Category rustCategory = KeyBinding.Category.create(Identifier.of("rustmc", "keybinds"));
        toggleNativeMetrics = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_ram_bar",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, rustCategory));
        toggleFrameGraph = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_frame_graph",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, rustCategory));
    }
    private void handleKeybinds(MinecraftClient client) {
        RustMCConfig cfg = RustMC.CONFIG;
        boolean changed = false;
        while (toggleNativeMetrics.wasPressed()) {
            boolean enabled = !cfg.isEnableNativeMetricsHud();
            cfg.setNativeStatsEnabled(enabled);
            String state = enabled ? "ON" : "OFF";
            RustMC.LOGGER.info("[Rust-MC] Native stats overlays: {}", state);
            showActionBar(client, "Rust-MC Native Stats: " + state);
            changed = true;
        }
        while (toggleFrameGraph.wasPressed()) {
            cfg.setDebugHudGraphEnabled(!cfg.isDebugHudGraphEnabled());
            String state = cfg.isDebugHudGraphEnabled() ? "ON" : "OFF";
            RustMC.LOGGER.info("[Rust-MC] Frame graph: {}", state);
            showActionBar(client, "Rust-MC Frame Graph: " + state);
            changed = true;
        }
        // Persist to disk so the toggle survives restarts
        if (changed) {
            RustMC.saveConfig();
        }
    }


    private static void showActionBar(MinecraftClient client, String message) {
        if (client == null || client.player == null) {
            return;
        }
        client.player.sendMessage(Text.literal(message), true);
    }
}