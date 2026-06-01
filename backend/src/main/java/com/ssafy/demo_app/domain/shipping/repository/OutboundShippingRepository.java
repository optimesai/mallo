package com.ssafy.demo_app.domain.shipping.repository;

import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboundShippingRepository extends JpaRepository<OutboundShipping, Integer> {
    boolean existsByShippingNo(String shippingNo);
    boolean existsByItem(ItemMaster item);
    boolean existsByPartner(PartnerMaster partner);
    void deleteByItem(ItemMaster item);
}
