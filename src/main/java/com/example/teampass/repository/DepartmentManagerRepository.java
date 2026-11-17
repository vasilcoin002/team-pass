package com.example.teampass.repository;

import com.example.teampass.model.user.DepartmentManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentManagerRepository extends JpaRepository<DepartmentManager, Long> {

    Optional<DepartmentManager> findByPersonId(String personId);

}
