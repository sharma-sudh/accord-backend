package com.sudh.accord.service;

import com.sudh.accord.entity.User;
import com.sudh.accord.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class StreakService {

    // Users are Indian university students; "today"/"yesterday" must be
    // computed in IST regardless of the server's own default zone, matching
    // spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Kolkata.
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final UserRepository userRepository;

    public StreakService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Called once per day on app open (not on task completion). Advances,
     * no-ops, or resets currentStreak based on lastCheckInDate, then persists.
     */
    public User checkIn(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDate today = LocalDate.now(ZONE);
        LocalDate lastCheckIn = user.getLastCheckInDate();

        if (lastCheckIn != null && lastCheckIn.isEqual(today)) {
            // Already checked in today — no-op.
            return user;
        } else if (lastCheckIn != null && lastCheckIn.isEqual(today.minusDays(1))) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else {
            // Never checked in, or last check-in was more than 1 day ago.
            user.setCurrentStreak(1);
        }

        user.setLastCheckInDate(today);
        return userRepository.save(user);
    }

    public User getStreak(UUID userId) {
        return userRepository.findById(userId).orElseThrow();
    }
}