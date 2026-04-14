package com.alexxiconify.rustmc.config;
//
 //  Configuration POJO for Rust-MC. All fields are serialized/deserialized by Gson,
 //  and getters/setters are referenced by ModMenu (YACL) via method references.
public class RustMCConfig {
    // Bump this whenever the ModMenu config surface changes so old saved values are reset.
    public static final String CURRENT_CONFIG_VERSION = "2.2.0";
    private String configVersion = CURRENT_CONFIG_VERSION;

    // Math/debug overlays
    private boolean useNativeF3       = true;
    // World / system features
    private boolean useNativeLighting    = true;
    private boolean useNativeCompression = true;
    private boolean useNativePathfinding = true;
    private boolean useNativeCulling     = true;
    private boolean useFastLoadingScreen = false;
    private boolean useNativeCommands    = false;
    // Mod compat toggles
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
    private boolean enableDhCaveCulling        = true;
    private boolean enableDhCullingDebugLog    = false;
    private String dhCullingSpaceMode          = DH_CULLING_SPACE_AUTO;
    private boolean enableDebugHudGraph        = false;
    private boolean enablePieChart             = false;
    private boolean enableNativeMetricsHud     = false;
    // DNS / Server List
    private boolean enableDnsCache             = true;
    // Mod bridges
    private boolean bridgeSodium    = true;
    private boolean bridgeStarlight = true;
    private boolean bridgeC2ME      = true;
    private boolean bridgeIris      = true;
    private boolean bridgeLithium   = true;
    // Loading screen colors
    private int loadingBarBgColor      = 0xFF1A1A1A;
    private int loadingBarLowColor     = 0xFF22AA44;
    private int loadingBarMidColor     = 0xFFCCAA00;
    private int loadingBarHighColor    = 0xFFCC2222;
    private int loadingBarTextColor    = 0xDDFFFFFF;
    private int loadingBarSubtextColor = 0x9900FFFF;
    // Developer
    private boolean silenceLogs = true;
    private boolean nativeReady = false;
    private boolean experimentalCoexistEnabled = true;

