# Rollback Archive: `v1.0.3-a3`

## Current status

- Rollback snapshot archived.
- All usable rollback notes consolidated here.
- No active rollback work remains.

## Still-open issues

- None recorded.

## Actionable TODOs

- Re-check native culling, lighting, and compat glue only if a regression returns.
- Re-check `LightingMixin`, `NativeBridge`, `FrustumMixin`, and HUD helpers if hot-path regressions reappear.
- Keep any future rollback note short and tied to active roadmap work.

## Verification notes

- Full history lives in [`completed-changes.md`](completed-changes.md).
- Active work lives in [`../ROADMAP.md`](../ROADMAP.md).
- Rollback snapshot: target `v1.0.3-a3` (`12e8073`), rolled back from `main` at `9cd2790`.
- Backup branch: `backup/pre-rollback-v1.0.3-a3-20260412-210316`.
- Commits removed in rollback: 13 (`8afb27f` → `9cd2790`).
- Scope touched: `LightingMixin`, `NativeBridge`, `FrustumMixin`, compat mixins, render HUD helpers.
- Historical totals: 83 files changed, 2438 insertions, 1519 deletions.