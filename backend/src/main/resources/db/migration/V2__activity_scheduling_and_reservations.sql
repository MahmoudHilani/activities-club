alter table activities
  add column description text,
  add column organizer_id bigint references users(id) on delete set null,
  add column start_at timestamptz,
  add column end_at timestamptz,
  add column location_name varchar(160),
  add column location_address varchar(255),
  add column capacity integer,
  add column status varchar(20) not null default 'DRAFT',
  add column visibility varchar(20) not null default 'PUBLIC',
  add column reservation_opens_at timestamptz,
  add column reservation_closes_at timestamptz,
  add column created_at timestamptz not null default now(),
  add column updated_at timestamptz not null default now();

alter table activities
  add constraint activities_capacity_positive_chk
    check (capacity is null or capacity > 0),
  add constraint activities_end_after_start_chk
    check (end_at is null or start_at is null or end_at > start_at),
  add constraint activities_reservation_window_chk
    check (
      reservation_closes_at is null
      or reservation_opens_at is null
      or reservation_closes_at >= reservation_opens_at
    );

create index activities_organizer_id_idx on activities(organizer_id);
create index activities_start_at_idx on activities(start_at);
create index activities_status_idx on activities(status);

create table activity_reservations (
  id bigserial primary key,
  activity_id bigint not null references activities(id) on delete cascade,
  user_id bigint not null references users(id) on delete cascade,
  status varchar(20) not null default 'RESERVED',
  reserved_at timestamptz not null default now(),
  cancelled_at timestamptz,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint activity_reservations_activity_user_uk unique (activity_id, user_id),
  constraint activity_reservations_cancelled_status_chk
    check (
      cancelled_at is null
      or status = 'CANCELLED'
      or status = 'NO_SHOW'
    )
);

create index activity_reservations_activity_id_idx on activity_reservations(activity_id);
create index activity_reservations_user_id_idx on activity_reservations(user_id);
create index activity_reservations_status_idx on activity_reservations(status);
