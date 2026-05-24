---
name: grill-with-docs
description: Challenge a plan against the existing domain model, sharpen terminology, and update documentation (CONTEXT.md and ADRs) inline as decisions crystallize. Use when the user wants to stress-test a plan against project language and documented decisions.
---

# Grill With Docs

Interview the user closely about each aspect of the plan until there is a
shared understanding. Walk down the design tree and resolve dependent
decisions one by one. For each question, provide a recommended answer.

Ask one question at a time and wait for feedback before continuing.

If a question can be answered by exploring the codebase, explore the
codebase instead.

## Domain Awareness

During codebase exploration, look for existing documentation:

- If `CONTEXT-MAP.md` exists at the root, use it to locate the relevant
  bounded context documentation.
- Otherwise, treat a root `CONTEXT.md` as the repository's single glossary.
- If neither exists, create `CONTEXT.md` only once the first term is resolved.
- Create `docs/adr/` only once an ADR is genuinely needed.

## During The Session

### Challenge Against The Glossary

When the user uses a term that conflicts with existing language in
`CONTEXT.md`, call out the mismatch immediately and ask which meaning applies.

### Sharpen Fuzzy Language

When the user uses vague or overloaded terms, propose a precise canonical
term and distinguish it from nearby concepts.

### Discuss Concrete Scenarios

Stress-test domain relationships with concrete edge cases that force precise
boundaries between concepts.

### Cross-Reference With Code

When the user states how something works, check whether the code agrees.
Surface contradictions and resolve them before relying on the claim.

### Update `CONTEXT.md` Inline

When a term is resolved, update `CONTEXT.md` immediately rather than batching
changes. Use [CONTEXT-FORMAT.md](./CONTEXT-FORMAT.md).

`CONTEXT.md` is a glossary, not a specification or an implementation log.
Keep implementation details out of it.

### Offer ADRs Sparingly

Only offer an ADR when the decision is hard to reverse, surprising without
context, and the result of a genuine trade-off. Use
[ADR-FORMAT.md](./ADR-FORMAT.md).
