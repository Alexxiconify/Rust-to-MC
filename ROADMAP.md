# Rust-MC Roadmap

Plan: [`docs/completed-changes.md`](docs/completed-changes.md). Tree: [`docs/file-tree-index.md`](docs/file-tree-index.md).

## 🔥 Hotspots & Actions

**Hotspots:**
- `NativeBridge::processChunkData` — JNI + array copies
- DH cull simplified — frustum + below-Y gate, no occlusion
- `particles.rs` — alloc + parallel overhead
- JNI boundary — short crossings

**Due:**
1. Chunk/frustum profile (3–6h, med risk)
2. DH cull profile for frustum/Y only (2–4h, low risk)
3. IntStream.parallel → manual (1–2h, low risk)
4. CI: clippy+gradle (2h, low risk)

## ✅ Done (May 1–6)

- HUD → `DiagnosticHudRenderer`
- Config → `DiagnosticMode`
- F7=cycle, F8=sparkline
- Abs coords (VP→world at xform)
- DH fused removed; frustum/Y only
- Frustum fingerprint (skip rebuild static)
- Shader/mesher micros
- Frame telemetry (Java ring, no native)
- Reflection-free packet accessors
- Particle distance cull + presets
- DH frustum path kept; occlusion banned; see `docs/dh-culling-stub-plan.md`
- wgpu 24.0, jni 0.22, glam 0.32
- GPU buffer pooling (thread-local)
- Occl: removed from DH runtime path
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

Last: May 12 (DH cull simplified to frustum + below-Y gate.)

## ✅ Changes (May 12)

- Java-side async LOD integration: added non-blocking example to offload GPU LOD mesh generation from render-like threads. Files: `src/main/java/com/alexxiconify/rustmc/NativeBridge.java` (new executor + async wrapper) and `src/main/java/com/alexxiconify/rustmc/compat/DistantHorizonsCompat.java` (conservative non-blocking path + async API).
- JNI pinned-in-place additions: prefer direct/pinned buffer variants where available for map texture and audio processing to minimize array copies; fallbacks preserved. See `NativeBridge.processMapTexture(IntBuffer)` and `NativeBridge.processAudio(ByteBuffer)`.
- Zero-copy GPU buffer handle API: added Create/Release/Map/Unmap primitives and Java wrappers (no-op when native not linked). API exposed in `NativeBridge` (createGpuBuffer/releaseGpuBuffer/mapGpuBufferPointer/unmapGpuBuffer).

Notes:
- All native additions are optional: wrappers detect missing native symbols and fall back to safe Java paths.
- This pass intentionally avoids changing public mod behavior; DH compatibility uses conservative fallback (returns empty mesh on render-thread call) — can be extended later to cache and inject results back into DH.