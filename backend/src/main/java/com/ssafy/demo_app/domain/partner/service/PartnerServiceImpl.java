package com.ssafy.demo_app.domain.partner.service;

import com.ssafy.demo_app.api.partner.dto.PartnerRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerShippedItemResponse;
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

    private static final String SUPPLIER_CODE_PREFIX = "SUP-";
    private static final String CUSTOMER_CODE_PREFIX = "CUS-";

    private final PartnerMasterRepository partnerMasterRepository;
    private final InboundReceiptRepository inboundReceiptRepository;
    private final OutboundShippingRepository outboundShippingRepository;

    @Override
    public PageResponse<PartnerResponse> getPartners(Pageable pageable, PartnerMaster.PartnerType partnerType,
                                                      PartnerMaster.PartnerStatus partnerStatus, Boolean hasBusinessNo,
                                                      String keyword) {
        Specification<PartnerMaster> spec = buildPartnerSpec(partnerType, partnerStatus, hasBusinessNo, keyword);
        if (isAggregateSort(pageable)) {
            return PageResponse.from(buildAggregateSortPage(spec, pageable));
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
        PartnerMaster partner = new PartnerMaster();
        applyRequest(partner, request, resolvePartnerCode(request.getPartnerCode(), request.getPartnerType()));
        partner.setPartnerStatus(PartnerMaster.PartnerStatus.ACTIVE);

        return toResponse(partnerMasterRepository.save(partner));
    }

    @Override
    @Transactional
    public PartnerResponse updatePartner(Integer partnerId, PartnerRequest request) {
        PartnerMaster partner = findPartner(partnerId);
        applyUpdateRequest(partner, request);

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
    public List<PartnerShippedItemResponse> getShippedItems(Integer partnerId) {
        PartnerMaster partner = findPartner(partnerId);
        if (partner.getPartnerType() != PartnerMaster.PartnerType.CUSTOMER) {
            return List.of();
        }

        Map<Integer, PartnerShippedItemResponse> shippedItems = new LinkedHashMap<>();
        for (OutboundShipping shipping : outboundShippingRepository.findByPartnerOrderByCreatedAtDescShippingIdDesc(partner)) {
            ItemMaster item = shipping.getItem();
            if (item == null || item.getItemId() == null) {
                continue;
            }
            LocalDateTime shippingAt = getShippingReferenceAt(shipping);
            PartnerShippedItemResponse response = shippedItems.computeIfAbsent(item.getItemId(), key -> {
                PartnerShippedItemResponse created = new PartnerShippedItemResponse();
                created.setItemCode(item.getItemCode());
                created.setItemName(item.getItemName());
                created.setItemType(item.getItemType());
                created.setUnit(item.getUnit());
                created.setLastShippingAt(shippingAt);
                return created;
            });
            response.setTotalShippingQty(response.getTotalShippingQty() + getShippingQty(shipping));
            response.setShippingCount(response.getShippingCount() + 1);
            LocalDateTime lastShippingAt = response.getLastShippingAt();
            if (lastShippingAt == null || (shippingAt != null && shippingAt.isAfter(lastShippingAt))) {
                response.setLastShippingAt(shippingAt);
            }
        }
        return shippedItems.values().stream()
                .sorted(Comparator.comparing(PartnerShippedItemResponse::getLastShippingAt,
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

    private String resolvePartnerCode(String requestedPartnerCode, PartnerMaster.PartnerType partnerType) {
        String prefix = getPartnerCodePrefix(partnerType);
        if (requestedPartnerCode == null || requestedPartnerCode.isBlank() || requestedPartnerCode.trim().equals(prefix)) {
            return generatePartnerCode(prefix);
        }
        String partnerCode = requestedPartnerCode.trim();
        validatePartnerCodePrefix(partnerCode, partnerType);
        validatePartnerCodeNotUsed(partnerCode);
        return partnerCode;
    }

    private void validatePartnerCodePrefix(String partnerCode, PartnerMaster.PartnerType partnerType) {
        String prefix = getPartnerCodePrefix(partnerType);
        if (partnerCode == null || !partnerCode.trim().startsWith(prefix)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void applyRequest(PartnerMaster partner, PartnerRequest request, String partnerCode) {
        partner.setPartnerCode(partnerCode);
        partner.setPartnerName(trimRequired(request.getPartnerName()));
        partner.setPartnerType(request.getPartnerType());
        partner.setBusinessNo(trimToNull(request.getBusinessNo()));
        partner.setRepresentative(trimToNull(request.getRepresentative()));
        partner.setContactPhone(trimToNull(request.getContactPhone()));
        partner.setContactEmail(trimToNull(request.getContactEmail()));
        partner.setNote(trimToNull(request.getNote()));
    }

    private String getPartnerCodePrefix(PartnerMaster.PartnerType partnerType) {
        return partnerType == PartnerMaster.PartnerType.SUPPLIER ? SUPPLIER_CODE_PREFIX : CUSTOMER_CODE_PREFIX;
    }

    private String generatePartnerCode(String prefix) {
        int latestNo = partnerMasterRepository
                .findByPartnerCodeStartingWith(prefix)
                .stream()
                .map(PartnerMaster::getPartnerCode)
                .mapToInt(code -> extractPartnerCodeNo(code, prefix))
                .max()
                .orElse(0);
        String partnerCode = prefix + String.format("%04d", latestNo + 1);

        validatePartnerCodeNotUsed(partnerCode);
        return partnerCode;
    }

    private int extractPartnerCodeNo(String partnerCode, String prefix) {
        if (partnerCode == null || !partnerCode.startsWith(prefix)) {
            return 0;
        }

        try {
            return Integer.parseInt(partnerCode.substring(prefix.length()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void applyUpdateRequest(PartnerMaster partner, PartnerRequest request) {
        partner.setPartnerName(trimRequired(request.getPartnerName()));
        partner.setBusinessNo(trimToNull(request.getBusinessNo()));
        partner.setRepresentative(trimToNull(request.getRepresentative()));
        partner.setContactPhone(trimToNull(request.getContactPhone()));
        partner.setContactEmail(trimToNull(request.getContactEmail()));
        partner.setNote(trimToNull(request.getNote()));
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
                keywordPredicates.add(cb.like(cb.lower(root.get("representative")), pattern));
                keywordPredicates.add(cb.like(cb.lower(root.get("contactPhone")), pattern));
                keywordPredicates.add(cb.like(cb.lower(root.get("contactEmail")), pattern));
                keywordPredicates.add(cb.like(cb.lower(root.get("note")), pattern));
                predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean isAggregateSort(Pageable pageable) {
        return pageable.getSort().stream()
                .anyMatch(order -> "usageCount".equals(order.getProperty()) || "lastUsedAt".equals(order.getProperty()));
    }

    private Page<PartnerResponse> buildAggregateSortPage(Specification<PartnerMaster> spec, Pageable pageable) {
        Sort.Order lastUsedOrder = pageable.getSort().getOrderFor("lastUsedAt");
        Sort.Order usageOrder = pageable.getSort().getOrderFor("usageCount");
        boolean sortByLastUsedAt = lastUsedOrder != null;
        boolean descending = sortByLastUsedAt ? lastUsedOrder.isDescending() : usageOrder == null || usageOrder.isDescending();
        Comparator<PartnerResponse> comparator = sortByLastUsedAt
                ? Comparator.comparing(PartnerResponse::getLastUsedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparingLong(PartnerResponse::getUsageCount);
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
        LocalDateTime lastInboundAt = inboundReceiptRepository.findTopByPartnerOrderByCreatedAtDesc(partner)
                .map(InboundReceipt::getCreatedAt)
                .orElse(null);
        LocalDateTime lastShippingAt = outboundShippingRepository.findTopByPartnerOrderByCreatedAtDesc(partner)
                .map(this::getShippingReferenceAt)
                .orElse(null);
        response.setLastUsedAt(maxDateTime(lastInboundAt, lastShippingAt));
        return response;
    }

    private PartnerUsageResponse toUsageResponse(PartnerMaster partner) {
        long inboundCount = inboundReceiptRepository.countByPartner(partner);
        long shippingCount = outboundShippingRepository.countByPartner(partner);
        LocalDateTime lastInboundAt = inboundReceiptRepository.findTopByPartnerOrderByCreatedAtDesc(partner)
                .map(InboundReceipt::getCreatedAt)
                .orElse(null);
        LocalDateTime lastShippingAt = outboundShippingRepository.findTopByPartnerOrderByCreatedAtDesc(partner)
                .map(this::getShippingReferenceAt)
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

    private LocalDateTime getShippingReferenceAt(OutboundShipping shipping) {
        if (shipping.getShippedAt() != null) {
            return shipping.getShippedAt();
        }
        return shipping.getCreatedAt();
    }

    private int getShippingQty(OutboundShipping shipping) {
        if (shipping.getRequestQty() == null) {
            return 0;
        }
        return shipping.getRequestQty();
    }
}
