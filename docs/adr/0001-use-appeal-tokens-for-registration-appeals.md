# Use Appeal Tokens for Registration Appeals

Denied registrations use a short-lived appeal token rather than a normal login session. This lets the denied person prove control of the existing registration and submit a registration appeal without becoming an authenticated member who can access activities, reservations, or admin routes.

Considered option: issue a normal login token for denied users and rely on route-level checks. Rejected because the current authentication boundary treats only approved users as members, and weakening that boundary would make every protected endpoint responsible for handling denied users correctly.
