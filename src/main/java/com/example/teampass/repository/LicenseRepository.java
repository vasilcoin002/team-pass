package com.example.teampass.repository;

import com.example.teampass.model.license.License;
import com.example.teampass.model.license.LicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {

    long countByProductIdAndStatus(Long productId, LicenseStatus status);
    boolean existsByEmployeeIdAndProductIdAndStatus(
            Long employeeId, Long productId, LicenseStatus status);

    @Query("""
        SELECT l FROM License l
        WHERE l.employee.email.value = :email
          AND l.product.name = :productName
          AND l.status = com.example.teampass.model.license.LicenseStatus.ACTIVE
    """)
    Optional<License> findEmployeeActiveLicense(
            @Param("email") String email,
            @Param("productName") String productName
    );

    @Query("""
        SELECT l FROM License l
        WHERE l.employee.email.value = :email
          AND l.status = com.example.teampass.model.license.LicenseStatus.ACTIVE
    """)
    List<License> findEmployeeActiveLicenses(@Param("email") String employeeEmail);


    @Query("""
        SELECT l FROM License l
        WHERE l.employee.department.id = :departmentId
          AND l.status = com.example.teampass.model.license.LicenseStatus.ACTIVE
    """)
    List<License> findDepartmentActiveLicenses(@Param("departmentId") Long departmentId);

    @Query("""
        SELECT l FROM License l
        WHERE l.product.name = :productName
          AND l.status = com.example.teampass.model.license.LicenseStatus.ACTIVE
    """)
    List<License> findAllActiveLicensesByProductName(@Param("productName") String productName);
}
