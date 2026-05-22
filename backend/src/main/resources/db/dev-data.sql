with organizer as (
  select id
  from users
  where lower(username) = lower('admin')
  order by id
  limit 1
)
insert into activities (
  title,
  description,
  organizer_id,
  start_at,
  end_at,
  location_name,
  location_address,
  capacity,
  image_path,
  ticket_price,
  is_overnight,
  status,
  visibility,
  reservation_opens_at,
  reservation_closes_at
)
select
  seed.title,
  seed.description,
  organizer.id,
  seed.start_at,
  seed.end_at,
  seed.location_name,
  seed.location_address,
  seed.capacity,
  seed.image_path,
  seed.ticket_price,
  seed.is_overnight,
  seed.status,
  seed.visibility,
  seed.reservation_opens_at,
  seed.reservation_closes_at
from organizer
cross join (
  values
    (
      'Campus Climbing Night',
      'Beginner-friendly indoor climbing session with equipment included and club leaders on hand for belay support.',
      timestamp with time zone '2026-06-03 18:30:00+01',
      timestamp with time zone '2026-06-03 21:00:00+01',
      'Gravity Climbing Centre',
      'Unit 6, Riverside Business Park, Dublin',
      18,
      'placeholder-activity.svg',
      12.50,
      false,
      'PUBLISHED',
      'PUBLIC',
      timestamp with time zone '2026-05-20 09:00:00+01',
      timestamp with time zone '2026-06-02 17:00:00+01'
    ),
    (
      'Kayaking Skills Morning',
      'Flat-water kayaking practice covering paddle control, rescue basics, and confidence-building drills.',
      timestamp with time zone '2026-06-08 09:30:00+01',
      timestamp with time zone '2026-06-08 12:30:00+01',
      'Grand Canal Dock',
      'Hanover Quay, Dublin',
      14,
      'placeholder-activity.svg',
      18.00,
      false,
      'PUBLISHED',
      'PUBLIC',
      timestamp with time zone '2026-05-22 10:00:00+01',
      timestamp with time zone '2026-06-06 18:00:00+01'
    ),
    (
      'Weekend Wicklow Hike',
      'A full-day guided hike through Wicklow trails with a steady pace, lunch stop, and return transport.',
      timestamp with time zone '2026-06-14 08:00:00+01',
      timestamp with time zone '2026-06-14 18:00:00+01',
      'Glendalough Visitor Centre',
      'Glendalough, County Wicklow',
      25,
      'placeholder-activity.svg',
      22.00,
      false,
      'PUBLISHED',
      'PUBLIC',
      timestamp with time zone '2026-05-22 10:00:00+01',
      timestamp with time zone '2026-06-12 18:00:00+01'
    ),
    (
      'Overnight Surf Trip',
      'Two-day Lahinch surf trip with coaching, shared accommodation, and evening group meal included.',
      timestamp with time zone '2026-06-27 07:30:00+01',
      timestamp with time zone '2026-06-28 19:00:00+01',
      'Lahinch Beach',
      'Promenade, Lahinch, County Clare',
      20,
      'placeholder-activity.svg',
      85.00,
      true,
      'PUBLISHED',
      'PUBLIC',
      timestamp with time zone '2026-05-25 09:00:00+01',
      timestamp with time zone '2026-06-20 17:00:00+01'
    ),
    (
      'Draft: Archery Taster',
      'Draft sample activity for checking admin filtering and edit screens before publishing.',
      timestamp with time zone '2026-07-02 17:00:00+01',
      timestamp with time zone '2026-07-02 19:00:00+01',
      'Sports Hall',
      'Main Campus',
      16,
      'placeholder-activity.svg',
      6.00,
      false,
      'DRAFT',
      'PUBLIC',
      timestamp with time zone '2026-06-15 09:00:00+01',
      timestamp with time zone '2026-07-01 17:00:00+01'
    )
) as seed(
  title,
  description,
  start_at,
  end_at,
  location_name,
  location_address,
  capacity,
  image_path,
  ticket_price,
  is_overnight,
  status,
  visibility,
  reservation_opens_at,
  reservation_closes_at
)
where not exists (
  select 1
  from activities
  where activities.title = seed.title
);
