package com.ssafy.demo_app.domain.ai.service.sql;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SqlExecutionService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> execute(String sql) {
        return jdbcTemplate.queryForList(sql);
    }
}
