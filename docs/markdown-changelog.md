# Markdown Changelog

This file lists conservative, low-risk markdown edits applied to prepare the repo for profiling and roadmap cleanup. All original content preserved where possible; edits primarily add links, a short findings summary, and helper docs/scripts.

Edits applied (May 1, 2026)

- `ROADMAP.md`
  - Added top-level "Findings & Prioritized Actions" summary including hotspot list and short-term owner/ETA annotations. Left existing roadmap text unchanged below the new summary.

- `README.md`
  - Added a "Profiling & Performance" section linking to `docs/profiling.md` and describing available helper scripts.

- `ModInfo.md`
  - Added a short note referencing `ROADMAP.md` and `docs/profiling.md` to guide compatibility evaluation with profiling artifacts.

- `docs/profiling.md` (new)
  - Added detailed PowerShell commands for static analysis, building rust with symbols, JVM Flight Recorder usage, WPR/ETW capture, and artifact bundling instructions.

- `scripts/collect-profiling.ps1` (new)
  - PowerShell helper to build rust with symbols, run WPR for a configurable interval, gather artifacts, and bundle into `profiling-artifacts.zip`.

- `scripts/repo-health.ps1` (new)
  - PowerShell helper to run `cargo check`, `cargo clippy`, and `./gradlew build` and collect their outputs under `.\health\`.

If you need the edits to be more aggressive (line-level rewrites or reorderings), request a follow-up and I will prepare a stricter diff for review.

---

Last updated: May 1, 2026