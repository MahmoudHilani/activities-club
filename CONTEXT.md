# Activities Club

Activities Club manages student and staff registrations, activities, and reservations for club events.

## Language

**Underage Student**:
A student who will be under 18 on an activity's start date.
_Avoid_: Minor, child, underage user

**Date of Birth**:
The student birth date recorded by an admin for a student registration.
_Avoid_: Age, underage flag

**Unknown Date of Birth**:
A missing student birth date that is not enough evidence to classify the student as underage.
_Avoid_: Missing age, assumed adult

**Overnight Activity**:
An activity that includes an overnight stay.
_Avoid_: Overnight trip, adult-only activity

## Relationships

- A **Date of Birth** is the source fact for deciding whether a student is an **Underage Student**.
- An **Unknown Date of Birth** does not make a student an **Underage Student**.
- An **Underage Student** must not hold an active reservation or waitlist place for an **Overnight Activity**.
- An **Overnight Activity** is identified by an explicit admin decision, not inferred from start or end times.
- A **Date of Birth** may be recorded during approval or later from user management.

## Example dialogue

> **Dev:** "If a student is 17 when approved but 18 before the activity starts, are they an **Underage Student** for that activity?"
> **Domain expert:** "No, age is evaluated against the activity start date."

## Flagged ambiguities

- "underage" was used as a stable student attribute, but it is date-relative; resolved as **Underage Student** evaluated on the activity start date.
- "missing date of birth" was treated as underage; resolved as **Unknown Date of Birth**, which does not block overnight reservations.
