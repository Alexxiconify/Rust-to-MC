package com.alexxiconify.rustmc.config;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            RustMCConfig cfg = RustMC.CONFIG;

            return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Rust to MC Config"))

                // ── Status ────────────────────────────────────────────────
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Status"))
                    .tooltip(Text.literal("Runtime status of the Rust native library."))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Core"))
                        .description(OptionDescription.of(Text.literal(
                            "Shows whether the Rust native library loaded correctly.\n" +
                            "If FAILED, all optimizations fall back to vanilla Java.")))
                        .binding(true, NativeBridge::isReady, val -> {})
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                            .formatValue(val -> Text.literal(NativeBridge.isReady() ? "§aREADY" : "§cFAILED")))
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Sodium Detected"))
                        .description(OptionDescription.of(Text.literal("Whether Sodium is installed.")))
                        .binding(true, () -> ModBridge.SODIUM, val -> {})
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                            .formatValue(val -> Text.literal(ModBridge.SODIUM ? "§aYES" : "§7NO")))
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Iris Detected"))
                        .description(OptionDescription.of(Text.literal("Whether Iris is installed.")))
                        .binding(true, () -> ModBridge.IRIS, val -> {})
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                            .formatValue(val -> Text.literal(ModBridge.IRIS ? "§aYES" : "§7NO")))
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Lithium Detected"))
                        .description(OptionDescription.of(Text.literal("Whether Lithium is installed.")))
                        .binding(true, () -> ModBridge.LITHIUM, val -> {})
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                            .formatValue(val -> Text.literal(ModBridge.LITHIUM ? "§aYES" : "§7NO")))
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("C2ME Detected"))
                        .description(OptionDescription.of(Text.literal("Whether C2ME is installed.")))
                        .binding(true, () -> ModBridge.C2ME, val -> {})
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                            .formatValue(val -> Text.literal(ModBridge.C2ME ? "§aYES" : "§7NO")))
                        .build())
                    .build())

                // ── Math Optimizations ────────────────────────────────────
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Math Optimizations"))
                    .tooltip(Text.literal("Toggle individual Rust-backed math replacements."))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Sine"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces MathHelper.sin() with a fast Rust Bhaskara-I approximation (~0.001 max error).")))
                        .binding(true, cfg::isUseNativeSine, v -> cfg.setUseNativeSine(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Cosine"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces MathHelper.cos() with a fast Rust approximation via sin(x + π/2).")))
                        .binding(true, cfg::isUseNativeCos, v -> cfg.setUseNativeCos(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Sqrt"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces MathHelper.sqrt() with native hardware sqrt via Rust.")))
                        .binding(true, cfg::isUseNativeSqrt, v -> cfg.setUseNativeSqrt(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Inv-Sqrt"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces MathHelper.fastInvSqrt() with the Quake III fast inverse sqrt (two NR iterations).")))
                        .binding(true, cfg::isUseNativeInvSqrt, v -> cfg.setUseNativeInvSqrt(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Noise (World Gen)"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces SimplexNoiseSampler with a Rust Simplex implementation seeded by the world seed.\n" +
                            "Automatically disabled when C2ME Bridge is ON and C2ME is installed.")))
                        .binding(true, cfg::isUseNativeNoise, v -> cfg.setUseNativeNoise(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .build())

                // ── Native Features ───────────────────────────────────────
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Native Features"))
                    .tooltip(Text.literal("Toggle individual Rust-backed feature hooks."))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Compression"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces Minecraft packet Zlib compression with a Rust zlib-ng encoder.\n" +
                            "Reduces CPU overhead on busy servers.")))
                        .binding(true, cfg::isUseNativeCompression, v -> cfg.setUseNativeCompression(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Lighting (Experimental)"))
                        .description(OptionDescription.of(Text.literal(
                            "Hooks into the lighting engine for Rust-parallel updates.\n" +
                            "Automatically disabled when Sodium Bridge, Starlight Bridge, C2ME Bridge, or Iris Bridge is ON " +
                            "and the respective mod is installed.")))
                        .binding(true, cfg::isUseNativeLighting, v -> cfg.setUseNativeLighting(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Pathfinding (Experimental)"))
                        .description(OptionDescription.of(Text.literal(
                            "Uses a Rust A* to pre-compute mob path distances.\n" +
                            "Cancels vanilla only when the mob is already at its target (result = 0).\n" +
                            "Automatically disabled when Lithium Bridge is ON and Lithium is installed.")))
                        .binding(true, cfg::isUseNativePathfinding, v -> cfg.setUseNativePathfinding(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Commands (Experimental)"))
                        .description(OptionDescription.of(Text.literal(
                            "Passes server commands to Rust before Brigadier processes them.\n" +
                            "Currently a no-op (Rust returns -1 so vanilla always runs).\n" +
                            "Leave OFF unless testing custom command interception.")))
                        .binding(false, cfg::isUseNativeCommands, v -> cfg.setUseNativeCommands(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .build())

                // ── Mod Bridges ───────────────────────────────────────────
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Mod Bridges"))
                    .tooltip(Text.literal(
                        "Control which mods are allowed to 'own' a subsystem.\n" +
                        "When a bridge is ON and that mod is installed, Rust-MC steps aside."))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Sodium Bridge"))
                        .description(OptionDescription.of(Text.literal(
                            "When ON and Sodium is installed, Rust-MC defers rendering-related math to Sodium.")))
                        .binding(true, cfg::isBridgeSodium, v -> cfg.setBridgeSodium(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Starlight Bridge"))
                        .description(OptionDescription.of(Text.literal(
                            "When ON and Starlight is installed, disables the native lighting hook so Starlight owns lighting.")))
                        .binding(true, cfg::isBridgeStarlight, v -> cfg.setBridgeStarlight(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("C2ME Bridge"))
                        .description(OptionDescription.of(Text.literal(
                            "When ON and C2ME is installed, disables native math, noise, and lighting hooks so C2ME owns them.")))
                        .binding(true, cfg::isBridgeC2ME, v -> cfg.setBridgeC2ME(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Iris Bridge"))
                        .description(OptionDescription.of(Text.literal(
                            "When ON and Iris is installed, disables the native lighting hook so Iris can own the light pipeline.")))
                        .binding(true, cfg::isBridgeIris, v -> cfg.setBridgeIris(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Lithium Bridge"))
                        .description(OptionDescription.of(Text.literal(
                            "When ON and Lithium is installed, disables the native pathfinding hook so Lithium owns it.")))
                        .binding(true, cfg::isBridgeLithium, v -> cfg.setBridgeLithium(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .build())

                // ── Developer ─────────────────────────────────────────────
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Developer"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Silence Startup Logs"))
                        .description(OptionDescription.of(Text.literal(
                            "Filters repetitive INFO-level startup spam from other mods\n" +
                            "(\"Loading mod…\", mixin redirect notices, etc.).\n" +
                            "WARN and ERROR messages are never suppressed.")))
                        .binding(true, cfg::isSilenceLogs, v -> cfg.setSilenceLogs(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .build())

                .save(RustMC::saveConfig)
                .build()
                .generateScreen(parent);
        };
    }
}