package com.example.teampass.service;

import com.example.teampass.exception.AccessDeniedException;
import com.example.teampass.exception.InactiveUserException;
import com.example.teampass.model.user.Role;
import com.example.teampass.model.user.User;
import com.example.teampass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User getActiveUserOrThrow(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        User user = optionalUser.orElseThrow(
                () -> new IllegalArgumentException("User with id: " + userId + " not found")
        );

        if (!user.isActive()) {
            throw new InactiveUserException("User with id: "+ userId + " is not active");
        }

        return user;
    }

    @Transactional
    public User getActiveManagerOrThrow(Long managerId) {
        User manager = getActiveUserOrThrow(managerId);

        if (manager.getRole() != Role.DEPARTMENT_MANAGER) {
            throw new AccessDeniedException("DEPARTMENT_MANAGER role required to review license request");
        }

        return manager;
    }

}
