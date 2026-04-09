# Guide

1. **Bug Proofing**: Make code robust with proper error handling and null checks.
2. **Minimalism**: Keep comments non-verbose, simple, and strictly necessary.
3. **Conciseness**: Keep all chat output short and focused on the task.
4. **Environment**: ALWAYS use **Windows 11 / PowerShell 7** native commands.
   - Avoid Linux-specific commands (grep, sed, awk, etc.).
   - Use `Select-String` instead of `grep`.
   - Use `Get-Content` and `Set-Content` instead of `cat` and `>` redirects for complex data.
   - Use `$null` instead of `/dev/null`.
   - Use `Remove-Item` instead of `rm -rf`.
   - Prefer standard PowerShell aliases or full cmdlets for clarity in diagnostic commands.
5. **Roadmap**: ALWAYS update `ROADMAP.md` after implementing new features, fixing major bugs, or identifying new technical goals. Keep progress status and the "Completed" section current.
