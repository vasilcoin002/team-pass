package com.example.teampass.service;

import com.example.teampass.dto.CreateUserRequest;
import com.example.teampass.exception.AccessDeniedException;
import com.example.teampass.exception.AlreadyExistsException;
import com.example.teampass.exception.InactiveUserException;
import com.example.teampass.model.department.Department;
import com.example.teampass.model.user.Email;
import com.example.teampass.model.user.Role;
import com.example.teampass.model.user.User;
import com.example.teampass.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentService departmentService;


    @Transactional(readOnly = true)
    public User getActiveUserOrThrow(@NonNull Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        User user = optionalUser.orElseThrow(
                () -> new IllegalArgumentException("User with id: " + userId + " not found")
        );

        if (!user.isActive()) {
            throw new InactiveUserException("User with id: "+ userId + " is not active");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public User getActiveManagerOrThrow(@NonNull Long managerId) {
        User manager = getActiveUserOrThrow(managerId);

        if (manager.getRole() != Role.DEPARTMENT_MANAGER) {
            throw new AccessDeniedException("DEPARTMENT_MANAGER role required to review license request");
        }

        return manager;
    }

//    @Transactional
//    public User createUser(@NonNull CreateUserRequest request) {
//        if (userRepository.existsByEmailValue(request.email())) {
//            throw new AlreadyExistsException("User with email " + request.name() + " already exists");
//        }
//
//        if (userRepository.existsByPersonId(request.personId())) {
//            throw new AlreadyExistsException("User with person id " + request.personId() + " already exists");
//        }
//
//        Department department = departmentService.getDepartmentOrThrow(request.departmentId());
//
//        // todo secure password
//        User user = User.builder()
//                .email(new Email(request.email()))
//                .passwordHash("")
//                .name(request.name())
//                .surname(request.surname())
//                .personId(request.personId())
//                .description(request.description())
//                .role(Role.EMPLOYEE)
//                .department(department)
//        .build();
//
//        user = userRepository.save(user);
//        return user;
//    }
}
