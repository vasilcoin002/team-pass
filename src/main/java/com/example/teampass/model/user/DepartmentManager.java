package com.example.teampass.model.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "department_managers")
@Getter
@Setter
@NoArgsConstructor
public class DepartmentManager extends Employee {
    @Override
    public boolean isManager() {
        return true;
    }
}
