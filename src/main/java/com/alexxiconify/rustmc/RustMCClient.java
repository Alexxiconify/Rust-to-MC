package com.alexxiconify.rustmc;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
public class RustMCClient implements ClientModInitializer {
    private KeyBinding toggleDiagnosticMode;
    private KeyBinding toggleSparkline;
    private static final net.minecraft.client.option.KeyBinding.Category RUST_CATEGORY = 
        net.minecraft.client.option.KeyBinding.Category.create(net.minecraft.util.Identifier.of("rust-mc", "keybinds"));

    @Override
    public void onInitializeClient() {
        registerKeybinds();
        registerConnectionHooks();
        ClientTickEvents.END_CLIENT_TICK.register(this::handleKeybinds);
    }

    private static void registerConnectionHooks() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> NativeBridge.persistDnsCache("join"));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> NativeBridge.persistDnsCache("disconnect"));
    }
    private void registerKeybinds() {
        toggleDiagnosticMode = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_diagnostic_mode",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, RUST_CATEGORY));
        toggleSparkline = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustmc.toggle_sparkline",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, RUST_CATEGORY));
    }
    private void handleKeybinds(MinecraftClient client) {
        RustMC.Config cfg = RustMC.CONFIG;
        boolean changed = false;
        while (toggleDiagnosticMode.wasPressed()) {
            RustMC.Config.DiagnosticMode next = cycleMode(cfg.getDiagnosticMode());
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

    private RustMC.Config.DiagnosticMode cycleMode(RustMC.Config.DiagnosticMode current) {
        RustMC.Config.DiagnosticMode[] values = RustMC.Config.DiagnosticMode.values();
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






