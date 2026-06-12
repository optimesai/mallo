package com.ssafy.demo_app.domain.partner.repository;

import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerMasterRepository extends JpaRepository<PartnerMaster, Integer>,
        JpaSpecificationExecutor<PartnerMaster> {
    Optional<PartnerMaster> findByPartnerCode(String partnerCode);
    boolean existsByPartnerCode(String partnerCode);
    boolean existsByPartnerCodeAndPartnerIdNot(String partnerCode, Integer partnerId);
    boolean existsByBusinessNo(String businessNo);
    List<PartnerMaster> findByPartnerCodeStartingWith(String partnerCodePrefix);
    List<PartnerMaster> findByPartnerTypeOrderByPartnerIdAsc(PartnerMaster.PartnerType partnerType);
    List<PartnerMaster> findByPartnerNameContainingIgnoreCaseOrPartnerCodeContainingIgnoreCaseOrderByPartnerIdAsc(
            String partnerName,
            String partnerCode
    );
    List<PartnerMaster> findByPartnerNameContainingIgnoreCaseOrPartnerCodeContainingIgnoreCaseOrBusinessNoContainingIgnoreCaseOrderByPartnerIdAsc(
            String partnerName,
            String partnerCode,
            String businessNo
    );
}
