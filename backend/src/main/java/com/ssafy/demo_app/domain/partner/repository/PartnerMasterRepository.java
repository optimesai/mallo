package com.ssafy.demo_app.domain.partner.repository;

import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerMasterRepository extends JpaRepository<PartnerMaster, Integer> {
    Optional<PartnerMaster> findByPartnerCode(String partnerCode);
}
