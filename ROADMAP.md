# Rust to MC Roadmap

This roadmap tracks the current plan for **Rust to MC**: move high-cost client hot-path work from Java to Rust while preserving gameplay correctness, mod compatibility, and stable frame pacing.

## Core Vision

- Keep vanilla behavior and visual correctness first, then optimize.
- Offload repeatable math/data-heavy work (frustum, particles, packet/data transforms) to Rust.
- Use JNI safely with predictable fallbacks so missing symbols never hard-crash gameplay.

## Current Baseline (April 2026)

- Primary target: **Minecraft 1.21.11** (active Gradle module `:versions:mc1_21_11`).
- Java side emphasizes compatibility gating via `MixinManager` and `ModBridge`.
- Rust side provides frustum, particle, audio, compression, and utility paths with fallback wrappers in `NativeBridge`.
- Distant Horizons culling path remains Rust-driven with fused/fallback behavior.

## Rollback Reference (v1.0.3-a3)

- Rollback target: `v1.0.3-a3` (`12e8073`)
- Rolled back from: `main` at `9cd2790`
- Commits removed in rollback: 13 (`v1.0.3-a3..9cd2790`)
- Backup branch: `backup/pre-rollback-v1.0.3-a3-20260412-210316`

### Saved Diff Artifacts

- `docs/rollback/commits_since_v1.0.3-a3.txt`
- `docs/rollback/name_status_since_v1.0.3-a3.txt`
- `docs/rollback/diff_stat_since_v1.0.3-a3.txt`
- `docs/rollback/full_diff_since_v1.0.3-a3.patch`
- `docs/rollback/working_tree_uncommitted.patch`

## Recovery and Reimplementation Track

1. Keep startup and compatibility stable (`NativeBridge`, `ModBridge`, `RustMC`, mixin gating).
2. Reintroduce DH/full rendering pipeline changes in small, testable batches.
3. Restore and simplify YACL config surface (remove defunct options, keep active toggles only).
4. Continue removing Java/Rust paths that regress performance or correctness.

## Short-Term Goals (Next 2-6 Weeks)

### 1) Frustum and DH Culling Reliability

- Keep DH section visibility decisions in Rust.
- Validate fused culling behavior against fallback path and edge camera/FOV cases.
- Add benchmark captures for section count, rejected count, and frame time impact.

### 2) JNI Hot-Path Hygiene

- Keep wrappers safe and explicit on fallback behavior.
- Minimize JNI overhead where it wins; avoid JNI where vanilla/Java is faster.
- Document per-path strategy (copy vs pinned) for maintainability.

### 3) Config and Compat Cleanup

- Keep `EntityRenderCompatMixin` as the single BBE/EMF/ETF/IF compat hook.
- Remove dead placeholder/accessor files when no longer referenced.
- Trim noisy inspections and stale suppressions without changing behavior.

## Medium-Term Goals (1-3 Months)

### 1) Native Lighting Pipeline Upgrade

- Replace placeholder/bit-packed propagation with a robust BFS-based Rust lighting propagation path.
- Validate against ScalableLux/Starlight ownership rules to avoid contention.

### 2) Native Packet and Data Workloads

- Expand decoder/packet offload where allocation pressure is measurable.
- Add profiling checkpoints before/after each offload to avoid regressions.

### 3) Native Chunk/LOD Work

- Continue DH/LOD workload offload improvements (culling and mesh/data transforms).
- Keep world-load and flight scenarios as primary performance validation workloads.

## Non-Goals / Guardrails

- Do not keep JNI math hooks that are slower than vanilla Java paths.
- Do not ship optimizations that break frustum correctness or DH LOD visibility rules.
- Do not add config surface for features that are removed/defunct.

## Backlog

- Fast JSON bridge (`serde_json`) for selected resource paths.
- ModMenu native stats surface (JNI calls, frustum checks, cache hits).
- Additional benchmark scenes for heavy particles, DH, and networking spikes.

## Recent Updates (April 2026)

- Cleaned/standardized comment style and removed stale mixin hooks that no longer remap.
- Fixed Gradle resource source-set ordering to avoid duplicate/override ambiguity.
- Simplified compat routing by replacing placeholder BBE mixin usage with `EntityRenderCompatMixin` flow.
- Updated Rust particle JNI path to avoid invalid dual mutable borrows of `JNIEnv`.

---

Last Updated: April 12, 2026