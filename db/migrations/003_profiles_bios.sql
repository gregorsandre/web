-- Up Migration
-- The cities table
CREATE TABLE cities (
        id      serial PRIMARY KEY,
        name    text NOT NULL,
        country text NOT NULL,
        UNIQUE (name, country)
);

CREATE TABLE profiles (
      user_id      uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
      display_name text NOT NULL DEFAULT '',
      about_me     text NOT NULL DEFAULT '',
      avatar_path  text,                          -- NULL = show the "👤" placeholder
      completed    boolean NOT NULL DEFAULT false, -- set by the service once profile + bio are valid
      updated_at   timestamptz NOT NULL DEFAULT now()
);

-- The biographical data points that power recommendations
-- (sports live in tags/user_tags, see 004)
CREATE TABLE bios (
      user_id        uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
      skill_level    smallint NOT NULL CHECK (skill_level BETWEEN 1 AND 5),
      goal           text NOT NULL CHECK (goal IN ('fun','fitness','competition','event_training')),
      looking_for    text NOT NULL CHECK (looking_for IN ('regular_partner','casual_games')),
      env_pref       text NOT NULL CHECK (env_pref IN ('indoor','outdoor','both')),
      age            smallint NOT NULL CHECK (age BETWEEN 18 AND 120),
      age_min        smallint NOT NULL CHECK (age_min >= 18),
      age_max        smallint NOT NULL CHECK (age_max <= 120),
      city_id        integer NOT NULL REFERENCES cities(id),
      availability   text[] NOT NULL CHECK (
   cardinality(availability) > 0
   AND availability <@ ARRAY['weekday_morning','weekday_evening','weekend']
 ),
      updated_at     timestamptz NOT NULL DEFAULT now(),
      CHECK (age_min <= age_max)
);

-- Down Migration
DROP TABLE bios;
DROP TABLE profiles;
DROP TABLE cities;