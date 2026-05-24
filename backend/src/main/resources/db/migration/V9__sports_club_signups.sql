create table sports_club_signups (
  id bigserial primary key,
  name varchar(120) not null,
  email varchar(120) not null,
  phone_number varchar(30) not null,
  student_number varchar(30) not null,
  course varchar(120) not null,
  gender varchar(20) not null,
  user_id bigint references users(id) on delete set null,
  created_at timestamptz not null default now(),
  constraint sports_club_signups_gender_chk check (gender in ('MALE', 'FEMALE', 'OTHER'))
);

create index sports_club_signups_created_at_idx on sports_club_signups (created_at desc);
create index sports_club_signups_email_idx on sports_club_signups (email);
create index sports_club_signups_user_id_idx on sports_club_signups (user_id);

create table sports_club_signup_clubs (
  signup_id bigint not null references sports_club_signups(id) on delete cascade,
  sports_club varchar(20) not null,
  primary key (signup_id, sports_club),
  constraint sports_club_signup_clubs_club_chk
    check (sports_club in ('FOOTBALL', 'BASKETBALL', 'BADMINTON', 'VOLLEYBALL'))
);

create index sports_club_signup_clubs_club_idx on sports_club_signup_clubs (sports_club);
