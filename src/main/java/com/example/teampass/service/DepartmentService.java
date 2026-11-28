package com.example.teampass.service;

import com.example.teampass.dto.CreateDepartmentRequest;
import com.example.teampass.dto.UpdateDepartmentRequest;
import com.example.teampass.exception.AlreadyExistsException;
import com.example.teampass.model.department.Department;
import com.example.teampass.repository.DepartmentRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public Department getDepartmentOrThrow(@NonNull Long departmentId) {
        Optional<Department> optionalDepartment = departmentRepository.findById(departmentId);
        return optionalDepartment.orElseThrow(
                () -> new IllegalArgumentException("Department with id: " + departmentId + " not found")
        );
    }

    @Transactional(readOnly = true)
    public Department getDepartmentOrThrow(@NonNull String name) {
        Optional<Department> optionalDepartment = departmentRepository.findByName(name);
        return optionalDepartment.orElseThrow(
                () -> new IllegalArgumentException("Department with name: " + name + " not found")
        );
    }

    @Transactional
    public Department createDepartment(@NonNull CreateDepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new AlreadyExistsException("Department with name " + request.name() + " already exists");
        }

        Department department = Department.builder()
                .name(request.name())
                .defaultMonthlyLimit(request.defaultMonthlyLimit())
                .build();

        department = departmentRepository.save(department);

        return department;
    }

    @Transactional
    public Department updateDepartment(@NonNull Long departmentId, @NonNull UpdateDepartmentRequest request) {
        Department department = getDepartmentOrThrow(departmentId);

        if (request.isEmpty()) {
            return department;
        }

        assignCorrectArgumentsIntoDepartment(department, request);

        department = departmentRepository.save(department);
        return department;
    }

    private void assignCorrectArgumentsIntoDepartment(Department department, UpdateDepartmentRequest request) {
        if (request.name() != null && !Objects.equals(department.getName(), request.name())) {
            if (departmentRepository.existsByName(request.name())) {
                throw new AlreadyExistsException("Department with name " + request.name() + " already exists");
            }
            department.setName(request.name());
        }
        if (request.defaultMonthlyLimit() != null) department.setDefaultMonthlyLimit(request.defaultMonthlyLimit());
    }

    // todo move to orchestra class
    @Transactional
    public boolean deleteDepartment(@NonNull Long departmentId) {
        boolean departmentExists = departmentRepository.existsById(departmentId);

        if (!departmentExists) {
            throw new IllegalArgumentException("Department with id " + departmentId + " not found");
        }

        departmentRepository.deleteById(departmentId);
        return true;
    }
}
