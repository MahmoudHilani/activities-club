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

**Denied Registration**:
A registration refused by an admin that may return for review only through an appeal by the same person.
_Avoid_: Rejected user, blocked account

**Registration Appeal**:
A denied person's request to update their registration details and return the registration for admin review.
_Avoid_: Re-registration, reopen signup

**Sports Club Signup**:
A standalone submission expressing interest in joining one or more sports clubs, captured from the home page form and stored independently of account registration.
_Avoid_: Sports membership, club registration, sports application

**Sports Club**:
A named sport offered as a selectable option on a **Sports Club Signup** (e.g. football, basketball, badminton, volleyball).
_Avoid_: Sport category, activity type

## Relationships

- A **Date of Birth** is the source fact for deciding whether a student is an **Underage Student**.
- An **Unknown Date of Birth** does not make a student an **Underage Student**.
- An **Underage Student** must not hold an active reservation or waitlist place for an **Overnight Activity**.
- An **Overnight Activity** is identified by an explicit admin decision, not inferred from start or end times.
- A **Date of Birth** may be recorded during approval or later from user management.
- A recorded **Date of Birth** remains on a student **Denied Registration** that returns for review, but not when its **Registration Appeal** changes it to staff.
- A **Denied Registration** returns for review only through a **Registration Appeal**, not by a new registration or by an admin reversing denial.
- A **Registration Appeal** requires the denied person to prove control of the existing registration.
- A **Registration Appeal** updates registration details, not account credentials.
- A **Sports Club Signup** is independent of a **Registration**; an anonymous visitor or a signed-in user may submit one.
- A **Sports Club Signup** submitted by a signed-in user is linked to that user; a submission by an anonymous visitor is not linked.
- A person may submit more than one **Sports Club Signup**; admins dedupe by email.
- A **Sports Club Signup** does not change a user's profile fields.

## Example dialogue

> **Dev:** "If a student is 17 when approved but 18 before the activity starts, are they an **Underage Student** for that activity?"
> **Domain expert:** "No, age is evaluated against the activity start date."

## Flagged ambiguities

- "underage" was used as a stable student attribute, but it is date-relative; resolved as **Underage Student** evaluated on the activity start date.
- "missing date of birth" was treated as underage; resolved as **Unknown Date of Birth**, which does not block overnight reservations.
- "reopen denied" suggested an admin action; resolved as a **Denied Registration** returning for review only through a **Registration Appeal**.
- "denial clears date of birth" was left implicit; resolved: a recorded **Date of Birth** remains for a returning student registration and is removed when an appeal changes it to staff.
- "re-register after denial" suggested a new signup; resolved as a **Registration Appeal** by the same denied person.
- "appeal" could include credential recovery; resolved: a **Registration Appeal** updates registration details only.
