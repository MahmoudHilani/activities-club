alter table activities
  add column image_path varchar(255) not null default 'placeholder-activity.svg',
  add column ticket_price numeric(10,2) not null default 0;

alter table activities
  add constraint activities_ticket_price_non_negative_chk
    check (ticket_price >= 0);
