package com.ssafy.demo_app.domain.shipping.repository;

import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboundShippingRepository extends JpaRepository<OutboundShipping, Integer>,
		JpaSpecificationExecutor<OutboundShipping> {
    boolean existsByShippingNo(String shippingNo);
    boolean existsByItem(ItemMaster item);
    long countByItem(ItemMaster item);
    boolean existsByPartner(PartnerMaster partner);
    long countByPartner(PartnerMaster partner);
    Optional<OutboundShipping> findTopByPartnerOrderByCreatedAtDesc(PartnerMaster partner);
    List<OutboundShipping> findByPartnerOrderByCreatedAtDescShippingIdDesc(PartnerMaster partner);
    void deleteByItem(ItemMaster item);
}
