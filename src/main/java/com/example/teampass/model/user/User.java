package com.example.teampass.model.user;

import com.example.teampass.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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
}
