package com.alexxiconify.rustmc;
import com.alexxiconify.rustmc.util.BlameLog;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
public class MixinManager implements IMixinConfigPlugin {
    private static final String PKG = "com.alexxiconify.rustmc.mixin.";
    private static final Map<String, BooleanSupplier> MIXIN_CONDITIONS;
    private static final Map<String, Long> groupTimings = new ConcurrentHashMap<>();
    private static final ThreadLocal<Long> applyStartNanos = new ThreadLocal<>();
    private static final Map<String, String> MIXIN_GROUP_PREFIXES = buildGroupPrefixes();
    private static Map<String, String> buildGroupPrefixes() {
        String dnsGroup = "DNS/Server List";
        Map<String, String> m = new LinkedHashMap<>();
        m.put("compat.EntityRender", "Entity Rendering (EMF/ETF/IF)");
        m.put("compat.ClientRedstone", "Redstone Optimization");
        m.put("compat.TickSync", "Tick Sync");
        m.put("compat.MiniHUD", "MiniHUD/Lighty");
        m.put("minihud.", "MiniHUD Culling");
        m.put("screen.", "Screen Overlays");
        m.put("Lighting", "Lighting Engine");
        m.put("Frustum", "Frustum/Raycast Culling");
        m.put("BoxMixin", "Frustum/Raycast Culling");
        m.put("Particle", "Particle Culling");
        m.put("ChunkBuilder", "Chunk Builder Threads");
        m.put("Resource", "Resource Reload");
        m.put("Bootstrap", "Bootstrap/DFU");
        m.put("Schemas", "Bootstrap/DFU");
        m.put("ServerPinger", dnsGroup);
        m.put("ServerAddress", dnsGroup);
        m.put("Multiplayer", dnsGroup);
        m.put("DebugHud", "Debug HUD Overlays");
        m.put("MinecraftClient", "Frame Timing");
        m.put("RenderBudget", "Render Budget");
        return Collections.unmodifiableMap(m);
    }
    static {
        MIXIN_CONDITIONS = Map.ofEntries(
            Map.entry(PKG + "MatrixMixin", ModBridge :: isMathOwned),
            Map.entry( PKG + "LightingMixin", ModBridge :: isLightingOwned ),
            Map.entry(PKG + "ChunkBuilderMixin", () -> RustMC.CONFIG.isEnableChunkBuilderExpand() && !ModBridge.SODIUM),
            Map.entry(PKG + "compat.ClientRedstoneSkipMixin", RustMC.CONFIG :: isEnableClientRedstoneSkip),
            Map.entry(PKG + "compat.TickSyncCompatMixin", RustMC.CONFIG :: isEnableTickSyncCompat),
            Map.entry(PKG + "compat.EntityRenderCompatMixin", () ->
                RustMC.CONFIG.isEnableBBECompat()
                ||
                RustMC.CONFIG.isEnableEMFCompat()
                || RustMC.CONFIG.isEnableETFCompat()
                || RustMC.CONFIG.isEnableEntityCullingCompat()
                || RustMC.CONFIG.isEnableImmediatelyFastCompat()),
            Map.entry(PKG + "network.ServerPingerMixin", RustMC.CONFIG :: isEnableDnsCache),
            Map.entry(PKG + "network.ServerAddressMixin", RustMC.CONFIG :: isEnableDnsCache),
            Map.entry(PKG + "network.MultiplayerScreenMixin", RustMC.CONFIG :: isEnableDnsCache)
        );
    }
    @Override
    public void onLoad(String mixinPackage) {
        RustMC.LOGGER.info("[Rust-MC] MixinManager loaded for package: {}", mixinPackage);
    }
    @Override
    public String getRefMapperConfig() { return null; }
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        BooleanSupplier condition = MIXIN_CONDITIONS.get(mixinClassName);
        if (condition != null) return condition.getAsBoolean();
        return true;
    }
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No target filtering needed — all targets accepted
    }
    @Override
    public List<String> getMixins() { return Collections.emptyList(); }
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        applyStartNanos.set(System.nanoTime());
    }
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        Long start = applyStartNanos.get();
        if (start == null) return;
        long elapsed = System.nanoTime() - start;
        applyStartNanos.remove();
        String group = classifyMixin(mixinClassName);
        groupTimings.merge( group, elapsed, Long :: sum );
    }
    @SuppressWarnings({"java:S3776", "CognitiveComplexity"})
    private static String classifyMixin(String mixinClassName) {
        for (Map.Entry<String, String> entry : MIXIN_GROUP_PREFIXES.entrySet()) {
            if (mixinClassName.contains(entry.getKey())) return entry.getValue();
        }
        int dot = mixinClassName.lastIndexOf('.');
        return dot >= 0 ? mixinClassName.substring(dot + 1) : mixinClassName;
    }
    //
     // Flushes per-group mixin timings into the BlameLog.
     // Called once from mod init after all mixins are applied.
    public static void flushBlameTimings() {
        if (groupTimings.isEmpty()) return;
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(groupTimings.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        for (Map.Entry<String, Long> entry : sorted) {
            long ms = entry.getValue() / 1_000_000;
            if (ms > 0) {
                BlameLog.begin("Mixin: " + entry.getKey());
                BlameLog.end();
            }
        }
        long totalMs = groupTimings.values().stream().mapToLong(Long::longValue).sum() / 1_000_000;
        RustMC.LOGGER.info("[Rust-MC] Mixin application totals ({}ms across {} groups):", totalMs, groupTimings.size());
        for (Map.Entry<String, Long> entry : sorted) {
            RustMC.LOGGER.info("[Rust-MC]   {}: {}ms", entry.getKey(), entry.getValue() / 1_000_000);
        }
    }
    public static Map<String, Long> getGroupTimings() {
        return Map.copyOf(groupTimings);
    }
}