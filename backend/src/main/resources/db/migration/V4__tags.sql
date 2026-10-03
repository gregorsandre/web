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

