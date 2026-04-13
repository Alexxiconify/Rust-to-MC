package com.alexxiconify.rustmc;
import com.alexxiconify.rustmc.config.RustMCConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
//
 //  Client-side initializer for Rust-MC.
 //  Registers keybinds for toggling overlays:
 //    F6 — Native metrics HUD
 //    F7 — Timing info overlay
 //    F8 — Frame-time sparkline graph
 //    F9 — DH culling debug log
public class RustMCClient implements ClientModInitializer {
    private KeyBinding toggleNativeMetrics;
    private KeyBinding togglePieChart;
    private KeyBinding toggleFrameGraph;
    private KeyBinding toggleDhCullingDebugLog;
    @Override
    public void onInitializeClient() {
        registerKeybinds();
        ClientTickEvents.END_CLIENT_TICK.register(client -> handleKeybinds());
    }
    private void registerKeybinds() {
        // Use a stable alphanumeric namespace for category translation compatibility with Controlling.
        KeyBinding.Category rustCategory = KeyBinding.Category.create(Identifier.of("rustmc", "keybinds"));
        toggleNativeMetrics = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_ram_bar",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, rustCategory));
        togglePieChart = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_pie_chart",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, rustCategory));
        toggleFrameGraph = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_frame_graph",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, rustCategory));
        toggleDhCullingDebugLog = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_dh_culling_log",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, rustCategory));
    }
    private void handleKeybinds() {
        RustMCConfig cfg = RustMC.CONFIG;
        boolean changed = false;
        while (toggleNativeMetrics.wasPressed()) {
            cfg.setEnableNativeMetricsHud(!cfg.isEnableNativeMetricsHud());
            RustMC.LOGGER.info("[Rust-MC] Native metrics HUD: {}", cfg.isEnableNativeMetricsHud() ? "ON" : "OFF");
            changed = true;
        }
        while (togglePieChart.wasPressed()) {
            cfg.setEnablePieChart(!cfg.isEnablePieChart());
            RustMC.LOGGER.info("[Rust-MC] Timing info overlay: {}", cfg.isEnablePieChart() ? "ON" : "OFF");
            changed = true;
        }
        while (toggleFrameGraph.wasPressed()) {
            cfg.setEnableDebugHudGraph(!cfg.isEnableDebugHudGraph());
            RustMC.LOGGER.info("[Rust-MC] Frame graph: {}", cfg.isEnableDebugHudGraph() ? "ON" : "OFF");
            changed = true;
        }
        while (toggleDhCullingDebugLog.wasPressed()) {
            cfg.setEnableDhCullingDebugLog(!cfg.isEnableDhCullingDebugLog());
            RustMC.LOGGER.info("[Rust-MC] DH culling debug log: {}", cfg.isEnableDhCullingDebugLog() ? "ON" : "OFF");
            changed = true;
        }
        // Persist to disk so the toggle survives restarts
        if (changed) {
            RustMC.saveConfig();
        }
    }
}