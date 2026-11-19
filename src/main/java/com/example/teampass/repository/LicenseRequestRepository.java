package com.example.teampass.repository;

import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.license.LicenseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseRequestRepository extends JpaRepository<LicenseRequest, Long> {

    boolean existsByUserIdAndProductIdAndStatus(
            Long userId, Long productId, LicenseRequestStatus status);

    @Query("""
        SELECT lr FROM LicenseRequest lr
        WHERE lr.user.email.value = :email
          AND lr.product.name = :productName
          AND lr.status = com.example.teampass.model.license.LicenseRequestStatus.PENDING
    """)
    Optional<LicenseRequest> findUserPendingRequest(
            @Param("email") String userEmail,
            @Param("productName") String productName
    );

    @Query("""
        SELECT lr FROM LicenseRequest lr
        WHERE lr.user.email.value = :email
        AND lr.status = com.example.teampass.model.license.LicenseRequestStatus.PENDING
    """)
    List<LicenseRequest> findAllUserPendingRequests(@Param("email") String email);

     @Query("""
        SELECT lr FROM LicenseRequest lr
        WHERE lr.user.id = :userId
        AND lr.status = com.example.teampass.model.license.LicenseRequestStatus.PENDING
    """)
    List<LicenseRequest> findAllUserPendingRequests(@Param("userId") Long userId);

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
