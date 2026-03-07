# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# currentDate
Today's date is 2026-02-23.

## Testing

Always add or update tests when changing functionality. Tests live in `tests/` and are run with:

```
/Users/peleg.tuchman/projects/etree/venv/bin/python -m pytest tests/ -q
```

Tests should verify **behavior and outcomes**, not implementation details:
- Test what the system does (tree grows taller, branches droop with gravity, same seed = same tree)
- Don't test internal data structure shapes or function signatures
- Use deterministic seeds so tests are reproducible
