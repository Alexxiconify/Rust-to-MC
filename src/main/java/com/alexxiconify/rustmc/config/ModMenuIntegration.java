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
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModMenuIntegration implements ModMenuApi {

    private static final String YES = "§aYES";
    private static final String NO  = "§7NO";

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
                .category(buildLoadingScreenCategory(cfg))
                .category(buildElbCategory())
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
                .description(OptionDescription.of(Text.literal(
                    "Shows whether the Rust native library loaded correctly.\n" +
                    "If FAILED, all optimizations fall back to vanilla Java.")))
                .binding(true, NativeBridge::isReady, val -> {})
                .controller(opt -> BooleanControllerBuilder.create(opt)
                    .formatValue(val -> Text.literal(NativeBridge.isReady() ? "§aREADY" : "§cFAILED")))
                .build())
            .option(buildDetectOption("Sodium Detected",   () -> ModBridge.SODIUM))
            .option(buildDetectOption("Iris Detected",     () -> ModBridge.IRIS))
            .option(buildDetectOption("Lithium Detected",  () -> ModBridge.LITHIUM))
            .option(buildDetectOption("C2ME Detected",     () -> ModBridge.C2ME))
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
                "Replaces MathHelper.sin() with a LUT (lookup table).",
                cfg::isUseNativeSine, v -> cfg.setUseNativeSine(v != null && v)))
            .option(buildBooleanOption("Fast Cosine",
                "Replaces MathHelper.cos() with a LUT.",
                cfg::isUseNativeCos, v -> cfg.setUseNativeCos(v != null && v)))
            .option(buildBooleanOption("Native Sqrt",
                "Replaces MathHelper.sqrt() cast path.",
                cfg::isUseNativeSqrt, v -> cfg.setUseNativeSqrt(v != null && v)))
            .option(buildBooleanOption("Fast Inv-Sqrt",
                "Replaces MathHelper.fastInvSqrt() with Quake III algorithm.",
                cfg::isUseNativeInvSqrt, v -> cfg.setUseNativeInvSqrt(v != null && v)))
            .option(buildBooleanOption("Native Tan",
                "Replaces MathHelper.tan().", cfg::isUseNativeTan, v -> cfg.setUseNativeTan(v != null && v)))
            .option(buildBooleanOption("Native Atan2",
                "Replaces MathHelper.atan2().", cfg::isUseNativeAtan2, v -> cfg.setUseNativeAtan2(v != null && v)))
            .option(buildBooleanOption("Native Floor",
                "Replaces MathHelper.floor() with bitwise cast.", cfg::isUseNativeFloor, v -> cfg.setUseNativeFloor(v != null && v)))
            .option(buildBooleanOption("Native Noise (World Gen)",
                "Replaces SimplexNoiseSampler with Rust Simplex, seeded by world seed.\n" +
                "Disabled automatically when C2ME Bridge is ON.",
                cfg::isUseNativeNoise, v -> cfg.setUseNativeNoise(v != null && v)))
            .build();
    }

    private ConfigCategory buildFeaturesCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Native Features"))
            .tooltip(Text.literal("Toggle individual Rust-backed feature hooks."))
            .option(buildBooleanOption("Native Compression",
                "Replaces packet Zlib compression with Rust zlib-ng encoder.",
                cfg::isUseNativeCompression, v -> cfg.setUseNativeCompression(v != null && v)))
            .option(buildBooleanOption("Native Lighting (Experimental)",
                "Hooks lighting engine for Rust-parallel updates.\n" +
                "Disabled when Sodium/Starlight/C2ME/Iris Bridge is ON.",
                cfg::isUseNativeLighting, v -> cfg.setUseNativeLighting(v != null && v)))
            .option(buildBooleanOption("Native Pathfinding (Experimental)",
                "Rust A* pre-computes mob path distances; cancels vanilla only when mob is at target.\n" +
                "Disabled when Lithium Bridge is ON.",
                cfg::isUseNativePathfinding, v -> cfg.setUseNativePathfinding(v != null && v)))
            .option(buildBooleanOption("Native Culling (Fixes Dripstone)",
                "Prevents aggressive face culling on 3D Dripstone (VanillaTweaks).",
                cfg::isUseNativeCulling, v -> cfg.setUseNativeCulling(v != null && v)))
            .option(buildBooleanOption("Native Commands (Experimental)",
                "Passes server commands to Rust before Brigadier. Currently a no-op — leave OFF.",
                cfg::isUseNativeCommands, v -> cfg.setUseNativeCommands(v != null && v)))
            .option(buildBooleanOption("Limit Xaero Minimap",
                "Limits Xaero's Minimap to ~30 FPS update rate to save CPU/GPU.",
                cfg::isLimitXaeroMinimap, v -> cfg.setLimitXaeroMinimap(v != null && v)))
            .build();
    }

    private ConfigCategory buildBridgesCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Mod Bridges"))
            .tooltip(Text.literal("Control which mods are allowed to 'own' a subsystem.\nWhen ON + mod installed, Rust-MC steps aside."))
            .option(buildBooleanOption("Sodium Bridge",
                "Defer rendering-related math to Sodium when present.",
                cfg::isBridgeSodium, v -> cfg.setBridgeSodium(v != null && v)))
            .option(buildBooleanOption("Starlight Bridge",
                "Disable native lighting when Starlight is installed.",
                cfg::isBridgeStarlight, v -> cfg.setBridgeStarlight(v != null && v)))
            .option(buildBooleanOption("C2ME Bridge",
                "Disable native math/noise/lighting hooks when C2ME is installed.",
                cfg::isBridgeC2ME, v -> cfg.setBridgeC2ME(v != null && v)))
            .option(buildBooleanOption("Iris Bridge",
                "Disable native lighting hook when Iris is installed.",
                cfg::isBridgeIris, v -> cfg.setBridgeIris(v != null && v)))
            .option(buildBooleanOption("Lithium Bridge",
                "Disable native pathfinding when Lithium is installed.",
                cfg::isBridgeLithium, v -> cfg.setBridgeLithium(v != null && v)))
            .option(buildBooleanOption("Disable DH Chunk Fade",
                "Disables Distant Horizons LOD fade for a sharper look.",
                cfg::isDisableDhFade, v -> cfg.setDisableDhFade(v != null && v)))
            .build();
    }

    /** Loading screen overlay color configuration. */
    private ConfigCategory buildLoadingScreenCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Loading Screen Colors"))
            .tooltip(Text.literal("Customize the in-game loading overlay colors."))
            .option(buildBooleanOption("Enable Fast Loading Screen",
                "Enables the Rust-MC loading overlay (RAM bar, mod count, dark background).",
                cfg::isUseFastLoadingScreen, v -> cfg.setUseFastLoadingScreen(v != null && v)))
            .option(buildColorOption("Bar Background",
                "Dark track color behind the RAM bar.",
                () -> new Color(cfg.getLoadingBarBgColor(), true),
                c -> cfg.setLoadingBarBgColor(c.getRGB())))
            .option(buildColorOption("Bar Low (< 60%)",
                "Color when RAM usage is below 60%.",
                () -> new Color(cfg.getLoadingBarLowColor(), true),
                c -> cfg.setLoadingBarLowColor(c.getRGB())))
            .option(buildColorOption("Bar Mid (60–80%)",
                "Color when RAM usage is 60–80%.",
                () -> new Color(cfg.getLoadingBarMidColor(), true),
                c -> cfg.setLoadingBarMidColor(c.getRGB())))
            .option(buildColorOption("Bar High (> 80%)",
                "Color when RAM usage exceeds 80%.",
                () -> new Color(cfg.getLoadingBarHighColor(), true),
                c -> cfg.setLoadingBarHighColor(c.getRGB())))
            .option(buildColorOption("RAM Label Text",
                "Color of the RAM MB / MB text.",
                () -> new Color(cfg.getLoadingBarTextColor(), true),
                c -> cfg.setLoadingBarTextColor(c.getRGB())))
            .option(buildColorOption("Mod Count Text",
                "Color of the 'Rust-MC • N mods' label.",
                () -> new Color(cfg.getLoadingBarSubtextColor(), true),
                c -> cfg.setLoadingBarSubtextColor(c.getRGB())))
            .build();
    }

    private ConfigCategory buildElbCategory() {
        com.iafenvoy.elb.config.ElbConfig elb = com.iafenvoy.elb.config.ElbConfig.getInstance();
        return ConfigCategory.createBuilder()
            .name(Text.literal("Early Loading Bar"))
            .tooltip(Text.literal("Customize the pre-launch Swing window appearance."))
            .option(Option.<String>createBuilder()
                .name(Text.literal("Window Title"))
                .description(OptionDescription.of(Text.literal("Title text. Use %version% for MC version.")))
                .binding("Early Loading Bar %version%", elb::getBarTitle, elb::setBarTitle)
                .controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create)
                .build())
            .option(Option.<String>createBuilder()
                .name(Text.literal("Loading Message"))
                .description(OptionDescription.of(Text.literal("Text shown below the progress bars.")))
                .binding("Loading Minecraft %version%...", elb::getBarMessage, elb::setBarMessage)
                .controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create)
                .build())
            .option(Option.<String>createBuilder()
                .name(Text.literal("Logo Path"))
                .description(OptionDescription.of(Text.literal("Absolute path to a .png / .jpg logo (optional).")))
                .binding("", elb::getLogoPath, elb::setLogoPath)
                .controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create)
                .build())
            .option(buildColorOptionSwing("Memory Bar Color",
                "Color of the RAM usage bar in the ELB window.",
                () -> parseSwingColor(elb.getMemoryBarColor(), java.awt.Color.RED),
                c -> elb.setMemoryBarColor(String.valueOf(c.getRGB()))))
            .option(buildColorOptionSwing("Mod Bar Color",
                "Color of the mod loading progress bar in the ELB window.",
                () -> parseSwingColor(elb.getMessageBarColor(), java.awt.Color.MAGENTA),
                c -> elb.setMessageBarColor(String.valueOf(c.getRGB()))))
            .build();
    }

    private ConfigCategory buildDevCategory(RustMCConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Text.literal("Developer"))
            .option(buildBooleanOption("Silence Startup Logs",
                "Filters repetitive INFO-level startup spam from other mods.\nWARN and ERROR messages are never suppressed.",
                cfg::isSilenceLogs, v -> cfg.setSilenceLogs(v != null && v)))
            .build();
    }

    // --- Helpers ---

    private Option<Boolean> buildBooleanOption(String name, String desc, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal(desc)))
            .binding(true, getter, setter)
            .controller(BooleanControllerBuilder::create)
            .build();
    }

    private Option<Color> buildColorOption(String name, String desc, Supplier<Color> getter, Consumer<Color> setter) {
        return Option.<Color>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal(desc)))
            .binding(getter.get(), getter, setter)
            .controller(ColorControllerBuilder::create)
            .build();
    }

    /** Same as buildColorOption but for Swing java.awt.Color (ELB colors). */
    private Option<Color> buildColorOptionSwing(String name, String desc, Supplier<Color> getter, Consumer<Color> setter) {
        return buildColorOption(name, desc, getter, setter);
    }

    private static java.awt.Color parseSwingColor(String val, java.awt.Color fallback) {
        try { return new java.awt.Color(Integer.parseInt(val)); } catch (Exception e) { return fallback; }
    }
}