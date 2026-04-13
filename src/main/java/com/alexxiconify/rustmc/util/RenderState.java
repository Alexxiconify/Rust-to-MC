package com.alexxiconify.rustmc.util;

// Shared volatile render-pass state flags set by mixins and read by optimization hooks. Multi-reader (particle/entity hooks). Volatile is sufficient for this single-writer pattern.
@SuppressWarnings({"java:S1104", "java:S1444"})
public final class RenderState {

    private RenderState() {}
    // Whether the current render pass has heavy entity mods active (EMF/ETF). Used to tighten culling.
    public static volatile boolean heavyEntityModsActive = false;
    // Whether ImmediatelyFast is handling batch rendering this frame. Skip own hints if true.
    public static volatile boolean immediatelyFastActive = false;
    // When true, FPS is below 60 — tighten all culling distances aggressively.
    public static volatile boolean renderBudgetTight = false;
    // When true, FPS is above 90 — relax culling for better visual quality.
    public static volatile boolean renderBudgetRelaxed = false;
}