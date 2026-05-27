package com.ssafy.demo_app.domain.shipping.repository;

import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboundShippingRepository extends JpaRepository<OutboundShipping, Integer> {
}
