create table activities (
  id bigserial primary key,
  title varchar(120) not null
);

create table users (
  id bigserial primary key,
  username varchar(30) not null unique,
  email varchar(120) not null unique,
  password_hash varchar(255) not null,
  created_at timestamptz not null default now(),
  role varchar(20) not null default 'STUDENT'
)