# CONTEXT.md Format

## Structure

```md
# {Context Name}

{One or two sentence description of what this context is and why it exists.}

## Language

**Order**:
{A concise description of the term}
_Avoid_: Purchase, transaction

**Invoice**:
A request for payment sent to a customer after delivery.
_Avoid_: Bill, payment request

**Customer**:
A person or organization that places orders.
_Avoid_: Client, buyer, account

## Relationships

- An **Order** produces one or more **Invoices**
- An **Invoice** belongs to exactly one **Customer**

## Example dialogue

> **Dev:** "When a **Customer** places an **Order**, do we create the **Invoice** immediately?"
> **Domain expert:** "No - an **Invoice** is only generated once a **Fulfillment** is confirmed."

## Flagged ambiguities

- "account" was used to mean both **Customer** and **User** - resolved: these are distinct concepts.
```

## Rules

- Be opinionated: when multiple words exist for the same concept, pick one and
  list aliases to avoid.
- Flag conflicts explicitly with a clear resolution.
- Keep definitions tight: one sentence max, defining what the concept is.
- Show relationships with bold term names and cardinality where it is clear.
- Include only concepts specific to this project's context, not general
  programming terminology.
- Group terms under subheadings when natural clusters emerge.
- Include an example dialogue that clarifies related concept boundaries.

## Single Versus Multiple Contexts

For a single context, keep `CONTEXT.md` at the repository root.

For multiple contexts, keep `CONTEXT-MAP.md` at the repository root and use it
to identify the relevant context-specific `CONTEXT.md` and `docs/adr/`
location.

If neither file exists, create a root `CONTEXT.md` lazily when the first term
is resolved. If the relevant context is unclear, ask.
