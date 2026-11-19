package com.example.teampass.model.user;

import com.example.teampass.model.BaseEntity;
import com.example.teampass.model.department.Department;
import com.example.teampass.model.license.License;
import com.example.teampass.model.license.LicenseRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "app_users")
@Inheritance(strategy = InheritanceType.JOINED) // to ensure department_id column is NOT NULL
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Embedded private Email email;
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String surname;
    @Column(nullable = false, unique = true, updatable = false)
    private String personId;
    private String description;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @OneToMany(mappedBy = "user")
    private List<License> licenses;
    @OneToMany(mappedBy = "user")
    private List<LicenseRequest> licenseRequests;
}
