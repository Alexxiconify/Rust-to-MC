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
        m.put("integration.EntityRender", "Entity Rendering (EMF/ETF/IF)");
        m.put("integration.ClientRedstone", "Redstone Optimization");
        m.put("integration.TickSync", "Tick Sync");
        m.put("integration.MiniHUD", "MiniHUD/Lighty");
        m.put("integration.RenderUtils", "MiniHUD Culling");
        m.put("screen.", "Screen Overlays");
        m.put("Lighting", "Lighting Engine");
        m.put("Frustum", "Frustum/Raycast Culling");
        m.put("BoxMixin", "Frustum/Raycast Culling");
        m.put("Particle", "Particle Culling");
        m.put("ChunkBuilder", "Chunk Builder Threads");
        m.put("Resource", "Resource Reload");
        m.put("Bootstrap", "Bootstrap/DFU");
        m.put("ServerPinger", dnsGroup);
        m.put("ServerAddress", dnsGroup);
        m.put("Multiplayer", dnsGroup);
        m.put("network.ClientPlayNetworkHandler", "Chunk Ingest Offload");
        m.put("DebugHud", "Debug HUD Overlays");
        m.put("MinecraftClient", "Frame Timing");
        m.put("RenderBudget", "Render Budget");
        return Collections.unmodifiableMap(m);
    }
    static {
        MIXIN_CONDITIONS = Map.ofEntries(
            Map.entry(PKG + "performance.MatrixMixin", ModBridge :: isMathOwned),
            Map.entry(PKG + "performance.LightingMixin", ModBridge :: isLightingOwned),
            Map.entry(PKG + "performance.ChunkBuilderMixin", () -> RustMC.CONFIG.isEnableChunkBuilderExpand() && !ModBridge.SODIUM),
            Map.entry(PKG + "integration.ClientRedstoneSkipMixin", RustMC.CONFIG :: isEnableClientRedstoneSkip),
            Map.entry(PKG + "integration.TickSyncCompatMixin", RustMC.CONFIG :: isEnableTickSyncCompat),
            Map.entry(PKG + "integration.EntityRenderCompatMixin", () ->
                RustMC.CONFIG.isEnableBBECompat()
                ||
                RustMC.CONFIG.isEnableEMFCompat()
                || RustMC.CONFIG.isEnableETFCompat()
                || RustMC.CONFIG.isEnableEntityCullingCompat()
                || RustMC.CONFIG.isEnableImmediatelyFastCompat()),
            Map.entry(PKG + "network.ServerPingerMixin", RustMC.CONFIG :: isEnableDnsCache),
            Map.entry(PKG + "network.ServerAddressMixin", RustMC.CONFIG :: isEnableDnsCache),
            Map.entry(PKG + "network.MultiplayerScreenMixin", RustMC.CONFIG :: isEnableDnsCache),
            Map.entry(PKG + "network.ClientPlayNetworkHandlerMixin", RustMC.CONFIG :: isEnableChunkIngestOffload)
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
    // Flushes per-group mixin timings into the BlameLog after startup.
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