package com.example.teampass.model.user;

import com.example.teampass.model.department.Department;
import com.example.teampass.model.license.License;
import com.example.teampass.model.license.LicenseRequest;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee extends User {
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

    @OneToMany(mappedBy = "employee")
    private List<License> licenses;
    @OneToMany(mappedBy = "employee")
    private List<LicenseRequest> licenseRequests;

    public boolean isManager() {
        return false;
    }
}
