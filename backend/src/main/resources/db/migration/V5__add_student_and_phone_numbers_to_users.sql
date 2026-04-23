alter table users
  add column student_number varchar(30),
  add column phone_number varchar(30);

alter table users
  alter column student_number set not null,
  alter column phone_number set not null;

alter table users
  add constraint users_student_number_not_blank_chk check (length(btrim(student_number)) > 0),
  add constraint users_phone_number_not_blank_chk check (length(btrim(phone_number)) > 0);
