---
description: Workflow rules for ROA API agent when executing `/roa-api-architect.md`.
---

# ROA API AI Agent Rules

**CRITICAL RULE:** ALL phases in `/roa-api-architect.md` are MANDATORY and MUST be completed in the exact order.
- Complete every task and subtask in each phase.
- NEVER skip phases.
- Work precise and methodical. Don’t rush.

**RULE:** Always follow IMPORTANT-labeled instructions in:
- `.claude/rules/rules.md`
- `.claude/rules/best-practices.md`
- `.claude/examples/api-test-examples.md`
- `.claude/instructions/core-framework-instructions.md`
- `.claude/instructions/api-framework-instructions.md`

**RULE:** After finishing each task/subtask that required coding:
1) run `mvn -q test-compile`
2) if errors occur → fix them BEFORE continuing

**RULE:** When fixing compile errors:
- DO refer to `skill-name:pandora` `.claude/skills/pandora/SKILL.md`
- DON’T delete classes that fail to compile
- DON’T “simplify” by removing required framework pieces (endpoints, DTOs, JSONPaths, auth classes, hooks, tests)
- DON’T remove validations from tests to “make it pass”

**RULE:** If Pandora metadata seems outdated → regenerate it:
- Run `mvn pandora:open -U`

**RULE:** API discovery must be evidence-based:
- Use Swagger MCP tools to confirm endpoint path/method/payload when unclear
- Never guess endpoint contracts

**RULE:** Tests MUST follow ROA API rules:
- Use `quest.use(RING_OF_API)` only (no direct RestAssured in tests)
- Use typed endpoints (`AppEndpoints`) only
- No wildcard imports
- No hardcoded URLs / secrets / raw magic strings
- Always end with `.complete()`

**RULE:** Run tests for real:
- Do NOT mark work complete unless tests were actually executed via `/roa-api-run-tests.md`