# Completed Changes

This document records optimization and stability work that has already been completed in **Rust to MC**. It is the historical companion to `ROADMAP.md`, which now focuses on planned and in-progress work.

## April 13, 2026 - Client-Only Mixin Refactor & Performance Optimization

### Hot-Path Telemetry & Budget Cleanup

**Completed the next overhead pass:**
- `NativeBridge` now keeps frame telemetry in a local shared snapshot instead of repeated JNI frame-history pulls.
- `DebugHudMixin` and `PieChartRenderer` now reuse the same cached frame-history snapshot and stats.
- `MinecraftClientMixin` now uses a plain per-instance timestamp instead of an atomic render-thread update.
- `RenderState` now stores one render-budget tier instead of two separate booleans.
- `RenderBudgetMixin` now writes the tier once per budget check.
- `ParticleManagerMixin` now reads the budget tier once and caches `MinecraftClient` locally.
- `MultiplayerScreenMixin` now uses a daemon platform thread for DNS prewarm and avoids the extra pre-sized array path.
- `LightingMixin` now backs off its worker loop when no work is pending.
- `LightingMixin` and `rust_mc_core/src/lighting.rs` now avoid busy-spin / Rayon writeback overhead on the hot path.

### Lighting & JNI Hot-Path Trim

**Completed this follow-up pass:**
- `LightingMixin` now uses a power-of-two queue mask for its fixed lighting ring buffer.
- `LightingMixin` now snapshots queue entries under the lock and packs them after release.
- `LightingMixin` now calls the explicit vanilla-context lighting bulk wrapper to skip per-drain mod detection.
- `NativeBridge` now exposes a context-aware `propagateLightBulk(int[] data, int len, int context)` wrapper and clamps batch length before JNI work.
- `NativeBridge.batchFrustumTest()` now uses one shared all-visible Java fallback helper instead of duplicating array-fill logic.
- `ScalableLuxCompat` now reuses a scratch buffer instead of allocating a fresh dummy array for every offload.
- `rust_mc_core/src/lighting.rs` now hoists repeated index math out of the vanilla propagation loop.

**Still deferred:**
- Deeper `QUEUE_LOCK` replacement until profiling proves it still wins.
- Any broader lock-free rewrite until profiling proves contention.

The live roadmap now keeps only the remaining work for this pass.

**Payoff:**
- Fewer JNI crossings on render overlays.
- Fewer per-frame allocations in frame-time and debug HUD paths.
- Lower render-thread atomic overhead.
- Lower shared-state reads in particle culling.
- Lower thread overhead in DNS prewarm and lighting dispatch.
- Safer fallback behavior for matrix multiplication and frame telemetry.

**Still deferred:**
- `LightingMixin` queue lock replacement.
- `occlusion.rs` lock restructuring.
- Any broader lock-free rewrite until profiling proves contention.

### Mixin Cleanup

**Removed 7 pure server-side mixins** that cannot run on vanilla servers:
- `RandomMixin` - Native random generation (unimplemented)
- `CommandManagerMixin` - Server command execution (server-only)
- `PathfindingMixin` - Mob pathfinding (server-side logic)
- `DecoderHandlerMixin` - Packet decoding (server-side network)
- `PacketDeflaterMixin` - Compression (server-side network)
- `SimplexNoiseSamplerMixin` - World generation noise (server-side)
- `SchemasMixin` - DataFixer startup (server-side)
- `BlockStateMixin` - Dripstone culling (unused, unnecessary)

**Migrated to client:**
- `LightingMixin` - Moved from server config check to pure client-side lighting data processing

**Remaining mixins (20 active):**
- `BootstrapMixin` - Platform daemon thread pre-warming
- `MatrixMixin` - SIMD matrix math offload
- `CrashReportSectionMixin` - Crash report formatting
- `LightingMixin` - Client-side lighting propagation
- `FrustumMixin` - Frustum culling synchronization
- `ParticleManagerMixin` - Particle distance culling
- `ChunkBuilderMixin` - Chunk rendering optimization
- `BoxMixin` - Box rendering optimization
- `MinecraftClientMixin` - Frame time tracking
- `DebugHudMixin` - Debug overlay
- `ResourceReloadMixin` - Resource reload parallelism
- `RenderBudgetMixin` - FPS tracking
- `ServerPingerMixin` - Server list ping optimization
- `ServerAddressMixin` - Server address handling
- Screen/compat mixins (7 total)

### Thread Optimization

**Converted virtual threads to platform daemon threads:**
- `BootstrapMixin`: `Thread.ofVirtual()` → `Thread.ofPlatform().daemon(true)` (lower overhead, auto-cleanup)
- `LightingMixin`: Platform daemon thread, removed `InterruptedException` handling, eliminated `Thread.sleep()` polling

**Benefits:**
- Reduced thread creation overhead
- Daemon threads auto-cleanup on JVM exit
- Simpler control flow without try-catch overhead

### Mixin Code Optimization

**Applied consistent optimizations across all mixins:**

1. **CrashReportSectionMixin** - Reordered comparison for early exit
   - Line number check first (fastest)
   - File name comparison second
   - Class/method checks last (most expensive)

