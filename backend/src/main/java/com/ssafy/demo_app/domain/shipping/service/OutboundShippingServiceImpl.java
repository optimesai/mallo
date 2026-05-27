package com.ssafy.demo_app.domain.shipping.service;

import com.ssafy.demo_app.api.shipping.dto.ShippingCreateRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingResponse;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.repository.ItemMasterRepository;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.partner.repository.PartnerMasterRepository;
import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
import com.ssafy.demo_app.domain.shipping.repository.OutboundShippingRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboundShippingServiceImpl implements OutboundShippingService {

    private final OutboundShippingRepository outboundShippingRepository;
    private final PartnerMasterRepository partnerMasterRepository;
    private final ItemMasterRepository itemMasterRepository;

    @Override
    @Transactional
    public ShippingResponse registerShipping(ShippingCreateRequest request) {
        if (outboundShippingRepository.existsByShippingNo(request.getShippingNo())) {
            throw new BusinessException(ErrorCode.SHIPPING_NO_DUPLICATE);
        }

        PartnerMaster partner = partnerMasterRepository.findByPartnerCode(request.getPartnerCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        ItemMaster item = itemMasterRepository.findByItemCode(request.getItemCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        OutboundShipping shipping = new OutboundShipping();
        shipping.setShippingNo(request.getShippingNo());
        shipping.setPartner(partner);
        shipping.setItem(item);
        shipping.setRequestQty(request.getRequestQty());
        shipping.setStatus(OutboundShipping.ShippingStatus.READY);

        OutboundShipping savedShipping = outboundShippingRepository.save(shipping);
        return ShippingResponse.from(savedShipping);
    }
}
