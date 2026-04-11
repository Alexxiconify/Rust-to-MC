package com.alexxiconify.rustmc.config;

// Configuration POJO for Rust-MC. All fields are serialized/deserialized by Gson, and getters/setters are referenced by ModMenu (YACL) via method references.
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
    private boolean useNativeRandom   = true;

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
    private boolean enableDebugHudGraph        = false;
    private boolean enablePieChart             = false;
    private boolean enableNativeMetricsHud    = false;

    // DNS / Server List
    private boolean enableDnsCache             = true;

    // Mod bridges
    private boolean bridgeSodium    = true;
    private boolean bridgeStarlight = true;
    private boolean bridgeC2ME      = true;
    private boolean bridgeIris      = true;
    private boolean bridgeLithium   = true;
    private boolean disableDhFade   = true;

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
        this.useNativeSine = o.useNativeSine;
        this.useNativeCos = o.useNativeCos;
        this.useNativeSqrt = o.useNativeSqrt;
        this.useNativeInvSqrt = o.useNativeInvSqrt;
        this.useNativeAtan2 = o.useNativeAtan2;
        this.useNativeFloor = o.useNativeFloor;
        this.useNativeNoise = o.useNativeNoise;
        this.useNativeF3 = o.useNativeF3;
        this.useNativeRandom = o.useNativeRandom;
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
        this.enableDebugHudGraph = o.enableDebugHudGraph;
        this.enablePieChart = o.enablePieChart;
        this.enableNativeMetricsHud = o.enableNativeMetricsHud;
        this.enableDnsCache = o.enableDnsCache;
        this.bridgeSodium = o.bridgeSodium;
        this.bridgeStarlight = o.bridgeStarlight;
        this.bridgeC2ME = o.bridgeC2ME;
        this.bridgeIris = o.bridgeIris;
        this.bridgeLithium = o.bridgeLithium;
        this.disableDhFade = o.disableDhFade;
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
    public boolean isUseNativeSine()        { return useNativeSine; }
    public boolean isUseNativeCos()         { return useNativeCos; }
    public boolean isUseNativeSqrt()        { return useNativeSqrt; }
    public boolean isUseNativeInvSqrt()     { return useNativeInvSqrt; }
    public boolean isUseNativeAtan2()       { return useNativeAtan2; }
    public boolean isUseNativeFloor()       { return useNativeFloor; }
    public boolean isUseNativeNoise()       { return useNativeNoise; }
    public boolean isUseNativeF3()          { return useNativeF3; }
    public boolean isEnableNativeRandom()   { return useNativeRandom; }
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
    public boolean isEnableDebugHudGraph()        { return enableDebugHudGraph; }
    public boolean isEnablePieChart()             { return enablePieChart; }
    public boolean isEnableNativeMetricsHud()    { return enableNativeMetricsHud; }
    public boolean isEnableDnsCache()             { return enableDnsCache; }
    public boolean isBridgeSodium()         { return bridgeSodium; }
    public boolean isBridgeStarlight()      { return bridgeStarlight; }
    public boolean isBridgeC2ME()           { return bridgeC2ME; }
    public boolean isBridgeIris()           { return bridgeIris; }
    public boolean isBridgeLithium()        { return bridgeLithium; }
    public boolean isDisableDhFade()        { return disableDhFade; }
    public int getLoadingBarBgColor()       { return loadingBarBgColor; }
    public int getLoadingBarLowColor()      { return loadingBarLowColor; }
    public int getLoadingBarMidColor()      { return loadingBarMidColor; }
    public int getLoadingBarHighColor()     { return loadingBarHighColor; }
    public int getLoadingBarTextColor()     { return loadingBarTextColor; }
    public int getLoadingBarSubtextColor()  { return loadingBarSubtextColor; }
    public boolean isSilenceLogs()          { return silenceLogs; }
    public boolean isNativeReady()          { return nativeReady; }
    public boolean isExperimentalCoexistEnabled() { return experimentalCoexistEnabled; }

    // Setters
    public void setUseNativeSine(boolean v)        { useNativeSine = v; }
    public void setUseNativeCos(boolean v)         { useNativeCos = v; }
    public void setUseNativeSqrt(boolean v)        { useNativeSqrt = v; }
    public void setUseNativeInvSqrt(boolean v)     { useNativeInvSqrt = v; }
    public void setUseNativeAtan2(boolean v)       { useNativeAtan2 = v; }
    public void setUseNativeFloor(boolean v)       { useNativeFloor = v; }
    public void setUseNativeNoise(boolean v)       { useNativeNoise = v; }
    public void setEnableNativeRandom(boolean v)   { useNativeRandom = v; }
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
    public void setEnableDebugHudGraph(boolean v)        { enableDebugHudGraph = v; }
    public void setEnablePieChart(boolean v)             { enablePieChart = v; }
    public void setEnableNativeMetricsHud(boolean v)    { enableNativeMetricsHud = v; }
    public void setEnableDnsCache(boolean v)             { enableDnsCache = v; }
    public void setBridgeSodium(boolean v)         { bridgeSodium = v; }
    public void setBridgeStarlight(boolean v)      { bridgeStarlight = v; }
    public void setBridgeC2ME(boolean v)           { bridgeC2ME = v; }
    public void setBridgeIris(boolean v)           { bridgeIris = v; }
    public void setBridgeLithium(boolean v)        { bridgeLithium = v; }
    public void setDisableDhFade(boolean v)        { disableDhFade = v; }
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