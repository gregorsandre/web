# Sports Buddies

A web app that finds you sports partners. You fill in a profile with your sports, skill level, goals, availability and location. The app recommends people nearby who fit, and once you connect you can chat in real time.

Built with a Java Spring Boot backend, a React + TypeScript frontend and PostgreSQL (with PostGIS for distances).

## Setup

You need Docker, Java 21 and Node.js 20.19 or newer.

```
docker compose up -d                       # PostgreSQL + PostGIS on port 5432
cd backend
./mvnw spring-boot:run                     # backend on http://localhost:8080
```

In a second terminal:

```
cd frontend
npm install
npm run dev                                # app on http://localhost:5173
```

On Windows use `.\mvnw.cmd` instead of `./mvnw`. The frontend sends API calls to the backend through `/api`.

### Test data

Start the backend once with a profile to reset or fill the database, then stop it (Ctrl+C) and start it again normally. While a `reset` profile is running, every restart wipes the database again.

```
./mvnw spring-boot:run "-Dspring-boot.run.profiles=reset,seed"    # empty the database and create 120 users
./mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"         # only empty the database
./mvnw spring-boot:run "-Dspring-boot.run.profiles=reset,seed" "-Dspring-boot.run.jvmArguments=-Dseed.count=500"   # more users
```

Seeded users are `user1@example.com` to `user120@example.com`, all with the password `password123`. To check the result: `docker compose exec db psql -U sportsbuddies -d sportsbuddies -c "SELECT count(*) FROM users;"`. To remove the database completely: `docker compose down -v`.

### Configuration

Login tokens are signed with `JWT_SECRET`. The built-in default is only for local development; set your own secret (at least 32 characters) anywhere else:

```
JWT_SECRET=your-long-random-secret ./mvnw spring-boot:run
```

## Usage

1. **Register** with an email and a password, then fill in your profile. Your location comes from the browser, so allow location access when asked.
2. **Discover** shows up to 10 recommended people, best match first. Press **Connect** to send a request or **Reject** to never see them again.
3. **Connections** lists incoming requests (Accept or Reject) and the people you are connected with. Open a profile to **Disconnect**.
4. **Message** on a connected person's profile opens your chat. **Chats** lists every chat, most recent first, with a dot in the menu when something is unread.
5. **Profile** shows your own details and email. Use **Edit** to change them, and **Add picture**, **Change picture** or **Remove** for your photo.
6. **Log out** is in the header on every page.

Profiles are only visible to people who are recommended to you, have a pending request with you, or are connected with you. Everyone else gets a 404, the same as for a user that doesn't exist.

## API

| Endpoint | Returns |
|---|---|
| `GET /users/{id}` | `id`, `name`, `avatar` (picture link) |
| `GET /users/{id}/profile` | `id`, `aboutMe` |
| `GET /users/{id}/bio` | biographical data used for matching |
| `GET /me`, `/me/profile`, `/me/bio` | the same for yourself (your bio also includes location, age range and distance) |
| `GET /recommendations` | up to 10 user ids |
| `GET /connections` | ids of connected users |

All requests need `Authorization: Bearer <token>` from `POST /auth/register` or `POST /auth/login`. Real-time chat events use a WebSocket at `/ws`.

## Extra features

- Online/offline status on profiles, in the chat and in the chat list
- "… is typing" indicator in chats
- Proximity filtering with real distances (PostGIS), using the smaller of the two people's maximum distances
- Unread message count per chat
- Pictures are cropped and resized in the browser before upload

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

The seed data creates 120 users with random profiles spread over several Estonian regions. Log in as `user20@example.com` or `user92@example.com` (password `password123`) and open Discover to see ranked recommendations. Many seeded users get few or none, because the hard rules leave out anyone who does not fit.

To see the rules on their own, register two new users in the same place:

- **Good match:** same sports, similar age inside each other's age range, both looking for the same thing. They are recommended to each other.
- **Poor match:** no shared sport, or ages outside each other's range, or one wants a regular partner and the other casual games. Neither sees the other.
