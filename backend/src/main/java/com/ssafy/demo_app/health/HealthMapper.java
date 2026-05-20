package com.ssafy.demo_app.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.demo_app.model.HealthCheck;

@Mapper
public interface HealthMapper {
    HealthCheck check();
}
