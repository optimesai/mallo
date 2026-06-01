package com.ssafy.demo_app.domain.partner.service;

import com.ssafy.demo_app.api.partner.dto.PartnerRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerResponse;
import com.ssafy.demo_app.domain.inventory.repository.InboundReceiptRepository;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.partner.repository.PartnerMasterRepository;
import com.ssafy.demo_app.domain.shipping.repository.OutboundShippingRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerServiceImpl implements PartnerService {

    private final PartnerMasterRepository partnerMasterRepository;
    private final InboundReceiptRepository inboundReceiptRepository;
    private final OutboundShippingRepository outboundShippingRepository;

    @Override
    public List<PartnerResponse> getPartners(PartnerMaster.PartnerType partnerType, String keyword) {
        List<PartnerMaster> partners = hasKeyword(keyword)
                ? searchPartnersIncludingBusinessNo(keyword)
                : getPartnersByType(partnerType);

        return partners.stream()
                .filter(partner -> partnerType == null || partner.getPartnerType() == partnerType)
                .map(PartnerResponse::from)
                .toList();
    }

    @Override
    public List<PartnerResponse> searchPartners(String searchValue) {
        List<PartnerMaster> partners = searchPartnersByIdCodeOrName(searchValue);
        if (partners.isEmpty()) {
            throw new BusinessException(ErrorCode.PARTNER_NOT_FOUND);
        }
        return partners.stream()
                .map(PartnerResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PartnerResponse createPartner(PartnerRequest request) {
        validatePartnerCodeNotUsed(request.getPartnerCode());

        PartnerMaster partner = new PartnerMaster();
        applyRequest(partner, request);

        return PartnerResponse.from(partnerMasterRepository.save(partner));
    }

    @Override
    @Transactional
    public PartnerResponse updatePartner(Integer partnerId, PartnerRequest request) {
        PartnerMaster partner = findPartner(partnerId);
        validatePartnerCodeNotUsedByAnotherPartner(request.getPartnerCode(), partnerId);
        applyRequest(partner, request);

        return PartnerResponse.from(partnerMasterRepository.save(partner));
    }

    @Override
    @Transactional
    public void deletePartner(Integer partnerId) {
        PartnerMaster partner = findPartner(partnerId);
        if (hasReferences(partner)) {
            throw new BusinessException(ErrorCode.PARTNER_HAS_REFERENCES);
        }
        partnerMasterRepository.delete(partner);
    }

    private PartnerMaster findPartner(Integer partnerId) {
        return partnerMasterRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
    }

    private void validatePartnerCodeNotUsed(String partnerCode) {
        if (partnerMasterRepository.existsByPartnerCode(partnerCode)) {
            throw new BusinessException(ErrorCode.PARTNER_CODE_DUPLICATE);
        }
    }

    private void validatePartnerCodeNotUsedByAnotherPartner(String partnerCode, Integer partnerId) {
        if (partnerMasterRepository.existsByPartnerCodeAndPartnerIdNot(partnerCode, partnerId)) {
            throw new BusinessException(ErrorCode.PARTNER_CODE_DUPLICATE);
        }
    }

    private void applyRequest(PartnerMaster partner, PartnerRequest request) {
        partner.setPartnerCode(trimRequired(request.getPartnerCode()));
        partner.setPartnerName(trimRequired(request.getPartnerName()));
        partner.setPartnerType(request.getPartnerType());
        partner.setBusinessNo(trimToNull(request.getBusinessNo()));
        partner.setRepresentative(trimToNull(request.getRepresentative()));
        partner.setContactPhone(trimToNull(request.getContactPhone()));
    }

    private List<PartnerMaster> getPartnersByType(PartnerMaster.PartnerType partnerType) {
        return partnerType == null
                ? partnerMasterRepository.findAll(Sort.by(Sort.Direction.ASC, "partnerId"))
                : partnerMasterRepository.findByPartnerTypeOrderByPartnerIdAsc(partnerType);
    }

    private boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.isBlank();
    }

    private List<PartnerMaster> searchPartnersIncludingBusinessNo(String keyword) {
        String trimmedKeyword = keyword.trim();
        Map<Integer, PartnerMaster> matchedPartners = new LinkedHashMap<>();

        parsePartnerId(trimmedKeyword)
                .flatMap(partnerMasterRepository::findById)
                .ifPresent(partner -> matchedPartners.put(partner.getPartnerId(), partner));

        partnerMasterRepository
                .findByPartnerNameContainingIgnoreCaseOrPartnerCodeContainingIgnoreCaseOrBusinessNoContainingIgnoreCaseOrderByPartnerIdAsc(
                        trimmedKeyword,
                        trimmedKeyword,
                        trimmedKeyword
                )
                .forEach(partner -> matchedPartners.putIfAbsent(partner.getPartnerId(), partner));

        return List.copyOf(matchedPartners.values());
    }

    private List<PartnerMaster> searchPartnersByIdCodeOrName(String searchValue) {
        String trimmedValue = searchValue.trim();
        Map<Integer, PartnerMaster> matchedPartners = new LinkedHashMap<>();

        parsePartnerId(trimmedValue)
                .flatMap(partnerMasterRepository::findById)
                .ifPresent(partner -> matchedPartners.put(partner.getPartnerId(), partner));

        partnerMasterRepository
                .findByPartnerNameContainingIgnoreCaseOrPartnerCodeContainingIgnoreCaseOrderByPartnerIdAsc(
                        trimmedValue,
                        trimmedValue
                )
                .forEach(partner -> matchedPartners.putIfAbsent(partner.getPartnerId(), partner));

        return List.copyOf(matchedPartners.values());
    }

    private java.util.Optional<Integer> parsePartnerId(String keyword) {
        try {
            return java.util.Optional.of(Integer.parseInt(keyword));
        } catch (NumberFormatException exception) {
            return java.util.Optional.empty();
        }
    }

    private boolean hasReferences(PartnerMaster partner) {
        return inboundReceiptRepository.existsByPartner(partner)
                || outboundShippingRepository.existsByPartner(partner);
    }

    private String trimRequired(String value) {
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
