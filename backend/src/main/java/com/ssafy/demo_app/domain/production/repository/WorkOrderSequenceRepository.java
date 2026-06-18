package com.ssafy.demo_app.domain.production.repository;

import com.ssafy.demo_app.domain.production.entity.WorkOrderSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WorkOrderSequenceRepository extends JpaRepository<WorkOrderSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from WorkOrderSequence s where s.planDate = :planDate")
    Optional<WorkOrderSequence> findByPlanDateForUpdate(@Param("planDate") LocalDate planDate);
}
