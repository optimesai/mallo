package com.ssafy.demo_app.domain.user.repository;

import com.ssafy.demo_app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmployeeNo(String employeeNo);

    boolean existsByEmployeeNo(String employeeNo);

    long countByRole(User.Role role);
}
