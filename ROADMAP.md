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

## ✅ Micro-Optimizations (May 12 – continued)

### A) Allocation Pooling & Reuse
- **ThreadLocal block array pool** (`LOD_BLOCK_POOL`): avoids repeated `Arrays.copyOf` in `generateLodMeshGpuAsync`; reuses pool with grow-on-demand for blocks snapshot.
- **ThreadLocal direct ByteBuffer pool** (`LOD_DIRECT_BUFFER`): reuses native buffers for `rustGenerateLodMeshGpuDirect` calls; grows on first use, reused thereafter.
- **Impact**: Reduces JNI->Rust array allocation churn and GC pressure when LOD meshing at high frequency.

### B) JNI Wrapper Conversions & Pinning
- Extracted nested try-catch into `tryGenerateLodMeshGpuDirect()` helper to simplify async flow and isolate direct-call logic.
- Added bounds checks & safe length handling to `propagateLightBulk` and `propagateLightDH` (input validation now inline).
- Prepared support flags (`supportsPropagateLight`, etc.) for future optional pinned variants.
- **Impact**: Cleaner code, reduced JNI nesting overhead, safer array bounds.

### C) Async LOD Result Caching & Integration
- **LRU mesh cache** (`LOD_MESH_CACHE`, 256 entry limit, LRU eviction): stores completed async LOD meshes keyed by `(chunkX, chunkZ, detail)`.
- **DistantHorizonsCompat integration**: `generateGpuLod` now checks cache first; if mesh previously generated async, returns it immediately (one-shot retrieval).
- **Callback-driven**: `generateLodMeshGpuAsync` caches mesh upon completion; subsequent DH calls on render thread will still return empty, but future calls will find cache entry.
- **Impact**: Async-generated meshes are not lost; DH will eventually reuse them when requesting same chunk again.

### D) Reflection Caching & Executor Pooling
- **Cached reflection lookups**: `getDirectBufferAddress` now caches Field and Method lookups (single initialization via synchronized block).
- **DistantHorizonsCompat reflection caching**: `tryInvokeNoArgFloatArray` and `tryInvokeMatrixGet` cache method references; `readField` caches Field per (type, name) pair using `ConcurrentHashMap`.
- **Executor improvements**:
  - Switched `LOD_EXECUTOR` from `FixedThreadPool` to `ForkJoinPool` for work-stealing and better load balancing in mesh generation.
  - Added `BACKGROUND_EXECUTOR` (fixed pool) for general offload tasks (DNS batch resolve, etc.).
  - DNS batch resolver now uses `BACKGROUND_EXECUTOR` instead of `CompletableFuture.runAsync()` (default commonPool); better control over thread count.
- **Impact**: Reduced reflection overhead in hot paths; better executor utilization and thread control; lower latency in DNS and LOD generation.

### Summary of Changes
| Aspect      | Optimization                                    | Impact                                                  |
|-------------|-------------------------------------------------|---------------------------------------------------------|
| Allocations | ThreadLocal pools (blocks, direct buffers)      | Fewer native alloc/dealloc cycles; lower GC             |
| JNI Calls   | Simplified nesting, added validation            | Cleaner code, safer array handling                      |
| Async       | Result caching (LRU, 256 entries)               | Meshes not lost; future requests find cached results    |
| Reflection  | Cached Field/Method lookups (global + per-type) | Avoids repeated reflection in hot matrix/field access   |
| Executors   | ForkJoinPool (LOD), shared pool (background)    | Work-stealing, better thread control, lower DNS latency |

**Files Modified:**
- `src/main/java/com/alexxiconify/rustmc/NativeBridge.java` — pools, helpers, caching, executor
- `src/main/java/com/alexxiconify/rustmc/compat/DistantHorizonsCompat.java` — reflection caching, async cache lookup

**Build Status:** ✅ `gradle compileJava` successful. No compile errors; all changes backward-compatible.