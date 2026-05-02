# Performance Findings & Potential Slowdowns

This document summarizes likely sources of overhead in the codebase (native Rust `rust_mc_core`, Java wrapper, Mixins, and config) and points to files where those costs originate. Use this as a checklist to guide profiling and remediation.

High-confidence hotspots

- Chunk ingest (JNI offload)
  - Why: large byte buffers cross JNI frequently; native decoder must parse chunk payloads quickly. Misuse of array copies or repeated JNI lookups increases overhead.
  - Key code: `src/main/java/com/alexxiconify/rustmc/NativeBridge.java::processChunkData` and `rust_mc_core/src/*` native decoder.
  - Symptoms: high CPU on chunk load, increased GC activity if Java copies buffers, occasional fallback to Java path when UnsatisfiedLinkError observed.
  - Short-term mitigations: enable `enableChunkIngestValidation` to collect timings, review use of critical array APIs in Rust, increase sampling and validate avg micros via `getChunkIngestStats()`.

- Frustum & DH fused culling
  - Why: frequent small JNI crossings per AABB or entity; per-frame updates and many tiny calls amplify call overhead. Coordinate system conversions can also add CPU work.
  - Key code: `NativeBridge.updateVanillaFrustum`, `rustFrustum*` natives, `cullDistantHorizonsSection`, `rustDHCullFused` and `DistantHorizonsCompat` mixins.
  - Symptoms: spikes during view movement, high per-frame CPU, regressions when fused native path slower than Java fallback.
  - Short-term mitigations: use batch/native fused calls (already present), enable `DiagnosticMode.TIMING` to gather `frustumCalls`/`frustumTotalNs` and compare to Java timings.

- Particle tick path
  - Why: large numbers of particles cause heavy per-particle math; Java allocation and stream APIs can add overhead. Native path may be slower for small batches due to JNI cost.
  - Key code: `com.alexxiconify.rustmc.util.ParticleTickDispatcher` and `NativeBridge.tickParticlesNative` / `rustTickParticles`.
  - Symptoms: CPU spikes when many particles, thread overhead when switching to Java fallback.
  - Short-term mitigations: tune `PARALLEL_THRESHOLD`, reuse thread pools or preallocate task arrays, collect `particleCalls`/`particleTotalNs` via diagnostic mode.

- JNI boundary patterns and array access
  - Why: Non-critical use of GetByteArrayElements or repeated Get/Release calls (or copying arrays) adds latency and GC pressure.
  - Key code: native bridge signatures in `NativeBridge.java` and corresponding Rust `jni` usage in `rust_mc_core/src/*`.
  - Symptoms: many short native calls, GC allocation spikes, unexpected pauses.
  - Short-term mitigations: audit Rust native code for `get_primitive_array_critical`, use `critical` APIs where safe, batch small items into single JNI calls.

- DNS/Network resolution
  - Why: synchronous DNS lookups on UI thread or frequent small resolution calls. The code uses batch resolver but fallbacks may revert to blocking Java InetAddress calls.
  - Key code: `NativeBridge.dnsBatchResolve`, `rustDnsBatchResolve`, `dnsBatchResolveJava`.
  - Symptoms: stutter when opening server list, slow server pings.
  - Short-term mitigations: ensure DNS batch resolver is used and enable `enableDnsCache`; persist cache so first-run cost is smaller.

- Build and tooling overhead
  - Why: stripping or inlining differences when building for profiling can mask hot paths; global Mutex in GPU compute paths can create contention.
  - Key code: `rust_mc_core/*` (thread-local buffer pooling implemented recently), `wgpu_mesher.rs`.
  - Symptoms: contention on LOD or AO generation; inconsistent profiler results between release and profiling builds.
  - Short-term mitigations: use thread-local or sharded pools (already implemented), build with `RUSTFLAGS` for profiling and compare.

Files to review (quick scan)

- Java: `src/main/java/com/alexxiconify/rustmc/NativeBridge.java`
- Java util: `src/main/java/com/alexxiconify/rustmc/util/ParticleTickDispatcher.java`
- Mixins: `src/main/java/com/alexxiconify/rustmc/mixin/**` (especially network & DH mixins)
- Rust: `rust_mc_core/src/frustum.rs`, `particles.rs`, `wgpu_mesher.rs`, `net.rs` (decoder), `pipeline.rs`

Immediate actions (prioritized)

1. Enable `DiagnosticMode.TIMING`, reproduce problem scenes, collect `logs/latest.log`, JFR, and ETW traces. Use `scripts/collect-profiling.ps1` to bundle artifacts. (Owner: you; Est: 1–2h)
2. Compare frustum/particle timings and confirm whether fused native calls outperform Java fallbacks. If native overhead dominates, consider batching further or widening the work per JNI call. (Owner: dev; Est: 2–4h)
3. Audit Rust JNI usage for unnecessary copies; switch safe paths to `get_primitive_array_critical` where possible. (Owner: native dev; Est: 2–6h)
4. Reduce per-frame allocations in Java hot paths (reuse arrays, thread-local scratch buffers). (Owner: dev; Est: 1–3h)
5. Add CI checks to run `cargo clippy` and `./gradlew build` and fail on warnings to keep hygiene. (Owner: infra; Est: 2–3h)

Risk notes

- Profiling builds change inlining; always compare optimized release builds after tuning.
- Changing JNI array access can introduce crashes if not carefully tested; add unit tests and safety fallbacks.

Where to record results

- Update `ROADMAP.md` with prioritized action items (done).
- Attach profiling artifacts and add findings to this file and `docs/completed-changes.md` after fixes are implemented.

---

Last updated: May 1, 2026