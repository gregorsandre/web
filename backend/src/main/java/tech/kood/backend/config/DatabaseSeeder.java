package tech.kood.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@Profile("seed")
public class DatabaseSeeder implements CommandLineRunner {

    private static final String[] SPORTS = {"running","tennis","football","basketball","cycling","swimming",
            "climbing","yoga","badminton","volleyball","hiking","padel","table tennis","gym"};
    private static final String[] GOAL = {"fun", "fitness","competition","event_training"};
    private static final String[] LOOKING_FOR = {"regular_partner", "casual_games"};
    private static final String[] ENV = {"indoor","outdoor","both"};
    private static final String[] AVAIL = {"weekday_morning","weekday_evening","weekend"};
    private static final double[][] CITIES = {
            {59.437, 24.754}, {58.378, 26.729}, {58.250, 22.484}, {59.397, 24.664}};
    private static final String[] NAMES = {"Mari","Jaan","Anna","Karl","Liis","Peeter","Kati","Toomas",
            "Eva","Mart","Laura","Andres","Maria","Rasmus","Helen","Oskar"};

    private final JdbcTemplate jdbc;

    public DatabaseSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int count = Integer.getInteger("seed.count", 120);
        Random rnd = new Random(42);
        for (String s : SPORTS)
            jdbc.update("INSERT INTO tags(name, category) VALUES (?, 'sport') ON CONFLICT DO NOTHING", s);
        List<Integer> tagIds = jdbc.queryForList("SELECT id FROM tags WHERE category='sport'", Integer.class);

        String hash = new BCryptPasswordEncoder().encode("password123");
        for (int i = 1; i <= count; i++) {
            UUID id = UUID.randomUUID();
            double[] c = CITIES[rnd.nextInt(CITIES.length)];
            int age = 18 + rnd.nextInt(40);
            int ageMin = Math.max(18, age - 3 - rnd.nextInt(8));
            int ageMax = age + 3 + rnd.nextInt(10);

            jdbc.update("INSERT INTO users(id, email, password_hash) VALUES (?,?,?)",
                    id, "user" + i + "@example.com", hash);
            jdbc.update("INSERT INTO profiles(user_id, display_name, about_me, completed) VALUES (?,?,?,true)",
                    id, NAMES[rnd.nextInt(NAMES.length)] + " " + i, "Looking for a sports buddy.");
            jdbc.update("""
                INSERT INTO bios(user_id, skill_level, goal, looking_for, env_pref, age, age_min, age_max,
                                 lat, lng, max_radius_km, availability)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,ARRAY[?]::text[])""",
                    id, 1 + rnd.nextInt(5), GOAL[rnd.nextInt(GOAL.length)], LOOKING_FOR[rnd.nextInt(LOOKING_FOR.length)], ENV[rnd.nextInt(ENV.length)],
                    age, ageMin, ageMax,
                    c[0] + (rnd.nextDouble() - 0.5) * 0.1, c[1] + (rnd.nextDouble() - 0.5) * 0.1,
                    10 + rnd.nextInt(40), AVAIL[rnd.nextInt(3)]);

            Collections.shuffle(tagIds, rnd);
            for (int t = 0; t < 1 + rnd.nextInt(4); t++)
                jdbc.update("INSERT INTO user_tags(user_id, tag_id) VALUES (?,?)", id, tagIds.get(t));
        }
    }
}