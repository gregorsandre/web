Just notes for now:

starting up the database and creating test/fake users:
1. make sure the docker engine is running then use following commands
```
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset"        # clears the database
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset,seed"   # clears the database and creates 120 test users
.\mvnw.cmd spring-boot:run                                           # for normal testing of register and login
```
to wipe the database completely:
```
docker compose down -v
```

Checks to see if database seeder worked properly. Run these in your terminal.
1. Open psql
```
docker exec -it web-db-1 psql -U sportsbuddies -d sportsbuddies
```
2. Turn off the pager, so the output stays on screen:
```
\pset pager off
```
3. Run each query on its own, ending with the semicolon:
```
SELECT count(*) FROM users;
SELECT count(*) FROM tags;
SELECT count(*) FROM user_tags;
```
What you should see after reset,seed

- users: 120, the seeder’s default and above the spec’s minimum of 100.
- tags: 14, the sports from migration 4.
- user_tags: a few hundred. Each user gets 1 to 4 sports, so about 300.

What a wrong result means

- users is 0: the seed profile didn’t run. Check that the start command had `reset,seed` and read the backend terminal for an error.
- tags is 0: migration 4 doesn’t contain the sports insert, or the reset didn’t run.
- The backend stopped with “No sports in the tags table”: the same cause as above.
- A duplicate email error: the seed ran twice on the same database. Run it together with `reset`.

One more quick check, to see sample users with their sports:
```
SELECT p.display_name, count(t.tag_id) AS sports FROM profiles p LEFT JOIN user_tags t ON t.user_id = p.user_id GROUP BY p.display_name LIMIT 5;
```
Type `\q` to leave psql when you’re done.



starting up the frontend (needs the backend running on port 8080):
```
cd frontend
npm install
npm run dev        # http://localhost:5173, API calls are proxied to the backend under /api
```
