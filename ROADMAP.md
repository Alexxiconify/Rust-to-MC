# Rust to MC Roadmap

This roadmap tracks the active plan for **Rust to MC**: move high-cost client hot-path work from Java to Rust while preserving gameplay correctness, mod compatibility, and stable frame pacing.

Scope: active work only. Completed optimization history lives in [`docs/completed-changes.md`](docs/completed-changes.md).

## Core Vision

- Keep vanilla behavior and visual correctness first, then optimize.
- Offload repeatable math/data-heavy work (frustum, particles, packet/data transforms) to Rust.
- Use JNI safely with predictable fallbacks so missing symbols never hard-crash gameplay.

## Current Baseline (April 2026)

- Primary target: **Minecraft 1.21.11** (active Gradle module `:versions:mc1_21_11`).
- Java side emphasizes compatibility gating via `MixinManager` and `ModBridge`.
- Rust side provides frustum, particle, audio, compression, and utility paths with fallback wrappers in `NativeBridge`.
- Distant Horizons culling path remains Rust-driven with fused/fallback behavior.

## Completed Changes

Completed optimization and stabilization work is documented in [`docs/completed-changes.md`](docs/completed-changes.md).

## Active Optimization Priorities

### 1) Frustum and DH Culling Reliability

Goal: keep culling correct, fast, and debuggable.

- Keep DH section visibility decisions in Rust.
- Validate fused culling behavior against the fallback path and edge camera/FOV cases.
- Add a debug toggle for culling visibility so testing can confirm what is being rejected.
- Capture section count, rejected count, and frame-time impact for each change.

### 2) JNI Hot-Path Hygiene

Goal: keep native offload only where it clearly wins.

- Keep wrappers safe and explicit on fallback behavior.
- Minimize JNI overhead where it helps; avoid JNI where vanilla Java is faster.
- Document per-path strategy (copy vs pinned) for maintainability.
- Profile before/after every JNI change so regressions are easy to catch.

### 3) Config and Compat Cleanup

Goal: simplify the runtime surface without losing functionality.

- Keep `EntityRenderCompatMixin` as the single BBE/EMF/ETF/IF compat hook.
- Remove dead placeholder/accessor files when no longer referenced.
- Trim noisy inspections and stale suppressions without changing behavior.
- Prefer one clear toggle per optimization instead of overlapping config paths.

### 4) Native Lighting, Packet, and Chunk Workloads

Goal: expand native offload only where profiling shows real payoff.

- Replace placeholder/bit-packed lighting propagation with a robust BFS-based Rust path.
- Validate against ScalableLux/Starlight ownership rules to avoid contention.
- Expand decoder/packet offload where allocation pressure is measurable.
- Continue DH/LOD mesh and data transform improvements only when world-load and flight scenarios stay stable.

## Validation Gates

Every optimization should satisfy at least one of these before it is considered ready:

- No visible correctness regression in frustum or LOD culling.
- Lower or unchanged frame time in the target scene.
- Lower CPU cost or allocation pressure on the measured hot path.
- No new crash, fallback breakage, or mod-compat regression.
- Clear benchmark or debug evidence that the change is doing useful work.

## Non-Goals / Guardrails

- Do not keep JNI math hooks that are slower than vanilla Java paths.
- Do not ship optimizations that break frustum correctness or DH LOD visibility rules.
- Do not add config surface for features that are removed/defunct.

## Backlog

- Fast JSON bridge (`serde_json`) for selected resource paths, only if profiling shows repeated data-transform cost.
- ModMenu native stats surface (JNI calls, frustum checks, cache hits) if it helps diagnose culling or hot-path regressions.
- Additional benchmark scenes for heavy particles, DH, and networking spikes.

---

Last Updated: April 12, 2026