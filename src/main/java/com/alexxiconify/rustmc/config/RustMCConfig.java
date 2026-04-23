package com.alexxiconify.rustmc.config;
//  Configuration POJO for Rust-MC. All fields are serialized/deserialized by Gson,
//  and getters/setters are referenced by ModMenu (YACL) via method references.
public class RustMCConfig {
    // Bump this whenever the ModMenu config surface changes so old saved values are reset.
    public static final String CURRENT_CONFIG_VERSION = "2.6.0";
    private String configVersion = CURRENT_CONFIG_VERSION;
    // Math/debug overlays
    private boolean useNativeF3       = true;
    // World / system features
    private boolean useNativeLighting    = true;
    private boolean useFastLoadingScreen = true;
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
    private boolean enableDebugHudGraph        = false;
    private boolean enablePieChart             = false;
    private boolean enableNativeMetricsHud     = false;
    // DNS / Server List
    private boolean enableDnsCache             = true;
    private boolean enableChunkIngestOffload   = false;
    // Mod bridges
    private boolean bridgeSodium    = true;
    private boolean bridgeC2ME      = true;
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

    public void copyFrom(RustMCConfig o) {
        this.configVersion = o.configVersion;
        this.useNativeF3 = o.useNativeF3;
        this.useNativeLighting = o.useNativeLighting;
        this.useFastLoadingScreen = o.useFastLoadingScreen;
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
        this.enableDebugHudGraph = o.enableDebugHudGraph;
        this.enablePieChart = o.enablePieChart;
        this.enableNativeMetricsHud = o.enableNativeMetricsHud;
        this.enableDnsCache = o.enableDnsCache;
        this.enableChunkIngestOffload = o.enableChunkIngestOffload;
        this.bridgeSodium = o.bridgeSodium;
        this.bridgeC2ME = o.bridgeC2ME;
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
    public boolean isUseFastLoadingScreen() { return useFastLoadingScreen; }
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
    public boolean isDebugHudGraphEnabled()       { return enableDebugHudGraph; }
    public boolean isEnablePieChart()             { return enablePieChart; }
    public boolean isEnableNativeMetricsHud()     { return enableNativeMetricsHud; }
    public boolean isEnableDnsCache()             { return enableDnsCache; }
    public boolean isEnableChunkIngestOffload()   { return enableChunkIngestOffload; }
    public boolean isBridgeSodium()         { return bridgeSodium; }
    public boolean isBridgeC2ME()           { return bridgeC2ME; }
    public boolean isBridgeLithium()        { return bridgeLithium; }
    public int getLoadingBarBgColor()       { return loadingBarBgColor; }
    public int getLoadingBarLowColor()      { return loadingBarLowColor; }
    public int getLoadingBarMidColor()      { return loadingBarMidColor; }
    public int getLoadingBarHighColor()     { return loadingBarHighColor; }
    public int getLoadingBarTextColor()     { return loadingBarTextColor; }
    public int getLoadingBarSubtextColor()  { return loadingBarSubtextColor; }
    public boolean isSilenceLogs()          { return silenceLogs; }
    public boolean isNativeReady()          { return nativeReady; }
    public boolean isExperimentalCoexistEnabled() { return experimentalCoexistEnabled; }
    public void setConfigVersion(String v)       { configVersion = v; }
    public void setUseNativeF3(boolean v)          { useNativeF3 = v; }
    public void setUseNativeLighting(boolean v)    { useNativeLighting = v; }
    public void setUseFastLoadingScreen(boolean v) { useFastLoadingScreen = v; }
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
    public void setDebugHudGraphEnabled(boolean v)       { enableDebugHudGraph = v; }
    public void setEnablePieChart(boolean v)             { enablePieChart = v; }
    public void setEnableNativeMetricsHud(boolean v)     { enableNativeMetricsHud = v; }
    public void setEnableDnsCache(boolean v)             { enableDnsCache = v; }
    public void setEnableChunkIngestOffload(boolean v)   { enableChunkIngestOffload = v; }
    public void setBridgeSodium(boolean v)         { bridgeSodium = v; }
    public void setBridgeC2ME(boolean v)           { bridgeC2ME = v; }
    public void setBridgeLithium(boolean v)        { bridgeLithium = v; }
    public void setLoadingBarBgColor(int v)        { loadingBarBgColor = v; }
    public void setLoadingBarLowColor(int v)       { loadingBarLowColor = v; }
    public void setLoadingBarMidColor(int v)       { loadingBarMidColor = v; }
    public void setLoadingBarHighColor(int v)      { loadingBarHighColor = v; }
    public void setLoadingBarTextColor(int v)      { loadingBarTextColor = v; }
    public void setLoadingBarSubtextColor(int v)   { loadingBarSubtextColor = v; }
    public void setSilenceLogs(boolean v)          { silenceLogs = v; }
    public void setNativeReady(boolean v)          { nativeReady = v; }
    public void setExperimentalCoexistEnabled(boolean v) { experimentalCoexistEnabled = v; }
}