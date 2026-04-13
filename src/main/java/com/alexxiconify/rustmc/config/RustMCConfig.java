package com.alexxiconify.rustmc.config;

// Configuration POJO for Rust-MC. All fields are serialized/deserialized by Gson, and getters/setters are referenced by ModMenu (YACL) via method references.
public class RustMCConfig {
    // Math optimizations
    @com.google.gson.annotations.Expose private boolean useNativeSine     = true;
    @com.google.gson.annotations.Expose private boolean useNativeCos      = true;
    @com.google.gson.annotations.Expose private boolean useNativeSqrt     = true;
    @com.google.gson.annotations.Expose private boolean useNativeInvSqrt  = true;
    @com.google.gson.annotations.Expose private boolean useNativeAtan2    = true;
    @com.google.gson.annotations.Expose private boolean useNativeFloor    = true;
    @com.google.gson.annotations.Expose private boolean useNativeNoise    = true;
    @com.google.gson.annotations.Expose private boolean useNativeF3       = true;
    @com.google.gson.annotations.Expose private boolean useNativeRandom   = true;

    // World / system features
    @com.google.gson.annotations.Expose private boolean useNativeLighting    = true;
    @com.google.gson.annotations.Expose private boolean useNativeCompression = true;
    @com.google.gson.annotations.Expose private boolean useFastLoadingScreen = false;
    @com.google.gson.annotations.Expose private boolean useNativeCommands    = false;

    // Mod compat toggles
    @com.google.gson.annotations.Expose private boolean enableParticleCulling      = true;
    @com.google.gson.annotations.Expose private boolean enableChunkBuilderExpand   = true;
    @com.google.gson.annotations.Expose private boolean enableTickSyncCompat       = true;
    @com.google.gson.annotations.Expose private boolean enableBBECompat            = true;
    @com.google.gson.annotations.Expose private boolean enableEMFCompat            = true;
    @com.google.gson.annotations.Expose private boolean enableETFCompat            = true;
    @com.google.gson.annotations.Expose private boolean enableAppleSkinCompat      = true;
    @com.google.gson.annotations.Expose private boolean enableEntityCullingCompat  = true;
    @com.google.gson.annotations.Expose private boolean enableImmediatelyFastCompat = true;
    @com.google.gson.annotations.Expose private boolean enableClientRedstoneSkip   = true;
    @com.google.gson.annotations.Expose private boolean enableDebugHudGraph        = false;
    @com.google.gson.annotations.Expose private boolean enablePieChart             = false;
    @com.google.gson.annotations.Expose private boolean enableNativeMetricsHud    = false;

    // DNS / Server List
    @com.google.gson.annotations.Expose private boolean enableDnsCache             = true;

    // Mod bridges
    @com.google.gson.annotations.Expose private boolean bridgeSodium    = true;
    @com.google.gson.annotations.Expose private boolean bridgeStarlight = true;
    @com.google.gson.annotations.Expose private boolean bridgeC2ME      = true;
    @com.google.gson.annotations.Expose private boolean bridgeIris      = true;
    @com.google.gson.annotations.Expose private boolean bridgeLithium   = true;
    @com.google.gson.annotations.Expose private boolean disableDhFade   = true;

    // Loading screen colors
    @com.google.gson.annotations.Expose private int loadingBarBgColor      = 0xFF1A1A1A;
    @com.google.gson.annotations.Expose private int loadingBarLowColor     = 0xFF22AA44;
    @com.google.gson.annotations.Expose private int loadingBarMidColor     = 0xFFCCAA00;
    @com.google.gson.annotations.Expose private int loadingBarHighColor    = 0xFFCC2222;
    @com.google.gson.annotations.Expose private int loadingBarTextColor    = 0xDDFFFFFF;
    @com.google.gson.annotations.Expose private int loadingBarSubtextColor = 0x9900FFFF;

    // Developer
    @com.google.gson.annotations.Expose private boolean silenceLogs = true;
    @com.google.gson.annotations.Expose(serialize = false, deserialize = false)
    private boolean nativeReady = false;
    @com.google.gson.annotations.Expose private boolean experimentalCoexistEnabled = true;
    @com.google.gson.annotations.Expose private boolean lockCullingToPlayer = false;

    // --- Runtime Mod Detection State (not saved to disk) ---
    @com.google.gson.annotations.Expose(serialize = false, deserialize = false) private boolean modSodiumLoaded = false;
    @com.google.gson.annotations.Expose(serialize = false, deserialize = false) private boolean modLithiumLoaded = false;
    @com.google.gson.annotations.Expose(serialize = false, deserialize = false) private boolean modIrisLoaded = false;
    @com.google.gson.annotations.Expose(serialize = false, deserialize = false) private boolean modStarlightLoaded = false;
    @com.google.gson.annotations.Expose(serialize = false, deserialize = false) private boolean modC2MELoaded = false;
    @com.google.gson.annotations.Expose(serialize = false, deserialize = false) private boolean modDistantHorizonsLoaded = false;
    @com.google.gson.annotations.Expose(serialize = false, deserialize = false) private boolean modImmediatelyFastLoaded = false;
    @com.google.gson.annotations.Expose(serialize = false, deserialize = false) private boolean modEntityCullingLoaded = false;

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
        this.lockCullingToPlayer = o.lockCullingToPlayer;
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
    public boolean isLockCullingToPlayer()        { return lockCullingToPlayer; }

    public boolean isModSodiumLoaded() { return modSodiumLoaded; }
    public boolean isModLithiumLoaded() { return modLithiumLoaded; }
    public boolean isModIrisLoaded() { return modIrisLoaded; }
    public boolean isModStarlightLoaded() { return modStarlightLoaded; }
    public boolean isModC2MELoaded() { return modC2MELoaded; }
    public boolean isModDistantHorizonsLoaded() { return modDistantHorizonsLoaded; }
    public boolean isModImmediatelyFastLoaded() { return modImmediatelyFastLoaded; }
    public boolean isModEntityCullingLoaded() { return modEntityCullingLoaded; }

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
    public void setLockCullingToPlayer(boolean v)        { lockCullingToPlayer = v; }

    public void setModSodiumLoaded(boolean v) { modSodiumLoaded = v; }
    public void setModLithiumLoaded(boolean v) { modLithiumLoaded = v; }
    public void setModIrisLoaded(boolean v) { modIrisLoaded = v; }
    public void setModStarlightLoaded(boolean v) { modStarlightLoaded = v; }
    public void setModC2MELoaded(boolean v) { modC2MELoaded = v; }
    public void setModDistantHorizonsLoaded(boolean v) { modDistantHorizonsLoaded = v; }
    public void setModImmediatelyFastLoaded(boolean v) { modImmediatelyFastLoaded = v; }
    public void setModEntityCullingLoaded(boolean v) { modEntityCullingLoaded = v; }
}