package com.ssafy.demo_app.global.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.ssafy.**.mapper")
public class MyBatisConfig {
}
