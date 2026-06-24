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
    long countByStatus(OutboundShipping.ShippingStatus status);

    @Query(value = """
            select os.shipping_id as shippingId,
                   os.shipping_no as shippingNo,
                   p.partner_code as partnerCode,
                   p.partner_name as partnerName,
                   i.item_code as itemCode,
                   i.item_name as itemName,
                   os.request_qty as requestQty,
                   os.shipped_qty as shippedQty,
                   os.shipping_type as shippingType,
                   wl.location_code as pickingLocationCode,
                   os.vehicle_no as vehicleNo,
                   os.carrier as carrier,
                   os.tracking_no as trackingNo,
                   os.estimated_delivery as estimatedDelivery,
                   os.cancel_reason as cancelReason,
                   os.status as status,
                   u.user_name as workerName,
                   os.shipped_at as shippedAt
            from outbound_shipping os
            left join partner_master p on os.partner_id = p.partner_id
            left join item_master i on os.item_id = i.item_id
            left join warehouse_location wl on os.picking_location_id = wl.location_id
            left join users u on os.worker_id = u.user_id
            where (:status is null or os.status = :status)
              and (
                :keyword is null
                or lower(coalesce(os.shipping_no, '')) like concat('%', lower(:keyword), '%')
                or lower(coalesce(i.item_code, '')) like concat('%', lower(:keyword), '%')
                or lower(coalesce(i.item_name, '')) like concat('%', lower(:keyword), '%')
                or lower(coalesce(p.partner_code, '')) like concat('%', lower(:keyword), '%')
                or lower(coalesce(p.partner_name, '')) like concat('%', lower(:keyword), '%')
              )
            order by os.created_at desc, os.shipping_id desc
            """,
            countQuery = """
                    select count(*)
                    from outbound_shipping os
                    left join partner_master p on os.partner_id = p.partner_id
                    left join item_master i on os.item_id = i.item_id
                    where (:status is null or os.status = :status)
                      and (
                        :keyword is null
                        or lower(coalesce(os.shipping_no, '')) like concat('%', lower(:keyword), '%')
                        or lower(coalesce(i.item_code, '')) like concat('%', lower(:keyword), '%')
                        or lower(coalesce(i.item_name, '')) like concat('%', lower(:keyword), '%')
                        or lower(coalesce(p.partner_code, '')) like concat('%', lower(:keyword), '%')
                        or lower(coalesce(p.partner_name, '')) like concat('%', lower(:keyword), '%')
                      )
                    """,
            nativeQuery = true)
    org.springframework.data.domain.Page<ShippingListProjection> findShippingList(
            @Param("status") String status,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );

    @Query(value = """
            select max(coalesce(os.shipped_at, os.created_at))
            from outbound_shipping os
            where os.partner_id = :partnerId
            """, nativeQuery = true)
    Optional<LocalDateTime> findLastShippingAtByPartnerId(@Param("partnerId") Integer partnerId);

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
