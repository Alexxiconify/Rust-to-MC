package com.alexxiconify.rustmc.config;

/**
 * Configuration POJO for Rust-MC. All fields are serialized/deserialized by Gson,
 * and getters/setters are referenced by ModMenu (YACL) via method references.
 * Static analysis may report some as "unused" but they are all part of the config surface.
 */
@SuppressWarnings("unused")
public class RustMCConfig {
    // Math optimizations
    private boolean useNativeSine     = true;
    private boolean useNativeCos      = true;
    private boolean useNativeSqrt     = true;
    private boolean useNativeInvSqrt  = true;
    private boolean useNativeAtan2    = true;
    private boolean useNativeFloor    = true;
    private boolean useNativeNoise    = true;
    private boolean useNativeF3       = true;

    // World / system features
    private boolean useNativeLighting    = true;
    private boolean useNativeCompression = true;
    private boolean useNativePathfinding = true;
    private boolean useNativeCulling     = true;
    private boolean useFastLoadingScreen = false;
    private boolean useNativeCommands    = false;
    public enum GhostMapMode {
        NONE, DH_ONLY, SEED_ONLY, DH_THEN_SEED
    }

    private boolean limitXaeroMinimap    = true;
    private GhostMapMode ghostMapMode    = GhostMapMode.DH_THEN_SEED;
    private String customGhostMapSeed    = "609567216262790763";

    // Mod compat toggles (new — each can be disabled in ModMenu)
    private boolean enableParticleCulling      = true;
    private boolean enableChunkBuilderExpand   = true;
    private boolean enableTickSyncCompat       = true;
    private boolean enableBBECompat            = true;
    private boolean enableEMFCompat            = true;
    private boolean enableETFCompat            = true;
    private boolean enableAppleSkinCompat      = true;
    private boolean enableEntityCullingCompat  = true;
    private boolean enableImmediatelyFastCompat = true;
    private boolean enableClientRedstoneSkip   = true;
    private boolean enableDebugHudGraph        = false;
    private boolean enablePieChart             = false;

    // DNS / Server List
    private boolean enableDnsCache             = true;

    // Mod bridges
    private boolean bridgeSodium    = true;
    private boolean bridgeStarlight = true;
    private boolean bridgeC2ME      = true;
    private boolean bridgeIris      = true;
    private boolean bridgeLithium   = true;
    private boolean disableDhFade   = true;

    // Loading screen colors (ARGB int)
    private int loadingBarBgColor      = 0xFF1A1A1A;
    private int loadingBarLowColor     = 0xFF22AA44;
    private int loadingBarMidColor     = 0xFFCCAA00;
    private int loadingBarHighColor    = 0xFFCC2222;
    private int loadingBarTextColor    = 0xDDFFFFFF;
    private int loadingBarSubtextColor = 0x9900FFFF;

    // Developer
    private boolean silenceLogs = true;
    private boolean nativeReady = false;

    public void copyFrom(RustMCConfig o) {
        useNativeSine         = o.useNativeSine;
        useNativeCos          = o.useNativeCos;
        useNativeSqrt         = o.useNativeSqrt;
        useNativeInvSqrt      = o.useNativeInvSqrt;
        useNativeAtan2        = o.useNativeAtan2;
        useNativeFloor        = o.useNativeFloor;
        useNativeNoise        = o.useNativeNoise;
        useNativeF3           = o.useNativeF3;
        useNativeLighting     = o.useNativeLighting;
        useNativeCompression  = o.useNativeCompression;
        useNativePathfinding  = o.useNativePathfinding;
        useNativeCulling      = o.useNativeCulling;
        useFastLoadingScreen  = o.useFastLoadingScreen;
        useNativeCommands     = o.useNativeCommands;
        limitXaeroMinimap     = o.limitXaeroMinimap;
        enableParticleCulling      = o.enableParticleCulling;
        enableChunkBuilderExpand   = o.enableChunkBuilderExpand;
        enableTickSyncCompat       = o.enableTickSyncCompat;
        enableBBECompat            = o.enableBBECompat;
        enableEMFCompat            = o.enableEMFCompat;
        enableETFCompat            = o.enableETFCompat;
        enableAppleSkinCompat      = o.enableAppleSkinCompat;
        enableEntityCullingCompat  = o.enableEntityCullingCompat;
        enableImmediatelyFastCompat = o.enableImmediatelyFastCompat;
        enableClientRedstoneSkip   = o.enableClientRedstoneSkip;
        enableDebugHudGraph        = o.enableDebugHudGraph;
        enablePieChart             = o.enablePieChart;
        enableDnsCache             = o.enableDnsCache;
        bridgeSodium          = o.bridgeSodium;
        bridgeStarlight       = o.bridgeStarlight;
        bridgeC2ME            = o.bridgeC2ME;
        bridgeIris            = o.bridgeIris;
        bridgeLithium         = o.bridgeLithium;
        disableDhFade         = o.disableDhFade;
        ghostMapMode          = o.ghostMapMode;
        customGhostMapSeed    = o.customGhostMapSeed;
        loadingBarBgColor     = o.loadingBarBgColor;
        loadingBarLowColor    = o.loadingBarLowColor;
        loadingBarMidColor    = o.loadingBarMidColor;
        loadingBarHighColor   = o.loadingBarHighColor;
        loadingBarTextColor   = o.loadingBarTextColor;
        loadingBarSubtextColor = o.loadingBarSubtextColor;
        silenceLogs           = o.silenceLogs;
        nativeReady           = o.nativeReady;
    }