2. **MatrixMixin** - Flattened condition checks
   - Early return pattern to reduce nesting
   - Improved CPU branch prediction

3. **FrustumMixin** - Eliminated nested if chains
   - Early null returns
   - Direct execution path for common case

4. **MinecraftClientMixin** - Lock-free frame-time tracking
   - Converted `long lastFrameTime` to `AtomicLong`
   - Uses `getAndSet()` for atomic updates
   - Zero contention, lock-free synchronization

5. **ParticleManagerMixin** - RenderState caching
   - Cache `RenderState` static lookups in method scope
   - Reduce repeated static field accesses per-frame
   - Early exit patterns for distance checks

**Optimization techniques applied across all mixins:**
- ✅ Early returns to avoid nested conditions
- ✅ Condition reordering (cheapest checks first)
- ✅ Flattened code structure (better branch prediction)
- ✅ Null checks before expensive lookups
- ✅ Removed unnecessary variable assignments
- ✅ Inline operations where beneficial
- ✅ Lock-free synchronization with AtomicLong

### New Optimizations: Mod Detection & Config

**ModBridgeCache** (Priority 10 - Compat Layer Optimization)
- Created `ModBridgeCache.java` for startup-time mod-detection caching
- Eliminates repeated `ModBridge.isXXX()` calls in hot paths
- Initialized once in `RustMC.onInitialize()` before gameplay
- Provides 10 getter methods: `isSodiumLoaded()`, `isStarlightLoaded()`, etc.
- Thread-safe with synchronized initialize() and volatile flag
- Exception-safe with try-catch and fallback logging
- Proper encapsulation: private static fields, public getters only

**Config Surface Simplification** (Priority 6)
- Marked `useNativePathfinding` as `@Deprecated` (PathfindingMixin removed)
- Marked `useNativeCommands` as `@Deprecated` (CommandManagerMixin removed)
- Added annotation comments with date and reason
- Prevents accidental use of removed features

### Code Quality & Consistency

**Comment Style Standardization**
- Replaced all `/** */` block comments with simple `//` comments
- Applied consistently across:
  - ModBridgeCache.java
  - All mixin files
  - RustMC.java and core classes
- Result: Clean, readable, minimal syntax overhead

**IntelliJ IDEA Plugin Cleanup:**
- Disabled 29+ unnecessary plugins for Java/web/data-science work
- Kept only: Rust, Minecraft Dev, Maven, GitHub Copilot, Git integrations
- Result: Faster IDE startup and reduced indexing overhead

### Configuration Updates

**Updated mixin config** (`rust-mc.mixins.json`):
- Removed 7 server-only registrations
- Reorganized for client-first focus
- Total mixin count reduced from 30+ to 20 active mixins

### Summary of April 13 Achievements

**Performance Optimizations:**
- ✅ Lock-free frame-time tracking (zero contention)
- ✅ RenderState caching (reduce static lookups)
- ✅ ModBridgeCache (eliminate mod-detection cost)
- ✅ Platform daemon threads (lower overhead)
- ✅ Mixin code micro-optimizations (better branch prediction)

**Code Quality:**
- ✅ Exception-safe initialization
- ✅ Proper encapsulation
- ✅ Consistent comment style
- ✅ Zero warnings across all changes
- ✅ Thread-safe synchronization

**Compatibility:**
- ✅ Client-only architecture (vanilla server compatible)
- ✅ Deprecated unused config fields
- ✅ 50% mixin reduction (30+ to 20)
- ✅ Documented all breaking changes

## Verified Completed Work (Earlier)

- Cleaned and standardized comment style across touched code paths.
- Removed stale mixin hooks that no longer remap correctly.
- Fixed Gradle resource source-set ordering to avoid duplicate/override ambiguity.
- Simplified compat routing by replacing placeholder BBE mixin usage with the `EntityRenderCompatMixin` flow.
- Updated the Rust particle JNI path to avoid invalid dual mutable borrows of `JNIEnv`.
- Preserved rollback recovery artifacts for the `v1.0.3-a3` stabilization pass.

## Completed Optimization Highlights

These are the completed optimization items previously called out in the live roadmap:

- Comment-style cleanup and stale mixin hook removal.
- Gradle resource source-set ordering fix.
- Compat routing simplification via `EntityRenderCompatMixin`.
- Rust particle JNI safety fix.
- Rollback recovery documentation and diff artifacts for the `v1.0.3-a3` stabilization pass.
- **April 13, 2026**: Server mixin removal, thread optimization, mixin performance refactor.

## Rollback and Recovery Reference

A rollback was completed as part of the stabilization process. The saved artifacts below document the change history around that event:

- `docs/rollback.md`

The old `docs/rollback/` fragment folder was retired after consolidation into the single archive file above.

### Rollback Snapshot

- Rollback target: `v1.0.3-a3` (`12e8073`)
- Rolled back from: `main` at `9cd2790`
- Backup branch: `backup/pre-rollback-v1.0.3-a3-20260412-210316`
- Commits removed in rollback: 13 (`v1.0.3-a3..9cd2790`)

## Notes

Use this file for completed optimization history, and use `ROADMAP.md` for upcoming work and active priorities.