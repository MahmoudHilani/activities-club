alter table users
  add column approval_status varchar(20);

update users
set approval_status = 'APPROVED'
where approval_status is null;

alter table users
  alter column approval_status set default 'APPROVED';

alter table users
  alter column approval_status set not null;

alter table users
  add constraint users_approval_status_chk check (approval_status in ('PENDING', 'APPROVED', 'DENIED'));

create index users_approval_status_idx on users (approval_status);
