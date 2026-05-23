package com.sudh.accord.service;

import com.sudh.accord.entity.User;
import com.sudh.accord.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User updateBudget(UUID userId, BigDecimal newBudget){
        User user = userRepository.findById(userId).orElseThrow();
        user.setMonthlyBudget(newBudget);
        return userRepository.save(user);
    }
}
