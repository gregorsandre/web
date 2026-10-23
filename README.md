Just notes for now:

## Starting up the database and creating test users:
Make sure the docker engine is running then use following commands


Windows
```
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset"        # clears the database
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset,seed"   # clears the database and creates 120 test users
.\mvnw.cmd spring-boot:run                                           # for normal testing of register and login
```

For MacOs and Linux replace `.\mvnw.cmd` with `./mnvw`

The default database size is 120 users, which is still a rather small number of users. In order to change the number of users generated, use the following command and replace 500 with the number of users desired (digits only, no spaces):
```
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset,seed" "-Dspring-boot.run.jvmArguments=-Dseed.count=500"
```
to wipe the database completely:
```
docker compose down -v
```

Checks to see if database seeder worked properly. Run these in the terminal.
1. Open psql
```
docker compose exec db psql -U sportsbuddies -d sportsbuddies
```
2. Turn off the pager, so the output stays on screen:
```
\pset pager off
```
3. Run each query on its own, ending with the semicolon:
```
SELECT count(*) FROM users;
```
What you should see after `reset,seed`

- users: 120, the seeder’s default

What a wrong result means

- users is 0: the seed profile didn’t run. Check that the start command had `reset,seed` and read the backend terminal for an error.
- The backend stopped with “No sports in the tags table”: the same cause as above.
- A duplicate email error: the seed ran twice on the same database. Run it together with `reset`.

Type `\q` to leave psql when you’re done.



starting up the frontend (needs the backend running on port 8080):
```
cd frontend
npm install
npm run dev        # http://localhost:5173, API calls are proxied to the backend under /api
```


## Recommendation algorithm

Recommendations are produced in two stages. Hard requirements are enforced in the database query. Soft preferences are scored in the service. A person who fails a hard requirement is never shown. A person who passes them is ranked by how well their profile matches.

### Data points used

Age, preferred age range, skill level, goal, what they are looking for, indoor/outdoor preference, weekly availability, location with a maximum distance, and sports (tags).

### Stage 1: hard requirements (SQL)

A candidate is only considered if all of these are true:

| Rule                                                                  | Why                                                                                                                                 |
|-----------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Both profiles are completed                                           | Recommendations only exist for finished profiles.                                                                                   |
| Each person's age is inside the other's preferred range               | The preference has to hold in both directions, or one side would be unwelcome.                                                      |
| Each is inside the other's maximum distance                           | Distance is checked against the smaller of the two radii, so nobody is recommended to someone who does not want to travel that far. |
| They share at least one sport                                         | This is a sports buddy app, so a pair with nothing in common is a weak match by definition.                                         |
| They are looking for the same thing (regular partner or casual games) | These are different commitments, so a mismatch is a real incompatibility, not a small difference.                                   |
| The user has not dismissed them                                       | A dismissed person is never recommended again.                                                                                      |
| No connection or pending request exists between them                  | They are already in each other's connections or requests.                                                                           |

The query returns the nearest 300 candidates. Distance is calculated in the database with PostGIS on stored coordinates from the browser's location.

### Stage 2: scoring (maximum 10 points)

| Factor | Condition | Points |
|---|---|---|
| Age | Age difference of 5 years or less | +2 |
| Sports | Two or more shared sports | +2 |
| Skill level | Within one level | +1 |
| Skill level | Exactly the same level | +1 more |
| Goal | Same goal | +1 |
| Environment | Same indoor/outdoor preference, or either of them is happy with both ("indoor or outdoor") | +1 |
| Availability | At least one shared time slot | +1 |
| Availability | Two or more shared time slots | +1 more |

Where a factor has two rows, the second row is added to the first, so a closer match earns more.

### Threshold

A candidate needs at least **4 of 10 points** to be recommended. Everyone who reaches the scoring stage already passes the hard requirements, so 4 points means they also agree on a good part of the softer details. Below that, the match is weak and is left out, even if that leaves the list short. A short list is better than a list of people the user does not want to connect with.

### Ranking and limit

1. Highest score first.
2. Equal scores: nearest first.
3. Still equal: by user id, so the order is stable.

At most **10** recommendations are returned at a time. The endpoint returns only their ids.

### Why the rules are split this way

- **Hard rules** are things that make a match impossible or pointless, such as age range, distance, no common sport, or wanting a different kind of partner. Putting them in the query means the database does the filtering, and only the remaining candidates are scored.
- **Soft rules** are things where being different is fine but being similar is better, such as skill, goal, environment and availability. These affect the order and the cut-off, not whether someone is eligible.

### Demonstrating it

The seed data creates 120 users spread over several Estonian regions, with random profiles. To see the rules at work:

- Log in as any seeded user (for example `user1@example.com`, password `password123`) and open Discover.
