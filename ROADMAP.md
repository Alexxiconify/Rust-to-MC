# Rust-MC Roadmap

Plan: [`docs/completed-changes.md`](docs/completed-changes.md). Tree: [`docs/file-tree-index.md`](docs/file-tree-index.md).

## 🔥 Hotspots & Actions

**Hotspots:**
- `NativeBridge::processChunkData` — JNI + array copies
- `frustum.rs::update_from_matrix` — math + xforms
- `particles.rs` — alloc + parallel overhead
- JNI boundary — short crossings

**Due:**
1. Chunk/frustum profile (3–6h, med risk)
2. DH cull validate (2–4h, low risk)
3. IntStream.parallel → manual (1–2h, low risk)
4. CI: clippy+gradle (2h, low risk)

## ✅ Done (May 1–6)

- HUD → `DiagnosticHudRenderer`
- Config → `DiagnosticMode`
- F7=cycle, F8=sparkline
- Abs coords (VP→world at xform)
- DH fused (frustum+cave+occl 1 JNI call)
- Frustum fingerprint (skip rebuild static)
- Shader/mesher micros
- Frame telemetry (Java ring, no native)
- Reflection-free packet accessors
- Particle distance cull + presets
- wgpu 24.0, jni 0.22, glam 0.32
- GPU buffer pooling (thread-local)
- Occl: depth→frustum+AABB+cache
- DH compat: visibility (LRU 8K), fused planes
- JNI: 70–150/sec (adaptive cache)
- Network: zero-alloc chunk pooling
- **New:** Core module (math+config merged), utils. NativeStatsRenderer deleted.

**Gated:**
- Chunk ingest: 1/8 sample, `enableChunkIngestOffload`
- Native lighting: user coexist

## Priorities

1. Chunk/frustum profiling (3–6h)
2. Java frustum hash skip (highest ROI)
3. Worldgen WGPU batch candidates
4. Lighting digest batch

## Validation

- No regression
- Frame time ≥ target
- Lower CPU/JNI/alloc
- Zero new warnings
- Fallback always works

---

Last: May 6 (Batch consolidation + env<'local> fix + redundant file cleanup)