    // Getters
    public boolean isUseNativeSine()        { return useNativeSine; }
    public boolean isUseNativeCos()         { return useNativeCos; }
    public boolean isUseNativeSqrt()        { return useNativeSqrt; }
    public boolean isUseNativeInvSqrt()     { return useNativeInvSqrt; }
    public boolean isUseNativeAtan2()       { return useNativeAtan2; }
    public boolean isUseNativeFloor()       { return useNativeFloor; }
    public boolean isUseNativeNoise()       { return useNativeNoise; }
    public boolean isUseNativeF3()          { return useNativeF3; }
    public boolean isUseNativeLighting()    { return useNativeLighting; }
    public boolean isUseNativeCompression() { return useNativeCompression; }
    public boolean isUseNativePathfinding() { return useNativePathfinding; }
    public boolean isUseNativeCulling()     { return useNativeCulling; }
    public boolean isUseFastLoadingScreen() { return useFastLoadingScreen; }
    public boolean isUseNativeCommands()    { return useNativeCommands; }
    public boolean isLimitXaeroMinimap()    { return limitXaeroMinimap; }
    public boolean isGhostMapEnabled()      { return ghostMapMode == GhostMapMode.NONE; }
    public boolean isEnableParticleCulling()      { return enableParticleCulling; }
    public boolean isEnableChunkBuilderExpand()   { return enableChunkBuilderExpand; }
    public boolean isEnableTickSyncCompat()       { return enableTickSyncCompat; }
    public boolean isEnableBBECompat()            { return enableBBECompat; }
    public boolean isEnableEMFCompat()            { return enableEMFCompat; }
    public boolean isEnableETFCompat()            { return enableETFCompat; }
    public boolean isEnableAppleSkinCompat()      { return enableAppleSkinCompat; }
    public boolean isEnableEntityCullingCompat()  { return enableEntityCullingCompat; }
    public boolean isEnableImmediatelyFastCompat() { return enableImmediatelyFastCompat; }
    public boolean isEnableClientRedstoneSkip()   { return enableClientRedstoneSkip; }
    public boolean isEnableDebugHudGraph()        { return enableDebugHudGraph; }
    public boolean isEnablePieChart()             { return enablePieChart; }
    public boolean isEnableDnsCache()             { return enableDnsCache; }
    public boolean isBridgeSodium()         { return bridgeSodium; }
    public boolean isBridgeStarlight()      { return bridgeStarlight; }
    public boolean isBridgeC2ME()           { return bridgeC2ME; }
    public boolean isBridgeIris()           { return bridgeIris; }
    public boolean isBridgeLithium()        { return bridgeLithium; }
    public boolean isDisableDhFade()        { return disableDhFade; }
    public GhostMapMode getGhostMapMode()   { return ghostMapMode; }
    public String getCustomGhostMapSeed()    { return customGhostMapSeed; }
    public int getLoadingBarBgColor()       { return loadingBarBgColor; }
    public int getLoadingBarLowColor()      { return loadingBarLowColor; }
    public int getLoadingBarMidColor()      { return loadingBarMidColor; }
    public int getLoadingBarHighColor()     { return loadingBarHighColor; }
    public int getLoadingBarTextColor()     { return loadingBarTextColor; }
    public int getLoadingBarSubtextColor()  { return loadingBarSubtextColor; }
    public boolean isSilenceLogs()          { return silenceLogs; }
    public boolean isNativeReady()          { return nativeReady; }

