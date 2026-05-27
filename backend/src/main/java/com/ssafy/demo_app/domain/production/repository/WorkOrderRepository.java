package com.ssafy.demo_app.domain.production.repository;

import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Integer> {
}
