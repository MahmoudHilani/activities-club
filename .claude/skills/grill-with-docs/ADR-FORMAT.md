# ADR Format

ADRs live in `docs/adr/` and use sequential numbering:
`0001-slug.md`, `0002-slug.md`, and so on.

Create `docs/adr/` only when the first ADR is needed.

## Template

```md
# {Short title of the decision}

{One to three sentences: what is the context, what was decided, and why.}
```

Optional sections are appropriate only when they add real value:

- Status: `proposed`, `accepted`, `deprecated`, or `superseded by ADR-NNNN`
- Considered Options: when rejected alternatives are worth retaining
- Consequences: when downstream effects are not obvious

## Numbering

Scan `docs/adr/` for the highest existing number and increment it.

## When To Offer An ADR

Only offer an ADR when all three conditions hold:

1. The decision is hard to reverse.
2. The decision would be surprising without context.
3. The decision results from a genuine trade-off.
