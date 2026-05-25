package com.ssafy.demo_app.domain.user.mapper;

import com.ssafy.demo_app.domain.user.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {

    void insertUser(User user);

    Optional<User> findByUserId(@Param("userId") Integer userId);

    Optional<User> findByEmployeeNo(@Param("employeeNo") String employeeNo);

    boolean existsByEmployeeNo(@Param("employeeNo") String employeeNo);

    List<User> findAll();

    int updateUser(User user);

    int updateRole(@Param("userId") Integer userId, @Param("role") User.Role role);

    int deleteByUserId(@Param("userId") Integer userId);
}
