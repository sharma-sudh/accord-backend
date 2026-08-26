package com.sudh.accord.service;

import com.sudh.accord.entity.User;
import com.sudh.accord.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class WeeklyNarrativeScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyNarrativeScheduler.class);

    private final UserRepository userRepository;
    private final WeeklyNarrativeService weeklyNarrativeService;

    public WeeklyNarrativeScheduler(UserRepository userRepository, WeeklyNarrativeService weeklyNarrativeService) {
        this.userRepository = userRepository;
        this.weeklyNarrativeService = weeklyNarrativeService;
    }

    // Sunday 9:00 AM IST. zone pinned explicitly rather than relying on the
    // JVM's default zone, matching the rest of the app's Asia/Kolkata
    // convention (see StreakService, application.properties'
    // hibernate.jdbc.time_zone, and the -Duser.timezone JVM arg in pom.xml).
    //
    // No FCM infra exists yet (see NarrativeController's javadoc), so this
    // only writes the narrative to the DB — delivery is the Android client
    // polling GET /api/v1/narrative/latest on next app open, not a push from
    // here. Swap in an FCM send after generateForUser succeeds once that
    // infra exists; the generation/storage side doesn't need to change.
    @Scheduled(cron = "0 0 9 * * SUN", zone = "Asia/Kolkata")
    public void generateWeeklyNarratives() {
        LocalDate weekStart = WeeklyNarrativeService.currentWeekStart();
        LocalDate weekEndExclusive = WeeklyNarrativeService.currentWeekEndExclusive();

        List<User> users = userRepository.findAll();
        int generated = 0;
        int skipped = 0;
        int failed = 0;

        for (User user : users) {
            try {
                boolean created = weeklyNarrativeService.generateForUser(user.getId(), weekStart, weekEndExclusive) != null;
                if (created) {
                    generated++;
                } else {
                    skipped++; // already had a narrative for this week
                }
            } catch (Exception e) {
                // Per-user try/catch is deliberate: a single Gemini timeout,
                // rate-limit, or malformed response must not abort the batch
                // for every other user.
                failed++;
                log.error("Failed to generate weekly narrative for user {} (week starting {})",
                        user.getId(), weekStart, e);
            }
        }

        log.info("Weekly narrative batch for week starting {} complete: {} generated, {} skipped, {} failed",
                weekStart, generated, skipped, failed);
    }
}