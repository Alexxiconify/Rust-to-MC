package com.alexxiconify.rustmc.util;
//
 //  Shared volatile render-pass state flags set by mixins and read by optimization hooks.
 //  Separated from mixin classes because Mixin doesn't allow non-private static fields.
 //  Single-writer (WorldRenderer render HEAD inject), multi-reader (particle/entity hooks).
 //  Volatile is sufficient for this single-writer pattern.
 //  IDE may report "never used" because it cannot trace cross-mixin references.
@SuppressWarnings({"java:S1104", "java:S1444"})
public final class RenderState {
    private RenderState() {}
    //
     // Whether the current render pass has heavy entity mods active (EMF/ETF).
     // Set by EntityRenderCompatMixin at the start of each render pass.
     // Read by ParticleManagerMixin to tighten culling when entity rendering is expensive.
    @SuppressWarnings("java:S3077")
    public static volatile boolean heavyEntityModsActive = false;
    //
     // Whether ImmediatelyFast is handling batch rendering this frame.
     // When true, we skip our own batching hints and use a more generous particle cutoff
     // since IF makes each draw call cheaper through its batching pipeline.
    @SuppressWarnings("java:S3077")
    public static volatile boolean immediatelyFastActive = false;
    //
     // Render budget tier set by `RenderBudgetMixin` at 4 Hz.
     // 0 = normal, 1 = tight, 2 = relaxed.
    @SuppressWarnings("java:S3077")
    public static volatile int renderBudgetTier = 0;
}