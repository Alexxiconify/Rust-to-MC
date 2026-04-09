# Guide

1. **Bug Proofing**: Make code robust with proper error handling and null checks.
2. **Minimalism**: Keep comments non-verbose, simple, and strictly necessary.
3. **Conciseness**: Keep all chat output short and focused on the task.
4. **Environment**: PRIORITIZE using Antigravity's built-in tools over running shell commands. If you run a shell command, use **Windows 11 / PowerShell 7** native commands.
   - Avoid Linux-specific commands (grep, sed, awk, etc.).
   - Use built-in tools like `grep_search` or `view_file` instead of shell commands whenever possible.
   - If you must use shell commands, use `Select-String`, `$null`, `Remove-Item` instead of `grep`, `/dev/null`, `rm -rf`.
5. **Roadmap**: ALWAYS update `ROADMAP.md` after implementing new features, fixing major bugs, or identifying new technical goals. Keep progress status and the "Completed" section current.
6. **Reduce Thinking**: Make sure to only think when necessary avoid a ton of thinking and just do the task.
