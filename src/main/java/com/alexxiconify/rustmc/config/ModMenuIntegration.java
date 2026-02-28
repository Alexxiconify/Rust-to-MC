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

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModMenuIntegration implements ModMenuApi {

    private static final String YES = "§aYES";
    private static final String NO = "§7NO";

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            RustMCConfig cfg = RustMC.CONFIG;
            return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Rust to MC Config"))
                .category(buildStatusCategory())
                .category(buildMathCategory(cfg))
                .category(buildFeaturesCategory(cfg))
                .category(buildBridgesCategory(cfg))
                .category(buildDevCategory(cfg))
                .save(RustMC::saveConfig)
                .build()
                .generateScreen(parent);
        };
    }

    private ConfigCategory buildStatusCategory() {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Status"))
            .tooltip(Text.literal("Runtime status of the Rust native library."))
            .option(Option.<Boolean>createBuilder()
                .name(Text.literal("Native Core"))
                .description(OptionDescription.of(Text.literal("""
                    Shows whether the Rust native library loaded correctly.
                    If FAILED, all optimizations fall back to vanilla Java.""")))
                .binding(true, NativeBridge::isReady, val -> {})
                .controller(opt -> BooleanControllerBuilder.create(opt)
                    .formatValue(val -> Text.literal(NativeBridge.isReady() ? "§aREADY" : "§cFAILED")))
                .build())
            .option(buildDetectOption("Sodium Detected", () -> ModBridge.SODIUM))
            .option(buildDetectOption("Iris Detected", () -> ModBridge.IRIS))
            .option(buildDetectOption("Lithium Detected", () -> ModBridge.LITHIUM))
            .option(buildDetectOption("C2ME Detected", () -> ModBridge.C2ME))
            .build();
    }

    private Option<Boolean> buildDetectOption(String name, Supplier<Boolean> isDetected) {
        return Option.<Boolean>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal("Whether " + name.replace(" Detected", "") + " is installed.")))
            .binding(true, isDetected, val -> {})
            .controller(opt -> BooleanControllerBuilder.create(opt)
                .formatValue(val -> Text.literal(Boolean.TRUE.equals(isDetected.get()) ? YES : NO)))
            .build();
    }

    private ConfigCategory buildMathCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Math Optimizations"))
            .tooltip(Text.literal("Toggle individual Rust-backed math replacements."))
            .option(buildBooleanOption("Fast Sine", 
                "Replaces MathHelper.sin() with a blazing fast Lithium-style lookup table.",
                cfg::isUseNativeSine, v -> cfg.setUseNativeSine(v != null && v)))
            .option(buildBooleanOption("Fast Cosine", 
                "Replaces MathHelper.cos() with a blazing fast Lithium-style lookup table.",
                cfg::isUseNativeCos, v -> cfg.setUseNativeCos(v != null && v)))
            .option(buildBooleanOption("Native Sqrt", 
                "Replaces MathHelper.sqrt() with Fast Math replacement.",
                cfg::isUseNativeSqrt, v -> cfg.setUseNativeSqrt(v != null && v)))
            .option(buildBooleanOption("Fast Inv-Sqrt", 
                "Replaces MathHelper.fastInvSqrt() with Quake III fast inverse sqrt.",
                cfg::isUseNativeInvSqrt, v -> cfg.setUseNativeInvSqrt(v != null && v)))
            .option(buildBooleanOption("Native Tan", 
                "Replaces MathHelper.tan() with Fast Math replacement.",
                cfg::isUseNativeTan, v -> cfg.setUseNativeTan(v != null && v)))
            .option(buildBooleanOption("Native Atan2", 
                "Replaces MathHelper.atan2() with Fast Math replacement.",
                cfg::isUseNativeAtan2, v -> cfg.setUseNativeAtan2(v != null && v)))
            .option(buildBooleanOption("Native Floor", 
                "Replaces MathHelper.floor() with Fast Math replacement.",
                cfg::isUseNativeFloor, v -> cfg.setUseNativeFloor(v != null && v)))
            .option(buildBooleanOption("Native Noise (World Gen)", 
                """
                Replaces SimplexNoiseSampler with a Rust Simplex implementation seeded by the world seed.
                Automatically disabled when C2ME Bridge is ON and C2ME is installed.""",
                cfg::isUseNativeNoise, v -> cfg.setUseNativeNoise(v != null && v)))
            .build();
    }

    private ConfigCategory buildFeaturesCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Native Features"))
            .tooltip(Text.literal("Toggle individual Rust-backed feature hooks."))
            .option(buildBooleanOption("Native Compression", 
                """
                Replaces Minecraft packet Zlib compression with a Rust zlib-ng encoder.
                Reduces CPU overhead on busy servers.""",
                cfg::isUseNativeCompression, v -> cfg.setUseNativeCompression(v != null && v)))
            .option(buildBooleanOption("Native Lighting (Experimental)", 
                """
                Hooks into the lighting engine for Rust-parallel updates.
                Automatically disabled when Sodium, Starlight, C2ME, or Iris Bridge is ON and installed.""",
                cfg::isUseNativeLighting, v -> cfg.setUseNativeLighting(v != null && v)))
            .option(buildBooleanOption("Native Pathfinding (Experimental)", 
                """
                Uses a Rust A* to pre-compute mob path distances.
                Cancels vanilla only when the mob is already at its target (result = 0).
                Automatically disabled when Lithium Bridge is ON and Lithium is installed.""",
                cfg::isUseNativePathfinding, v -> cfg.setUseNativePathfinding(v != null && v)))
            .option(buildBooleanOption("Native Commands (Experimental)", 
                """
                Passes server commands to Rust before Brigadier processes them.
                Currently a no-op (Rust returns -1 so vanilla always runs).
                Leave OFF unless testing custom command interception.""",
                cfg::isUseNativeCommands, v -> cfg.setUseNativeCommands(v != null && v)))
            .build();
    }

    private ConfigCategory buildBridgesCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Mod Bridges"))
            .tooltip(Text.literal("Control which mods are allowed to 'own' a subsystem.\nWhen a bridge is ON and that mod is installed, Rust-MC steps aside."))
            .option(buildBooleanOption("Sodium Bridge", 
                "When ON and Sodium is installed, Rust-MC defers rendering-related math to Sodium.",
                cfg::isBridgeSodium, v -> cfg.setBridgeSodium(v != null && v)))
            .option(buildBooleanOption("Starlight Bridge", 
                "When ON and Starlight is installed, disables the native lighting hook so Starlight owns lighting.",
                cfg::isBridgeStarlight, v -> cfg.setBridgeStarlight(v != null && v)))
            .option(buildBooleanOption("C2ME Bridge", 
                "When ON and C2ME is installed, disables native math, noise, and lighting hooks so C2ME owns them.",
                cfg::isBridgeC2ME, v -> cfg.setBridgeC2ME(v != null && v)))
            .option(buildBooleanOption("Iris Bridge", 
                "When ON and Iris is installed, disables the native lighting hook so Iris can own the light pipeline.",
                cfg::isBridgeIris, v -> cfg.setBridgeIris(v != null && v)))
            .option(buildBooleanOption("Lithium Bridge", 
                "When ON and Lithium is installed, disables the native pathfinding hook so Lithium owns it.",
                cfg::isBridgeLithium, v -> cfg.setBridgeLithium(v != null && v)))
            .build();
    }

    private ConfigCategory buildDevCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Developer"))
            .option(buildBooleanOption("Silence Startup Logs", 
                """
                Filters repetitive INFO-level startup spam from other mods
                ("Loading mod…", mixin redirect notices, etc.).
                WARN and ERROR messages are never suppressed.""",
                cfg::isSilenceLogs, v -> cfg.setSilenceLogs(v != null && v)))
            .build();
    }

    private Option<Boolean> buildBooleanOption(String name, String desc, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal(desc)))
            .binding(true, getter, setter)
            .controller(BooleanControllerBuilder::create)
            .build();
    }
}