    public static final String DH_CULLING_SPACE_AUTO = "auto";
    public static final String DH_CULLING_SPACE_ABSOLUTE = "absolute";
    public static final String DH_CULLING_SPACE_PLUS_CAMERA = "plus_camera";
    public static final String DH_CULLING_SPACE_MINUS_CAMERA = "minus_camera";
    public void copyFrom(RustMCConfig o) {
        this.configVersion = o.configVersion;
        this.useNativeF3 = o.useNativeF3;
        this.useNativeLighting = o.useNativeLighting;
        this.useNativeCompression = o.useNativeCompression;
        this.useNativePathfinding = o.useNativePathfinding;
        this.useNativeCulling = o.useNativeCulling;
        this.useFastLoadingScreen = o.useFastLoadingScreen;
        this.useNativeCommands = o.useNativeCommands;
        this.enableParticleCulling = o.enableParticleCulling;
        this.enableChunkBuilderExpand = o.enableChunkBuilderExpand;
        this.enableTickSyncCompat = o.enableTickSyncCompat;
        this.enableBBECompat = o.enableBBECompat;
        this.enableEMFCompat = o.enableEMFCompat;
        this.enableETFCompat = o.enableETFCompat;
        this.enableAppleSkinCompat = o.enableAppleSkinCompat;
        this.enableEntityCullingCompat = o.enableEntityCullingCompat;
        this.enableImmediatelyFastCompat = o.enableImmediatelyFastCompat;
        this.enableClientRedstoneSkip = o.enableClientRedstoneSkip;
        this.enableDhCaveCulling = o.enableDhCaveCulling;
        this.enableDhCullingDebugLog = o.enableDhCullingDebugLog;
        this.dhCullingSpaceMode = normalizeDhCullingSpaceMode(o.dhCullingSpaceMode);
        this.enableDebugHudGraph = o.enableDebugHudGraph;
        this.enablePieChart = o.enablePieChart;
        this.enableNativeMetricsHud = o.enableNativeMetricsHud;
        this.enableDnsCache = o.enableDnsCache;
        this.bridgeSodium = o.bridgeSodium;
        this.bridgeStarlight = o.bridgeStarlight;
        this.bridgeC2ME = o.bridgeC2ME;
        this.bridgeIris = o.bridgeIris;
        this.bridgeLithium = o.bridgeLithium;
        this.loadingBarBgColor = o.loadingBarBgColor;
        this.loadingBarLowColor = o.loadingBarLowColor;
        this.loadingBarMidColor = o.loadingBarMidColor;
        this.loadingBarHighColor = o.loadingBarHighColor;
        this.loadingBarTextColor = o.loadingBarTextColor;
        this.loadingBarSubtextColor = o.loadingBarSubtextColor;
        this.silenceLogs = o.silenceLogs;
        this.nativeReady = o.nativeReady;
        this.experimentalCoexistEnabled = o.experimentalCoexistEnabled;
    }
    // Getters
    public String getConfigVersion()        { return configVersion; }
    public boolean isUseNativeF3()          { return useNativeF3; }
    public boolean isUseNativeLighting()    { return useNativeLighting; }
    public boolean isUseNativeCompression() { return useNativeCompression; }
    public boolean isUseNativePathfinding() { return useNativePathfinding; }
    public boolean isUseNativeCulling()     { return useNativeCulling; }
    public boolean isUseFastLoadingScreen() { return useFastLoadingScreen; }
    public boolean isUseNativeCommands()    { return useNativeCommands; }
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
    public boolean isEnableDhCaveCulling()        { return enableDhCaveCulling; }
    public boolean isEnableDhCullingDebugLog()    { return enableDhCullingDebugLog; }
    public String getDhCullingSpaceMode()         { return normalizeDhCullingSpaceMode(dhCullingSpaceMode); }
    public boolean isEnableDebugHudGraph()        { return enableDebugHudGraph; }
    public boolean isEnablePieChart()             { return enablePieChart; }
    public boolean isEnableNativeMetricsHud()     { return enableNativeMetricsHud; }
    public boolean isEnableDnsCache()             { return enableDnsCache; }
    public boolean isBridgeSodium()         { return bridgeSodium; }
    public boolean isBridgeStarlight()      { return bridgeStarlight; }
    public boolean isBridgeC2ME()           { return bridgeC2ME; }
    public boolean isBridgeIris()           { return bridgeIris; }
    public boolean isBridgeLithium()        { return bridgeLithium; }
    public int getLoadingBarBgColor()       { return loadingBarBgColor; }
    public int getLoadingBarLowColor()      { return loadingBarLowColor; }
    public int getLoadingBarMidColor()      { return loadingBarMidColor; }
    public int getLoadingBarHighColor()     { return loadingBarHighColor; }
    public int getLoadingBarTextColor()     { return loadingBarTextColor; }
    public int getLoadingBarSubtextColor()  { return loadingBarSubtextColor; }
    public boolean isSilenceLogs()          { return silenceLogs; }
    @SuppressWarnings("unused")
    public boolean isNativeReady()          { return nativeReady; }
    public boolean isExperimentalCoexistEnabled() { return experimentalCoexistEnabled; }
    // Setters
    public void setConfigVersion(String v)       { configVersion = v; }
    public void setUseNativeF3(boolean v)          { useNativeF3 = v; }
    public void setUseNativeLighting(boolean v)    { useNativeLighting = v; }
    public void setUseNativeCompression(boolean v) { useNativeCompression = v; }
    public void setUseNativePathfinding(boolean v) { useNativePathfinding = v; }
    public void setUseNativeCulling(boolean v)     { useNativeCulling = v; }
    public void setUseFastLoadingScreen(boolean v) { useFastLoadingScreen = v; }
    public void setUseNativeCommands(boolean v)    { useNativeCommands = v; }
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
    public void setEnableDhCaveCulling(boolean v)        { enableDhCaveCulling = v; }
    public void setEnableDhCullingDebugLog(boolean v)    { enableDhCullingDebugLog = v; }
    public void setDhCullingSpaceMode(String mode)       { dhCullingSpaceMode = normalizeDhCullingSpaceMode(mode); }
    public void setEnableDebugHudGraph(boolean v)        { enableDebugHudGraph = v; }
    public void setEnablePieChart(boolean v)             { enablePieChart = v; }
    public void setEnableNativeMetricsHud(boolean v)     { enableNativeMetricsHud = v; }
    public void setEnableDnsCache(boolean v)             { enableDnsCache = v; }
    public void setBridgeSodium(boolean v)         { bridgeSodium = v; }
    public void setBridgeStarlight(boolean v)      { bridgeStarlight = v; }
    public void setBridgeC2ME(boolean v)           { bridgeC2ME = v; }
    public void setBridgeIris(boolean v)           { bridgeIris = v; }
    public void setBridgeLithium(boolean v)        { bridgeLithium = v; }
    public void setLoadingBarBgColor(int v)        { loadingBarBgColor = v; }
    public void setLoadingBarLowColor(int v)       { loadingBarLowColor = v; }
    public void setLoadingBarMidColor(int v)       { loadingBarMidColor = v; }
    public void setLoadingBarHighColor(int v)      { loadingBarHighColor = v; }
    public void setLoadingBarTextColor(int v)      { loadingBarTextColor = v; }
    public void setLoadingBarSubtextColor(int v)   { loadingBarSubtextColor = v; }
    public void setSilenceLogs(boolean v)          { silenceLogs = v; }
    @SuppressWarnings("unused")
    public void setNativeReady(boolean v)          { nativeReady = v; }
    @SuppressWarnings("unused")
    public void setExperimentalCoexistEnabled(boolean v) { experimentalCoexistEnabled = v; }

    public static String normalizeDhCullingSpaceMode(String mode) {
        if (mode == null) {
            return DH_CULLING_SPACE_AUTO;
        }
        return switch (mode.trim().toLowerCase(java.util.Locale.ROOT)) {
            case DH_CULLING_SPACE_ABSOLUTE -> DH_CULLING_SPACE_ABSOLUTE;
            case DH_CULLING_SPACE_PLUS_CAMERA -> DH_CULLING_SPACE_PLUS_CAMERA;
            case DH_CULLING_SPACE_MINUS_CAMERA -> DH_CULLING_SPACE_MINUS_CAMERA;
            default -> DH_CULLING_SPACE_AUTO;
        };
    }

    public static String nextDhCullingSpaceMode(String current) {
        return switch (normalizeDhCullingSpaceMode(current)) {
            case DH_CULLING_SPACE_AUTO -> DH_CULLING_SPACE_ABSOLUTE;
            case DH_CULLING_SPACE_ABSOLUTE -> DH_CULLING_SPACE_PLUS_CAMERA;
            case DH_CULLING_SPACE_PLUS_CAMERA -> DH_CULLING_SPACE_MINUS_CAMERA;
            default -> DH_CULLING_SPACE_AUTO;
        };
    }
}