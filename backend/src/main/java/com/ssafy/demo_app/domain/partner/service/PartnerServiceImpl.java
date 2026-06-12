package com.ssafy.demo_app.domain.partner.service;

import com.ssafy.demo_app.api.partner.dto.PartnerRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerSuppliedItemResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerUsageResponse;
import com.ssafy.demo_app.domain.inventory.entity.InboundReceipt;
import com.ssafy.demo_app.domain.inventory.repository.InboundReceiptRepository;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.partner.repository.PartnerMasterRepository;
import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
import com.ssafy.demo_app.domain.shipping.repository.OutboundShippingRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import com.ssafy.demo_app.global.response.PageResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerServiceImpl implements PartnerService {

    private final PartnerMasterRepository partnerMasterRepository;
    private final InboundReceiptRepository inboundReceiptRepository;
    private final OutboundShippingRepository outboundShippingRepository;

    @Override
    public PageResponse<PartnerResponse> getPartners(Pageable pageable, PartnerMaster.PartnerType partnerType,
                                                      PartnerMaster.PartnerStatus partnerStatus, Boolean hasBusinessNo,
                                                      String keyword) {
        Specification<PartnerMaster> spec = buildPartnerSpec(partnerType, partnerStatus, hasBusinessNo, keyword);
        if (isUsageCountSort(pageable)) {
            return PageResponse.from(buildUsageCountPage(spec, pageable));
        }
        Page<PartnerMaster> page = partnerMasterRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    public List<PartnerResponse> searchPartners(String searchValue) {
        List<PartnerMaster> partners = searchPartnersByIdCodeOrName(searchValue);
        if (partners.isEmpty()) {
            throw new BusinessException(ErrorCode.PARTNER_NOT_FOUND);
        }
        return partners.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public boolean existsByPartnerCode(String partnerCode) {
        return partnerCode != null && partnerMasterRepository.existsByPartnerCode(partnerCode.trim());
    }

    @Override
    @Transactional
    public PartnerResponse createPartner(PartnerRequest request) {
        validatePartnerCodeNotUsed(request.getPartnerCode());

        PartnerMaster partner = new PartnerMaster();
        applyRequest(partner, request);
        partner.setPartnerStatus(PartnerMaster.PartnerStatus.ACTIVE);

        return toResponse(partnerMasterRepository.save(partner));
    }

    @Override
    @Transactional
    public PartnerResponse updatePartner(Integer partnerId, PartnerRequest request) {
        PartnerMaster partner = findPartner(partnerId);
        validatePartnerCodeNotUsedByAnotherPartner(request.getPartnerCode(), partnerId);
        applyRequest(partner, request);

        return toResponse(partnerMasterRepository.save(partner));
    }

    @Override
    @Transactional
    public PartnerResponse updatePartnerStatus(Integer partnerId, PartnerMaster.PartnerStatus partnerStatus) {
        PartnerMaster partner = findPartner(partnerId);
        partner.setPartnerStatus(partnerStatus);
        return toResponse(partnerMasterRepository.save(partner));
    }

    @Override
    public PartnerUsageResponse getPartnerUsage(Integer partnerId) {
        PartnerMaster partner = findPartner(partnerId);
        return toUsageResponse(partner);
    }

    @Override
    public List<PartnerSuppliedItemResponse> getSuppliedItems(Integer partnerId) {
        PartnerMaster partner = findPartner(partnerId);
        if (partner.getPartnerType() != PartnerMaster.PartnerType.SUPPLIER) {
            return List.of();
        }

        Map<Integer, PartnerSuppliedItemResponse> suppliedItems = new LinkedHashMap<>();
        for (InboundReceipt receipt : inboundReceiptRepository.findByPartnerOrderByInboundDateDescInboundIdDesc(partner)) {
            ItemMaster item = receipt.getItem();
            PartnerSuppliedItemResponse response = suppliedItems.computeIfAbsent(item.getItemId(), key -> {
                PartnerSuppliedItemResponse created = new PartnerSuppliedItemResponse();
                created.setItemCode(item.getItemCode());
                created.setItemName(item.getItemName());
                created.setItemType(item.getItemType());
                created.setUnit(item.getUnit());
                created.setLastInboundDate(receipt.getInboundDate());
                return created;
            });
            response.setTotalInboundQty(response.getTotalInboundQty() + receipt.getInboundQty());
            response.setInboundCount(response.getInboundCount() + 1);
            LocalDate lastInboundDate = response.getLastInboundDate();
            if (lastInboundDate == null || receipt.getInboundDate().isAfter(lastInboundDate)) {
                response.setLastInboundDate(receipt.getInboundDate());
            }
        }
        return suppliedItems.values().stream()
                .sorted(Comparator.comparing(PartnerSuppliedItemResponse::getLastInboundDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
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
        if (partnerMasterRepository.existsByPartnerCode(partnerCode.trim())) {
            throw new BusinessException(ErrorCode.PARTNER_CODE_DUPLICATE);
        }
    }

    private void validatePartnerCodeNotUsedByAnotherPartner(String partnerCode, Integer partnerId) {
        if (partnerMasterRepository.existsByPartnerCodeAndPartnerIdNot(partnerCode.trim(), partnerId)) {
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

    private Specification<PartnerMaster> buildPartnerSpec(PartnerMaster.PartnerType partnerType,
                                                          PartnerMaster.PartnerStatus partnerStatus,
                                                          Boolean hasBusinessNo,
                                                          String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (partnerType != null) {
                predicates.add(cb.equal(root.get("partnerType"), partnerType));
            }
            if (partnerStatus != null) {
                predicates.add(cb.equal(root.get("partnerStatus"), partnerStatus));
            }
            if (hasBusinessNo != null) {
                if (hasBusinessNo) {
                    predicates.add(cb.and(
                            cb.isNotNull(root.get("businessNo")),
                            cb.notEqual(cb.trim(root.get("businessNo")), "")
                    ));
                } else {
                    predicates.add(cb.or(
                            cb.isNull(root.get("businessNo")),
                            cb.equal(cb.trim(root.get("businessNo")), "")
                    ));
                }
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                List<Predicate> keywordPredicates = new ArrayList<>();
                parsePartnerId(keyword.trim())
                        .ifPresent(partnerId -> keywordPredicates.add(cb.equal(root.get("partnerId"), partnerId)));
                keywordPredicates.add(cb.like(cb.lower(root.get("partnerCode")), pattern));
                keywordPredicates.add(cb.like(cb.lower(root.get("partnerName")), pattern));
                keywordPredicates.add(cb.like(cb.lower(root.get("businessNo")), pattern));
                predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean isUsageCountSort(Pageable pageable) {
        return pageable.getSort().stream().anyMatch(order -> "usageCount".equals(order.getProperty()));
    }

    private Page<PartnerResponse> buildUsageCountPage(Specification<PartnerMaster> spec, Pageable pageable) {
        Sort.Order usageOrder = pageable.getSort().getOrderFor("usageCount");
        boolean descending = usageOrder == null || usageOrder.isDescending();
        Comparator<PartnerResponse> comparator = Comparator.comparingLong(PartnerResponse::getUsageCount);
        if (descending) {
            comparator = comparator.reversed();
        }

        List<PartnerResponse> responses = partnerMasterRepository.findAll(spec).stream()
                .map(this::toResponse)
                .sorted(comparator.thenComparing(PartnerResponse::getPartnerId))
                .toList();
        int start = Math.min((int) pageable.getOffset(), responses.size());
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        List<PartnerResponse> content = responses.subList(start, end);
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        return new PageImpl<>(content, pageRequest, responses.size());
    }

    private PartnerResponse toResponse(PartnerMaster partner) {
        PartnerResponse response = PartnerResponse.from(partner);
        long inboundCount = inboundReceiptRepository.countByPartner(partner);
        long shippingCount = outboundShippingRepository.countByPartner(partner);
        response.setInboundCount(inboundCount);
        response.setShippingCount(shippingCount);
        response.setUsageCount(inboundCount + shippingCount);
        return response;
    }

    private PartnerUsageResponse toUsageResponse(PartnerMaster partner) {
        long inboundCount = inboundReceiptRepository.countByPartner(partner);
        long shippingCount = outboundShippingRepository.countByPartner(partner);
        LocalDateTime lastInboundAt = inboundReceiptRepository.findTopByPartnerOrderByCreatedAtDesc(partner)
                .map(InboundReceipt::getCreatedAt)
                .orElse(null);
        LocalDateTime lastShippingAt = outboundShippingRepository.findTopByPartnerOrderByCreatedAtDesc(partner)
                .map(OutboundShipping::getCreatedAt)
                .orElse(null);

        PartnerUsageResponse response = new PartnerUsageResponse();
        response.setPartnerId(partner.getPartnerId());
        response.setInboundCount(inboundCount);
        response.setShippingCount(shippingCount);
        response.setLastInboundAt(lastInboundAt);
        response.setLastShippingAt(lastShippingAt);
        response.setLastUsedAt(maxDateTime(lastInboundAt, lastShippingAt));
        response.setCanDelete(inboundCount == 0 && shippingCount == 0);
        if (!response.isCanDelete()) {
            response.setDeleteBlockedReason("입고 또는 출하 이력에서 참조 중인 거래처입니다. 삭제 대신 비활성화를 사용하세요.");
        }
        return response;
    }

    private LocalDateTime maxDateTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}
