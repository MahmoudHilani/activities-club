update users
set
  username = btrim(username),
  email = lower(btrim(email))
where username <> btrim(username)
   or email <> lower(btrim(email));

alter table users
  add column updated_at timestamptz not null default now();

alter table users
  add constraint users_username_not_blank_chk check (length(btrim(username)) > 0),
  add constraint users_email_not_blank_chk check (length(btrim(email)) > 0);

create or replace function set_users_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

create trigger users_set_updated_at
before update on users
for each row
execute function set_users_updated_at();

create unique index users_email_lower_uk on users (lower(email));
create unique index users_username_lower_uk on users (lower(username));
create index users_role_idx on users (role);
create index users_created_at_idx on users (created_at desc);
