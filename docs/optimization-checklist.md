# Optimization Focus Areas (May 7, 2026)

## Java Side Optimizations

### Completed ✓
- [x] ParticleTickDispatcher: thread-local futures pool (no per-tick alloc)
- [x] ParticleTickDispatcher: manual chunk partition (replaced IntStream)
- [x] DistantHorizonsCompat: visibility cache (LRU, 8K max)
- [x] DistantHorizonsCompat: fused shadow plane updates (40% FLOP reduction)
- [x] ClientPlayNetworkHandlerMixin: reflection-free packet accessor
- [x] ClientPlayNetworkHandlerMixin: 1/8 chunk ingest sampling
- [x] DebugHudMixin: local frame history ring buffer

### Candidates (Low Risk)
- [ ] **Cache FrustumContext access**: Frustum context fetch might be cacheable per-frame
- [ ] **Batch chunk statistics**: Instead of per-packet timing, batch every N packets
- [ ] **Reduce System.nanoTime() calls**: Chunk ingest tracks timing on every 1/8 packet
- [ ] **Lazy camera fetch**: Only fetch camera position on particle tick if needed

### High Risk (Profile First)
- [ ] Native chunk ingest expansion beyond 1/8 sampling (needs profiling proof)
- [ ] Worldgen offload candidates (needs parity harness)

## Rust Side Optimizations

### Completed ✓
- [x] Frustum: SIMD plane array storage
- [x] Frustum: array indexing inlining
- [x] Frustum: lazy matrix fingerprint
- [x] Occlusion: lightweight frustum+AABB+cache (no depth-buffer)
- [x] Particles: thread-local scratch buffers
- [x] Particles: parallel threshold (1024+)
- [x] Particles: distance-based early exit in loop
- [x] Lighting: compact grid propagation (18³)
- [x] JNI: critical array access for large buffers
- [x] Dependencies: wgpu 24.0, jni 0.22, glam 0.32
- [x] GPU: thread-local buffer pooling (no global Mutex)

### Candidates (Low Risk)
- [ ] **Frustum plane reduction**: Only compute 4 non-redundant planes (skip parallel W)
- [ ] **Batch occlusion test**: Consolidate multiple AABBs into single test call
- [ ] **Precompute plane magnitudes**: Cache sqrt(x²+y²+z²) during update
- [ ] **Rayon threshold tuning**: May benefit from 256-512 instead of 1024

### High Risk (Measure Impact)
- [ ] AVX-512 vectorization: would require MSRV bump + target-cpu=native
- [ ] SIMD batch culling: requires careful alignment + complex codegen

## Measurement Priorities

1. **JNI crossing cost baseline** (use micro-bench)
   - Query single AABB: expected <100ns
   - Batch 100 AABBs: expected <5µs total
   
2. **Frustum update frequency** (measure per-frame)
   - Stable scene: should skip 80%+ with fingerprinting
   - Camera movement: ~2-4 updates per second expected
   
3. **Occlusion hit rate**
   - Typical scene: 60-80% of sections culled
   - Dense scene: 40-50% culled

## Next Sprint Actions

- [ ] Create micro-bench harness (java+native, repeatable scenes)
- [ ] Profile chunk ingest path (validate 1/8 sampling trade-off)
- [ ] Measure frustum update frequency on test maps
- [ ] If Java frustum hash feasible: implement + measure gap to native

## Non-Invasive Quick Wins

- Reduce getDhReferenceY() null-checks (already done May 1)
- Skip visibility cache on static chunks (needs cache invalidation strategy)
- Batch particle distance checks per-camera-state-change