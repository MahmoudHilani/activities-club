alter table users
  add column date_of_birth date;

alter table activities
  add column is_overnight boolean not null default false;
