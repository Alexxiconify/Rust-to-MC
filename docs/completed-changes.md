# Completed Changes

This document records optimization and stability work that has already been completed in **Rust to MC**. It is the historical companion to `ROADMAP.md`, which now focuses on planned and in-progress work.

## Verified Completed Work

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