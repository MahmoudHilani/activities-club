alter table sports_club_signup_clubs
  drop constraint sports_club_signup_clubs_club_chk;

alter table sports_club_signup_clubs
  add constraint sports_club_signup_clubs_club_chk
    check (sports_club in (
      'FOOTBALL',
      'BASKETBALL',
      'BADMINTON',
      'VOLLEYBALL',
      'CRICKET',
      'BILLIARDS',
      'DANCE',
      'YOGA',
      'PILATES',
      'BOXERCISE'
    ));
