package com.alexxiconify.rustmc;

import com.alexxiconify.rustmc.config.RustMCConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side initialiser for Rust-MC.
 * Registers keybinds for toggling overlays:
 *   F7 — Performance pie chart
 *   F8 — Frame-time sparkline graph
 *   F9 — RAM bar overlay
 */
public class RustMCClient implements ClientModInitializer {

    private KeyBinding togglePieChart;
    private KeyBinding toggleFrameGraph;
    private KeyBinding toggleRamBar;

    @Override
    public void onInitializeClient() {
        registerKeybinds();
        ClientTickEvents.END_CLIENT_TICK.register(client -> handleKeybinds());
    }

    private void registerKeybinds() {
        KeyBinding.Category rustCategory = KeyBinding.Category.create(
                Identifier.of(RustMC.MOD_ID, "keybinds"));

        togglePieChart = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_pie_chart",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, rustCategory));

        toggleFrameGraph = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_frame_graph",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, rustCategory));

        toggleRamBar = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_ram_bar",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, rustCategory));
    }

    private void handleKeybinds() {
        RustMCConfig cfg = RustMC.CONFIG;

        while (togglePieChart.wasPressed()) {
            cfg.setEnablePieChart(!cfg.isEnablePieChart());
            RustMC.LOGGER.info("[Rust-MC] Pie chart: {}", cfg.isEnablePieChart() ? "ON" : "OFF");
        }
        while (toggleFrameGraph.wasPressed()) {
            cfg.setEnableDebugHudGraph(!cfg.isEnableDebugHudGraph());
            RustMC.LOGGER.info("[Rust-MC] Frame graph: {}", cfg.isEnableDebugHudGraph() ? "ON" : "OFF");
        }
        while (toggleRamBar.wasPressed()) {
            cfg.setUseFastLoadingScreen(!cfg.isUseFastLoadingScreen());
            RustMC.LOGGER.info("[Rust-MC] RAM bar overlay: {}", cfg.isUseFastLoadingScreen() ? "ON" : "OFF");
        }
    }
}