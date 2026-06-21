package com.ssafy.demo_app.domain.shipping.repository;

import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("""
            select p.partnerName,
                   coalesce(sum(coalesce(os.requestQty, 0) - coalesce(os.shippedQty, 0)), 0),
                   coalesce(sum(coalesce(os.shippedQty, 0)), 0),
                   count(os)
            from OutboundShipping os
            join os.partner p
            where os.createdAt >= :fromDateTime
              and os.status in (
                com.ssafy.demo_app.domain.shipping.entity.OutboundShipping.ShippingStatus.READY,
                com.ssafy.demo_app.domain.shipping.entity.OutboundShipping.ShippingStatus.PICKING,
                com.ssafy.demo_app.domain.shipping.entity.OutboundShipping.ShippingStatus.PACKING,
                com.ssafy.demo_app.domain.shipping.entity.OutboundShipping.ShippingStatus.INSPECTING,
                com.ssafy.demo_app.domain.shipping.entity.OutboundShipping.ShippingStatus.PARTIALLY_SHIPPED
              )
            group by p.partnerName
            order by coalesce(sum(coalesce(os.requestQty, 0) - coalesce(os.shippedQty, 0)), 0) desc
            """)
    List<Object[]> aggregateWaitingShippingByPartner(@Param("fromDateTime") LocalDateTime fromDateTime);

    @Query("""
            select count(os)
            from OutboundShipping os
            where os.createdAt >= :fromDateTime
              and os.status in (
                com.ssafy.demo_app.domain.shipping.entity.OutboundShipping.ShippingStatus.READY,
                com.ssafy.demo_app.domain.shipping.entity.OutboundShipping.ShippingStatus.PICKING,
                com.ssafy.demo_app.domain.shipping.entity.OutboundShipping.ShippingStatus.PACKING,
                com.ssafy.demo_app.domain.shipping.entity.OutboundShipping.ShippingStatus.INSPECTING,
                com.ssafy.demo_app.domain.shipping.entity.OutboundShipping.ShippingStatus.PARTIALLY_SHIPPED
              )
            """)
    long countWaitingShipping(@Param("fromDateTime") LocalDateTime fromDateTime);
}
