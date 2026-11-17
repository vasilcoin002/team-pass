package com.example.teampass.repository;

import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.license.LicenseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LicenseRequestRepository extends JpaRepository<LicenseRequest, Long> {

    boolean existsByEmployeeIdAndProductIdAndStatus(
            Long employeeId, Long productId, LicenseRequestStatus status);

    @Query("""
        SELECT lr FROM LicenseRequest lr
        WHERE lr.employee.email.value = :email
        AND lr.status = com.example.teampass.model.license.LicenseRequestStatus.PENDING
    """)
    List<LicenseRequest> findAllPendingRequestsByEmployee(@Param("email") String email);

     @Query("""
        SELECT lr FROM LicenseRequest lr
        WHERE lr.employee.id = :employeeId
        AND lr.status = com.example.teampass.model.license.LicenseRequestStatus.PENDING
    """)
    List<LicenseRequest> findAllPendingRequestsByEmployee(@Param("employeeId") Long employeeId);


    @Query("""
        SELECT lr FROM LicenseRequest lr
        WHERE lr.employee.email.value = :email
        AND lr.status = com.example.teampass.model.license.LicenseRequestStatus.PENDING
        AND lr.product.id = :productId
    """)
    List<LicenseRequest> findPendingRequestsForProductByEmployee(
            @Param("productId") Long productId,
            @Param("email") String employeeEmail
    );

    @Query("""
        SELECT lr FROM LicenseRequest lr
        WHERE lr.department.id = :departmentId
        AND lr.status = com.example.teampass.model.license.LicenseRequestStatus.PENDING
    """)
    List<LicenseRequest> findAllDepartmentPendingRequests(@Param("departmentId") Long departmentId);

    @Query("""
        SELECT lr FROM LicenseRequest lr
        WHERE lr.product.id = :productId
        AND lr.status = com.example.teampass.model.license.LicenseRequestStatus.PENDING
    """)
    List<LicenseRequest> findAllProductPendingRequests(@Param("productId") Long productId);

}