    // Setters
    public void setUseNativeSine(boolean v)        { useNativeSine = v; }
    public void setUseNativeCos(boolean v)         { useNativeCos = v; }
    public void setUseNativeSqrt(boolean v)        { useNativeSqrt = v; }
    public void setUseNativeInvSqrt(boolean v)     { useNativeInvSqrt = v; }
    public void setUseNativeAtan2(boolean v)       { useNativeAtan2 = v; }
    public void setUseNativeFloor(boolean v)       { useNativeFloor = v; }
    public void setUseNativeNoise(boolean v)       { useNativeNoise = v; }
    public void setUseNativeLighting(boolean v)    { useNativeLighting = v; }
    public void setUseNativeCompression(boolean v) { useNativeCompression = v; }
    public void setUseNativePathfinding(boolean v) { useNativePathfinding = v; }
    public void setUseNativeCulling(boolean v)     { useNativeCulling = v; }
    public void setUseFastLoadingScreen(boolean v) { useFastLoadingScreen = v; }
    public void setUseNativeCommands(boolean v)    { useNativeCommands = v; }
    public void setLimitXaeroMinimap(boolean v)    { limitXaeroMinimap = v; }
    public void setEnableParticleCulling(boolean v)      { enableParticleCulling = v; }
    public void setEnableChunkBuilderExpand(boolean v)   { enableChunkBuilderExpand = v; }
    public void setEnableTickSyncCompat(boolean v)       { enableTickSyncCompat = v; }
    public void setEnableBBECompat(boolean v)            { enableBBECompat = v; }
    public void setEnableEMFCompat(boolean v)            { enableEMFCompat = v; }
    public void setEnableETFCompat(boolean v)            { enableETFCompat = v; }
    public void setEnableAppleSkinCompat(boolean v)      { enableAppleSkinCompat = v; }
    public void setEnableEntityCullingCompat(boolean v)  { enableEntityCullingCompat = v; }
    public void setEnableImmediatelyFastCompat(boolean v) { enableImmediatelyFastCompat = v; }
    public void setEnableClientRedstoneSkip(boolean v)   { enableClientRedstoneSkip = v; }
    public void setEnableDebugHudGraph(boolean v)        { enableDebugHudGraph = v; }
    public void setEnablePieChart(boolean v)             { enablePieChart = v; }
    public void setEnableDnsCache(boolean v)             { enableDnsCache = v; }
    public void setBridgeSodium(boolean v)         { bridgeSodium = v; }
    public void setBridgeStarlight(boolean v)      { bridgeStarlight = v; }
    public void setBridgeC2ME(boolean v)           { bridgeC2ME = v; }
    public void setBridgeIris(boolean v)           { bridgeIris = v; }
    public void setBridgeLithium(boolean v)        { bridgeLithium = v; }
    public void setDisableDhFade(boolean v)        { disableDhFade = v; }
    public void setGhostMapMode(GhostMapMode v)    { ghostMapMode = v; }
    public void setCustomGhostMapSeed(String v)    { customGhostMapSeed = v; }
    public void setLoadingBarBgColor(int v)        { loadingBarBgColor = v; }
    public void setLoadingBarLowColor(int v)       { loadingBarLowColor = v; }
    public void setLoadingBarMidColor(int v)       { loadingBarMidColor = v; }
    public void setLoadingBarHighColor(int v)      { loadingBarHighColor = v; }
    public void setLoadingBarTextColor(int v)      { loadingBarTextColor = v; }
    public void setLoadingBarSubtextColor(int v)   { loadingBarSubtextColor = v; }
    public void setSilenceLogs(boolean v)          { silenceLogs = v; }
    public void setNativeReady(boolean v)          { nativeReady = v; }
}