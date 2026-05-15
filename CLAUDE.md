# Claude Project Instructions

Before making changes, read the shared project context:

- `CONTEXT.md`

Use the repository's existing patterns and preserve unrelated working-tree changes.
Do not revert, reformat, or clean up files outside the requested scope unless explicitly asked.

## Project Language

Use the terminology in `CONTEXT.md` for domain concepts. In particular:

- Use "Date of Birth" for the admin-recorded student birth date.
- Use "Underage Student" only for a student who will be under 18 on an activity's start date.
- Treat an unknown date of birth as missing evidence, not as proof that a student is underage.
- Treat "Overnight Activity" as an explicit admin decision.

## Useful Codex Agent References

Claude does not automatically execute Codex skills, but these files contain useful project workflows. Read the relevant file when the task matches:

- Frontend UI work: `.agents/skills/frontend-ui-engineering/SKILL.md`
- Tests or behavior changes: `.agents/skills/test-driven-development/SKILL.md`
- Debugging failures: `.agents/skills/debugging-and-error-recovery/SKILL.md`
- Code review: `.agents/skills/code-review-and-quality/SKILL.md`
- Git workflow: `.agents/skills/git-workflow-and-versioning/SKILL.md`
- API or type contracts: `.agents/skills/api-and-interface-design/SKILL.md`
- Security-sensitive changes: `.agents/skills/security-and-hardening/SKILL.md`
- Documentation or ADR updates: `.agents/skills/documentation-and-adrs/SKILL.md`

## Verification

Run the narrowest useful tests for the change first, then broader checks when the blast radius justifies it. For frontend changes, prefer:

```bash
cd frontend
npm run type-check
npm test -- <relevant spec path>
```

For backend changes, prefer the relevant Gradle test task from `backend`.
