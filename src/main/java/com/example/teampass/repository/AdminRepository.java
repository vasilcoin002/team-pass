package com.example.teampass.repository;

import com.example.teampass.model.user.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {

}
