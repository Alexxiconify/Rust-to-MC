# Completed Changes

This document records optimization and stability work that has already been completed in **Rust to MC**. It is the historical companion to `ROADMAP.md`, which now focuses on planned and in-progress work.

## April 13, 2026 - Client-Only Mixin Refactor & Performance Optimization

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

**Remaining mixins (10 active):**
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

4. **MinecraftClientMixin** - Reordered condition checks
   - Null check before `isReady()` lookup
   - Cheaper checks first

5. **ParticleManagerMixin** - Inline null-safe operations
   - Cleaner structure for distance culling path

**Optimization techniques applied across all mixins:**
- ✅ Early returns to avoid nested conditions
- ✅ Condition reordering (cheapest checks first)
- ✅ Flattened code structure (better branch prediction)
- ✅ Null checks before expensive lookups
- ✅ Removed unnecessary variable assignments
- ✅ Inline operations where beneficial

### Configuration & Setup

**IntelliJ IDEA Plugin Cleanup:**
- Disabled 29+ unnecessary plugins for Java/web/data-science work
- Kept only: Rust, Minecraft Dev, Maven, GitHub Copilot, Git integrations
- Result: Faster IDE startup and reduced indexing overhead

### Configuration Updates

**Updated mixin config** (`rust-mc.mixins.json`):
- Removed 7 server-only registrations
- Reorganized for client-first focus
- Total mixin count reduced from 30+ to 20 active mixins

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

- `docs/rollback/commits_since_v1.0.3-a3.txt`
- `docs/rollback/name_status_since_v1.0.3-a3.txt`
- `docs/rollback/diff_stat_since_v1.0.3-a3.txt`
- `docs/rollback/full_diff_since_v1.0.3-a3.patch`
- `docs/rollback/working_tree_uncommitted.patch`

### Rollback Snapshot

- Rollback target: `v1.0.3-a3` (`12e8073`)
- Rolled back from: `main` at `9cd2790`
- Backup branch: `backup/pre-rollback-v1.0.3-a3-20260412-210316`
- Commits removed in rollback: 13 (`v1.0.3-a3..9cd2790`)

## Notes

Use this file for completed optimization history, and use `ROADMAP.md` for upcoming work and active priorities.