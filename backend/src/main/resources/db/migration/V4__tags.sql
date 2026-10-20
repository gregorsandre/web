-- Sports are tags with category = 'sport'. Other categories could be added later.
CREATE TABLE tags (
                      id       serial PRIMARY KEY,
                      category text NOT NULL,
                      name     text NOT NULL,
                      UNIQUE (category, name)
);

CREATE TABLE user_tags (
                           user_id uuid    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           tag_id  integer NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,
                           PRIMARY KEY (user_id, tag_id)
);

INSERT INTO tags (category, name) VALUES
                                      ('sport', 'running'),
                                      ('sport', 'tennis'),
                                      ('sport', 'football'),
                                      ('sport', 'basketball'),
                                      ('sport', 'cycling'),
                                      ('sport', 'swimming'),
                                      ('sport', 'climbing'),
                                      ('sport', 'yoga'),
                                      ('sport', 'badminton'),
                                      ('sport', 'volleyball'),
                                      ('sport', 'hiking'),
                                      ('sport', 'padel'),
                                      ('sport', 'table tennis'),
                                      ('sport', 'gym');