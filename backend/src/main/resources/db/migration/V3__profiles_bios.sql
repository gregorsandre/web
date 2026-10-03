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
                      lat            double precision NOT NULL CHECK (lat BETWEEN -90 AND 90),
                      lng            double precision NOT NULL CHECK (lng BETWEEN -180 AND 180),
                      max_radius_km  integer NOT NULL CHECK (max_radius_km BETWEEN 1 AND 500),
                      availability   text[] NOT NULL CHECK (
                   cardinality(availability) > 0
                   AND availability <@ ARRAY['weekday_morning','weekday_evening','weekend']
                 ),
    -- derived from lat/lng so the two can never disagree; used for radius queries
                      location       geography(Point,4326) GENERATED ALWAYS AS
                   (ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography) STORED,
                      updated_at     timestamptz NOT NULL DEFAULT now(),
                      CHECK (age_min <= age_max)
);