package com.alexxiconify.rustmc.config;

public class RustMCConfig {
    // Math optimizations
    private boolean useNativeSine     = true;
    private boolean useNativeCos      = true;
    private boolean useNativeSqrt     = true;
    private boolean useNativeInvSqrt  = true;
    private boolean useNativeTan      = true;
    private boolean useNativeAtan2    = true;
    private boolean useNativeFloor    = true;
    private boolean useNativeNoise    = true;

    // World / system features
    private boolean useNativeLighting    = true;
    private boolean useNativeCompression = true;
    private boolean useNativePathfinding = true;
    private boolean useNativeCulling     = true;
    private boolean useFastLoadingScreen = true;
    private boolean useNativeCommands    = false; // experimental – off by default
    private boolean limitXaeroMinimap    = true;

    // Mod bridges
    private boolean bridgeSodium    = true;
    private boolean bridgeStarlight = true;
    private boolean bridgeC2ME      = true;
    private boolean bridgeIris      = true;
    private boolean bridgeLithium   = true;
    private boolean disableDhFade  = true;

    // Developer
    private boolean silenceLogs = true;
    private boolean nativeReady = false;

    public void copyFrom(RustMCConfig other) {
        this.useNativeSine       = other.useNativeSine;
        this.useNativeCos        = other.useNativeCos;
        this.useNativeSqrt       = other.useNativeSqrt;
        this.useNativeInvSqrt    = other.useNativeInvSqrt;
        this.useNativeTan        = other.useNativeTan;
        this.useNativeAtan2      = other.useNativeAtan2;
        this.useNativeFloor      = other.useNativeFloor;
        this.useNativeNoise      = other.useNativeNoise;
        this.useNativeLighting   = other.useNativeLighting;
        this.useNativeCompression = other.useNativeCompression;
        this.useNativePathfinding = other.useNativePathfinding;
        this.useNativeCulling    = other.useNativeCulling;
        this.useFastLoadingScreen = other.useFastLoadingScreen;
        this.useNativeCommands   = other.useNativeCommands;
        this.limitXaeroMinimap   = other.limitXaeroMinimap;
        this.bridgeSodium        = other.bridgeSodium;
        this.bridgeStarlight     = other.bridgeStarlight;
        this.bridgeC2ME          = other.bridgeC2ME;
        this.bridgeIris          = other.bridgeIris;
        this.bridgeLithium       = other.bridgeLithium;
        this.disableDhFade       = other.disableDhFade;
        this.silenceLogs         = other.silenceLogs;
        this.nativeReady         = other.nativeReady;
    }

    // Getters
    public boolean isUseNativeSine()       { return useNativeSine; }
    public boolean isUseNativeCos()        { return useNativeCos; }
    public boolean isUseNativeSqrt()       { return useNativeSqrt; }
    public boolean isUseNativeInvSqrt()    { return useNativeInvSqrt; }
    public boolean isUseNativeTan()        { return useNativeTan; }
    public boolean isUseNativeAtan2()      { return useNativeAtan2; }
    public boolean isUseNativeFloor()      { return useNativeFloor; }
    public boolean isUseNativeNoise()      { return useNativeNoise; }
    public boolean isUseNativeLighting()   { return useNativeLighting; }
    public boolean isUseNativeCompression(){ return useNativeCompression; }
    public boolean isUseNativePathfinding(){ return useNativePathfinding; }
    public boolean isUseNativeCulling()    { return useNativeCulling; }
    public boolean isUseFastLoadingScreen(){ return useFastLoadingScreen; }
    public boolean isUseNativeCommands()   { return useNativeCommands; }
    public boolean isLimitXaeroMinimap()   { return limitXaeroMinimap; }
    public boolean isBridgeSodium()        { return bridgeSodium; }
    public boolean isBridgeStarlight()     { return bridgeStarlight; }
    public boolean isBridgeC2ME()          { return bridgeC2ME; }
    public boolean isBridgeIris()          { return bridgeIris; }
    public boolean isBridgeLithium()       { return bridgeLithium; }
    public boolean isDisableDhFade()       { return disableDhFade; }
    public boolean isSilenceLogs()         { return silenceLogs; }
    public boolean isNativeReady()         { return nativeReady; }

    // Setters
    public void setUseNativeSine(boolean val)        { this.useNativeSine = val; }
    public void setUseNativeCos(boolean val)         { this.useNativeCos = val; }
    public void setUseNativeSqrt(boolean val)        { this.useNativeSqrt = val; }
    public void setUseNativeInvSqrt(boolean val)     { this.useNativeInvSqrt = val; }
    public void setUseNativeTan(boolean val)         { this.useNativeTan = val; }
    public void setUseNativeAtan2(boolean val)       { this.useNativeAtan2 = val; }
    public void setUseNativeFloor(boolean val)       { this.useNativeFloor = val; }
    public void setUseNativeNoise(boolean val)       { this.useNativeNoise = val; }
    public void setUseNativeLighting(boolean val)    { this.useNativeLighting = val; }
    public void setUseNativeCompression(boolean val) { this.useNativeCompression = val; }
    public void setUseNativePathfinding(boolean val) { this.useNativePathfinding = val; }
    public void setUseNativeCulling(boolean val)     { this.useNativeCulling = val; }
    public void setUseFastLoadingScreen(boolean val) { this.useFastLoadingScreen = val; }
    public void setUseNativeCommands(boolean val)    { this.useNativeCommands = val; }
    public void setLimitXaeroMinimap(boolean val)    { this.limitXaeroMinimap = val; }
    public void setBridgeSodium(boolean val)         { this.bridgeSodium = val; }
    public void setBridgeStarlight(boolean val)      { this.bridgeStarlight = val; }
    public void setBridgeC2ME(boolean val)           { this.bridgeC2ME = val; }
    public void setBridgeIris(boolean val)           { this.bridgeIris = val; }
    public void setBridgeLithium(boolean val)        { this.bridgeLithium = val; }
    public void setDisableDhFade(boolean val)        { this.disableDhFade = val; }
    public void setSilenceLogs(boolean val)          { this.silenceLogs = val; }
    public void setNativeReady(boolean val)          { this.nativeReady = val; }
}
