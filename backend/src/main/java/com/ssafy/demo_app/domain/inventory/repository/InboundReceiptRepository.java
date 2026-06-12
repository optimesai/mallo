package com.ssafy.demo_app.domain.inventory.repository;

import com.ssafy.demo_app.domain.inventory.entity.InboundReceipt;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InboundReceiptRepository extends JpaRepository<InboundReceipt, Integer>,
		JpaSpecificationExecutor<InboundReceipt> {
    boolean existsByItem(ItemMaster item);
    long countByItem(ItemMaster item);
    boolean existsByPartner(PartnerMaster partner);
    long countByPartner(PartnerMaster partner);
    Optional<InboundReceipt> findTopByPartnerOrderByCreatedAtDesc(PartnerMaster partner);
    List<InboundReceipt> findByPartnerOrderByInboundDateDescInboundIdDesc(PartnerMaster partner);
    void deleteByItem(ItemMaster item);
}
