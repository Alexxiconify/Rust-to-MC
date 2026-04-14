package com.alexxiconify.rustmc;
import com.alexxiconify.rustmc.config.RustMCConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
//
 //  Client-side initializer for Rust-MC.
 //  Registers keybinds for toggling overlays:
 //    F6 — Native metrics HUD
 //    F8 — Frame-time sparkline graph
 //    F9 — DH culling debug log
public class RustMCClient implements ClientModInitializer {
    private KeyBinding toggleNativeMetrics;
    private KeyBinding toggleFrameGraph;
    private KeyBinding toggleDhCullingDebugLog;
    private KeyBinding cycleDhCullingSpaceMode;
    @Override
    public void onInitializeClient() {
        registerKeybinds();
        ClientTickEvents.END_CLIENT_TICK.register(this::handleKeybinds);
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
        toggleDhCullingDebugLog = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_dh_culling_log",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, rustCategory));
        cycleDhCullingSpaceMode = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.cycle_dh_culling_space",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F10, rustCategory));
    }
    private void handleKeybinds(MinecraftClient client) {
        RustMCConfig cfg = RustMC.CONFIG;
        boolean changed = false;
        while (toggleNativeMetrics.wasPressed()) {
            cfg.setEnableNativeMetricsHud(!cfg.isEnableNativeMetricsHud());
            String state = cfg.isEnableNativeMetricsHud() ? "ON" : "OFF";
            RustMC.LOGGER.info("[Rust-MC] Native metrics HUD: {}", state);
            showActionBar(client, "Rust-MC Native Metrics HUD: " + state);
            changed = true;
        }
        while (toggleFrameGraph.wasPressed()) {
            cfg.setEnableDebugHudGraph(!cfg.isEnableDebugHudGraph());
            String state = cfg.isEnableDebugHudGraph() ? "ON" : "OFF";
            RustMC.LOGGER.info("[Rust-MC] Frame graph: {}", state);
            showActionBar(client, "Rust-MC Frame Graph: " + state);
            changed = true;
        }
        while (toggleDhCullingDebugLog.wasPressed()) {
            cfg.setEnableDhCullingDebugLog(!cfg.isEnableDhCullingDebugLog());
            String state = cfg.isEnableDhCullingDebugLog() ? "ON" : "OFF";
            RustMC.LOGGER.info("[Rust-MC] DH culling debug log: {}", state);
            showActionBar(client, "Rust-MC DH Culling Log: " + state);
            changed = true;
        }
        while (cycleDhCullingSpaceMode.wasPressed()) {
            String nextMode = RustMCConfig.nextDhCullingSpaceMode(cfg.getDhCullingSpaceMode());
            cfg.setDhCullingSpaceMode(nextMode);
            String modeLabel = formatDhMode(nextMode);
            RustMC.LOGGER.info("[Rust-MC] DH culling space mode: {}", nextMode);
            showActionBar(client, "Rust-MC DH Culling Space: " + modeLabel);
            changed = true;
        }
        // Persist to disk so the toggle survives restarts
        if (changed) {
            RustMC.saveConfig();
        }
    }

    private static String formatDhMode(String mode) {
        return switch (RustMCConfig.normalizeDhCullingSpaceMode(mode)) {
            case RustMCConfig.DH_CULLING_SPACE_ABSOLUTE -> "Absolute";
            case RustMCConfig.DH_CULLING_SPACE_PLUS_CAMERA -> "+Camera";
            case RustMCConfig.DH_CULLING_SPACE_MINUS_CAMERA -> "-Camera";
            default -> "Auto";
        };
    }

    private static void showActionBar(MinecraftClient client, String message) {
        if (client == null || client.player == null) {
            return;
        }
        client.player.sendMessage(Text.literal(message), true);
    }
}