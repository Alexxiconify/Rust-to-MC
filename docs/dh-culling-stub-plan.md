# DH Culling Notes

## Keep
- `DistantHorizonsCompat` frustum plane update
- Below-Y DH cave gate
- Vanilla lighting, particles, mesh paths
- `NativeBridge.updateVanillaFrustum(...)`
- `NativeBridge.batchCull(...)`
- `NativeBridge.isOutsideFrustum(...)`

## Remove now
- DH visibility cache
- extra rebind churn
- DH fused frustum/occlusion path in `NativeBridge.cullDistantHorizonsSection(...)`
- Rust DH occlusion work tied to section visibility

## Re-add later
- Frustum-only check first
- Cache only if profiler proves win
- More DH-native cull only after frame-time proof

## Policy
- No DH occlusion.
- No hidden occlusion fallback.
- Frustum + below-Y gate only.

## Note
- Runtime path now uses simple frustum + below-Y cull.
- Heavy DH occlusion stays out until profiler says yes.