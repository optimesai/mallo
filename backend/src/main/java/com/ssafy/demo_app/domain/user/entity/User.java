package com.ssafy.demo_app.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.ssafy.demo_app.global.common.BaseTimeEntity;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "employee_no", nullable = false, unique = true)
    private String employeeNo;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "department", nullable = false)
    private String department;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        WORKER,
        MANAGER,
        ADMIN
    }
}
