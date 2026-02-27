package com.alexxiconify.rustmc.config;

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
            RustMCConfig config = RustMC.CONFIG;

            return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Rust to MC Config"))

                // ── Status ────────────────────────────────────────────────
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Status"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Core"))
                        .description(OptionDescription.of(Text.literal(
                            "Shows whether the Rust native library loaded correctly. " +
                            "If FAILED, all optimizations fall back to vanilla Java.")))
                        .binding(true, NativeBridge::isReady, val -> {})
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                            .formatValue(val -> Text.literal(NativeBridge.isReady() ? "§aREADY" : "§cFAILED")))
                        .build())
                    .build())

                // ── Math Optimizations ────────────────────────────────────
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Math Optimizations"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Sine"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces MathHelper.sin() with a fast Rust Taylor-series approximation.")))
                        .binding(true, config::isUseNativeSine, v -> config.setUseNativeSine(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Cosine"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces MathHelper.cos() with a fast Rust approximation via sin(x + π/2).")))
                        .binding(true, config::isUseNativeCos, v -> config.setUseNativeCos(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Sqrt"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces MathHelper.sqrt() with the native hardware sqrt via Rust.")))
                        .binding(true, config::isUseNativeSqrt, v -> config.setUseNativeSqrt(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native InvSqrt"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces MathHelper.fastInvSqrt() with the Quake III fast inverse square root.")))
                        .binding(true, config::isUseNativeInvSqrt, v -> config.setUseNativeInvSqrt(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Noise (World Gen)"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces SimplexNoiseSampler with a Rust-side SIMD-accelerated implementation. " +
                            "Speeds up chunk generation. Disabled automatically when C2ME is present.")))
                        .binding(true, config::isUseNativeNoise, v -> config.setUseNativeNoise(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .build())

                // ── Native Features ───────────────────────────────────────
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Native Features"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Compression"))
                        .description(OptionDescription.of(Text.literal(
                            "Replaces Minecraft's packet Zlib compression with a Rust-based encoder. " +
                            "Reduces CPU overhead on high-population servers.")))
                        .binding(true, config::isUseNativeCompression, v -> config.setUseNativeCompression(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Lighting (Experimental)"))
                        .description(OptionDescription.of(Text.literal(
                            "Hooks into the lighting engine to offload updates to Rust. " +
                            "Currently a stub – real data serialisation is in progress. " +
                            "Disabled automatically when Sodium, Starlight or C2ME are present.")))
                        .binding(true, config::isUseNativeLighting, v -> config.setUseNativeLighting(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Pathfinding (Experimental)"))
                        .description(OptionDescription.of(Text.literal(
                            "Uses a Rust A* implementation to assist mob pathfinding. " +
                            "Currently cancels vanilla only when a mob is already at its target. " +
                            "Full path construction is in progress. Disabled when Lithium is present.")))
                        .binding(true, config::isUseNativePathfinding, v -> config.setUseNativePathfinding(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Native Commands (Experimental)"))
                        .description(OptionDescription.of(Text.literal(
                            "Passes server commands to Rust before Brigadier processes them. " +
                            "Currently a no-op (Rust returns -1 for all commands, so vanilla always runs). " +
                            "Enable only for testing – OFF by default.")))
                        .binding(false, config::isUseNativeCommands, v -> config.setUseNativeCommands(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .build())

                // ── Mod Bridges ───────────────────────────────────────────
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Mod Bridges"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Sodium Bridge"))
                        .description(OptionDescription.of(Text.literal(
                            "When enabled, Rust-MC defers rendering-related math to Sodium where possible.")))
                        .binding(true, config::isBridgeSodium, v -> config.setBridgeSodium(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Starlight Bridge"))
                        .description(OptionDescription.of(Text.literal(
                            "When enabled, disables the native lighting hook so Starlight can own lighting.")))
                        .binding(true, config::isBridgeStarlight, v -> config.setBridgeStarlight(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("C2ME Bridge"))
                        .description(OptionDescription.of(Text.literal(
                            "When enabled, disables native math and noise hooks so C2ME can own them.")))
                        .binding(true, config::isBridgeC2ME, v -> config.setBridgeC2ME(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .build())

                // ── Developer ─────────────────────────────────────────────
                .category(ConfigCategory.createBuilder()
                    .name(Text.literal("Developer"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Silence Startup Logs"))
                        .description(OptionDescription.of(Text.literal(
                            "Filters out repetitive INFO-level startup spam from other mods " +
                            "(\"Loading mod...\", mixin redirect notices, etc.).")))
                        .binding(true, config::isSilenceLogs, v -> config.setSilenceLogs(v != null && v))
                        .controller(BooleanControllerBuilder::create)
                        .build())
                    .build())

                .save(RustMC::saveConfig)
                .build()
                .generateScreen(parent);
        };
    }
}