package com.ssafy.demo_app.domain.ai.repository;

import com.ssafy.demo_app.domain.ai.entity.AiQueryHistory;
import com.ssafy.demo_app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiQueryHistoryRepository extends JpaRepository<AiQueryHistory, Integer> {

    List<AiQueryHistory> findTop10ByWorkerOrderByCreatedAtDesc(User worker);
}
