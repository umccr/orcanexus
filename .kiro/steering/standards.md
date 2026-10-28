---
inclusion: always
---
# Project Standards

Repo hygiene and safety rules for anyone working in this project. These are
project-scoped and intentionally exclude personal interaction preferences.

- Respect files and directories whose names start with a dot (`.`); do not treat
  them as disposable or auto-generated unless proven so.
- Ask permission before reading `.env` (and avoid echoing secret values).
- Ignore anything under the `.local/` directory — it is local scratch, not part of
  the project.
