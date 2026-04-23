alter table users
  add column user_type varchar(20) not null default 'STUDENT',
  add column is_admin boolean not null default false;

update users
set user_type = case
    when role = 'ADMIN' then 'STAFF'
    else 'STUDENT'
  end,
  is_admin = case
    when role = 'ADMIN' then true
    else false
  end;

alter table users
  alter column student_number drop not null,
  alter column phone_number drop not null;

alter table users
  drop constraint if exists users_student_number_not_blank_chk,
  drop constraint if exists users_phone_number_not_blank_chk;

alter table users
  drop column role;
