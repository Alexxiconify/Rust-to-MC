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
    private KeyBinding toggleDiagnosticMode;
    private KeyBinding toggleSparkline;
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
        toggleDiagnosticMode = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_diagnostic_mode",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, rustCategory));
        toggleSparkline = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_sparkline",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, rustCategory));
    }
    private void handleKeybinds(MinecraftClient client) {
        RustMCConfig cfg = RustMC.CONFIG;
        boolean changed = false;
        while (toggleDiagnosticMode.wasPressed()) {
            RustMCConfig.DiagnosticMode next = cycleMode(cfg.getDiagnosticMode());
            cfg.setDiagnosticMode(next);
            RustMC.LOGGER.info("[Rust-MC] Diagnostic mode: {}", next);
            showActionBar(client, "Rust-MC Diagnostics: " + next.name());
            changed = true;
        }
        while (toggleSparkline.wasPressed()) {
            cfg.setEnableSparklineGraph(!cfg.isEnableSparklineGraph());
            String state = cfg.isEnableSparklineGraph() ? "ON" : "OFF";
            RustMC.LOGGER.info("[Rust-MC] Sparkline graph: {}", state);
            showActionBar(client, "Rust-MC Sparkline: " + state);
            changed = true;
        }
        // Persist to disk so the toggle survives restarts
        if (changed) {
            RustMC.saveConfig();
        }
    }

    private RustMCConfig.DiagnosticMode cycleMode(RustMCConfig.DiagnosticMode current) {
        RustMCConfig.DiagnosticMode[] values = RustMCConfig.DiagnosticMode.values();
        return values[(current.ordinal() + 1) % values.length];
    }


    private static void showActionBar(MinecraftClient client, String message) {
        if (client == null) {
            return;
        }
        var player = client.player;
        if (player != null) {
            player.sendMessage(Text.literal(message), true);
        }
    }
}