package com.example.teampass.model.user;

import com.example.teampass.model.BaseEntity;
import com.example.teampass.model.department.Department;
import com.example.teampass.model.license.License;
import com.example.teampass.model.license.LicenseRequest;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_users")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @OneToMany(mappedBy = "user")
    private List<License> licenses;
    @OneToMany(mappedBy = "user")
    private List<LicenseRequest> licenseRequests;


    @Builder
    public User(Email email, String passwordHash,
                String name, String surname,
                String personId, String description,
                Role role, Department department
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.surname = surname;
        this.personId = personId;
        this.description = description;
        this.department = department;

        this.role = role != null ? role : Role.EMPLOYEE;
        this.active = true;
        this.licenses = new ArrayList<>();
        this.licenseRequests = new ArrayList<>();
    }
}
