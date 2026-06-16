package com.ssafy.demo_app.domain.item.service;

import com.ssafy.demo_app.api.bom.dto.BomResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.api.item.dto.ItemDuplicateCheckResponse;
import com.ssafy.demo_app.api.item.dto.ItemReferenceResponse;
import com.ssafy.demo_app.api.item.dto.ItemRequest;
import com.ssafy.demo_app.api.item.dto.ItemResponse;
import com.ssafy.demo_app.api.item.dto.ItemStatusUpdateRequest;
import com.ssafy.demo_app.api.item.dto.ItemUpdateRequest;
import com.ssafy.demo_app.api.item.dto.ItemUsageResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderExecutionSummary;
import com.ssafy.demo_app.api.production.dto.WorkOrderResponse;
import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import com.ssafy.demo_app.domain.bom.repository.BomStructureRepository;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InboundReceiptRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.repository.ItemMasterRepository;
import com.ssafy.demo_app.domain.production.repository.WorkOrderRepository;
import com.ssafy.demo_app.domain.shipping.repository.OutboundShippingRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import com.ssafy.demo_app.global.response.PageResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private static final String AUTO_CODE_PREFIX = "ITEM-";
    private static final String ITEM_CODE_PATTERN = "^[A-Za-z0-9-]+$";

    private final ItemMasterRepository itemMasterRepository;
    private final BomStructureRepository bomStructureRepository;
    private final CurrentInventoryRepository currentInventoryRepository;
    private final InboundReceiptRepository inboundReceiptRepository;
    private final InventoryTransactionHistoryRepository inventoryTransactionHistoryRepository;
    private final WorkOrderRepository workOrderRepository;
    private final OutboundShippingRepository outboundShippingRepository;

    @Override
    public PageResponse<ItemResponse> getItems(
            Pageable pageable,
            ItemMaster.ItemType itemType,
            ItemMaster.ItemStatus itemStatus,
            String keyword
    ) {
        Specification<ItemMaster> spec = buildItemSpec(itemType, itemStatus, keyword);
        Page<ItemResponse> page = itemMasterRepository.findAll(spec, pageable).map(ItemResponse::from);
        return PageResponse.from(page);
    }

    @Override
    public ItemResponse getItem(Integer itemId) {
        return ItemResponse.from(findItem(itemId));
    }

    @Override
    @Transactional
    public ItemResponse createItem(ItemRequest request) {
        ItemMaster item = new ItemMaster();
        item.setItemCode(resolveItemCode(request.getItemCode()));
        item.setItemStatus(ItemMaster.ItemStatus.ACTIVE);
        applyRequest(item, request);

        return ItemResponse.from(itemMasterRepository.save(item));
    }

    @Override
    @Transactional
    public ItemResponse updateItem(Integer itemId, ItemUpdateRequest request) {
        ItemMaster item = findItem(itemId);
        if (item.getItemType() != request.getItemType()
                && hasReferences(item)
                && !Boolean.TRUE.equals(request.getConfirmReferenceWarning())) {
            throw new BusinessException(ErrorCode.ITEM_REFERENCE_CONFIRM_REQUIRED);
        }
        applyRequest(item, request);

        return ItemResponse.from(itemMasterRepository.save(item));
    }

    @Override
    @Transactional
    public ItemResponse updateItemStatus(Integer itemId, ItemStatusUpdateRequest request) {
        ItemMaster item = findItem(itemId);
        item.setItemStatus(request.getItemStatus());
        return ItemResponse.from(itemMasterRepository.save(item));
    }

    @Override
    public ItemReferenceResponse getItemReferences(Integer itemId) {
        ItemMaster item = findItem(itemId);
        ItemReferenceResponse response = new ItemReferenceResponse();
        response.setItemId(item.getItemId());
        response.setItemCode(item.getItemCode());
        response.setBomParentCount(bomStructureRepository.countByParentItem(item));
        response.setBomChildCount(bomStructureRepository.countByChildItem(item));
        response.setInventoryCount(currentInventoryRepository.countByItem(item));
        response.setInboundCount(inboundReceiptRepository.countByItem(item));
        response.setTransactionHistoryCount(inventoryTransactionHistoryRepository.countByItem(item));
        response.setWorkOrderCount(workOrderRepository.countByItem(item));
        response.setShippingCount(outboundShippingRepository.countByItem(item));
        response.calculateFlags();
        return response;
    }

    @Override
    public ItemUsageResponse getItemUsages(Integer itemId) {
        ItemMaster item = findItem(itemId);
        ItemUsageResponse response = new ItemUsageResponse();
        response.setItemId(item.getItemId());
        response.setItemCode(item.getItemCode());
        response.setCurrentQtyTotal(currentInventoryRepository.findByItem(item).stream()
                .mapToInt(CurrentInventory::getCurrentQty)
                .sum());
        response.setAsParentBoms(bomStructureRepository.findByParentItemOrderByBomIdAsc(item).stream()
                .map(BomResponse::from)
                .toList());
        response.setAsChildBoms(bomStructureRepository.findByChildItemOrderByBomIdAsc(item).stream()
                .map(BomResponse::from)
                .toList());
        response.setWorkOrders(workOrderRepository.findByItemOrderByOrderIdDesc(item).stream()
                .map(workOrder -> WorkOrderResponse.from(workOrder, WorkOrderExecutionSummary.empty(), false, false))
                .toList());
        response.setShippingCount(outboundShippingRepository.countByItem(item));
        response.setRecentTransactions(inventoryTransactionHistoryRepository.findTop5ByItemOrderByTransactionIdDesc(item).stream()
                .map(TransactionHistoryResponse::from)
                .toList());
        return response;
    }

    @Override
    public ItemDuplicateCheckResponse checkDuplicates(String itemName, String spec, ItemMaster.Unit unit) {
        if (itemName == null || itemName.isBlank() || unit == null) {
            return new ItemDuplicateCheckResponse(List.of());
        }
        String normalizedSpec = spec == null || spec.isBlank() ? null : spec.trim();
        List<ItemResponse> items = itemMasterRepository
                .findByItemNameIgnoreCaseAndSpecAndUnit(itemName.trim(), normalizedSpec, unit)
                .stream()
                .map(ItemResponse::from)
                .toList();
        return new ItemDuplicateCheckResponse(items);
    }

    @Override
    @Transactional
    public void deleteItem(Integer itemId) {
        ItemMaster item = findItem(itemId);
        if (hasReferences(item)) {
            throw new BusinessException(ErrorCode.ITEM_HAS_REFERENCES);
        }
        itemMasterRepository.delete(item);
    }

    private ItemMaster findItem(Integer itemId) {
        return itemMasterRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }

    private String resolveItemCode(String requestedItemCode) {
        if (requestedItemCode == null || requestedItemCode.isBlank()) {
            return generateItemCode();
        }
        String itemCode = requestedItemCode.trim();
        validateItemCodeFormat(itemCode);
        validateItemCodeNotUsed(itemCode);
        return itemCode;
    }

    private void validateItemCodeFormat(String itemCode) {
        if (!itemCode.matches(ITEM_CODE_PATTERN)) {
            throw new BusinessException(ErrorCode.ITEM_CODE_INVALID);
        }
    }

    private void validateItemCodeNotUsed(String itemCode) {
        if (itemMasterRepository.existsByItemCode(itemCode)) {
            throw new BusinessException(ErrorCode.ITEM_CODE_DUPLICATE);
        }
    }

    private void applyRequest(ItemMaster item, ItemRequest request) {
        item.setItemName(request.getItemName().trim());
        item.setSpec(normalizeNullable(request.getSpec()));
        item.setUnit(request.getUnit());
        item.setItemType(request.getItemType());
        item.setSafetyStock(request.getSafetyStock());
    }

    private void applyRequest(ItemMaster item, ItemUpdateRequest request) {
        item.setItemName(request.getItemName().trim());
        item.setSpec(normalizeNullable(request.getSpec()));
        item.setUnit(request.getUnit());
        item.setItemType(request.getItemType());
        item.setSafetyStock(request.getSafetyStock());
    }

    private String generateItemCode() {
        int latestNo = itemMasterRepository
                .findByItemCodeStartingWith(AUTO_CODE_PREFIX)
                .stream()
                .map(ItemMaster::getItemCode)
                .mapToInt(this::extractAutoCodeNo)
                .max()
                .orElse(0);
        String itemCode = AUTO_CODE_PREFIX + String.format("%04d", latestNo + 1);

        validateItemCodeNotUsed(itemCode);
        return itemCode;
    }

    private int extractAutoCodeNo(String itemCode) {
        if (!itemCode.startsWith(AUTO_CODE_PREFIX)) {
            return 0;
        }

        try {
            return Integer.parseInt(itemCode.substring(AUTO_CODE_PREFIX.length()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private boolean hasReferences(ItemMaster item) {
        return bomStructureRepository.existsByParentItemOrChildItem(item, item)
                || currentInventoryRepository.existsByItem(item)
                || inboundReceiptRepository.existsByItem(item)
                || inventoryTransactionHistoryRepository.existsByItem(item)
                || workOrderRepository.existsByItem(item)
                || outboundShippingRepository.existsByItem(item);
    }

    private Specification<ItemMaster> buildItemSpec(
            ItemMaster.ItemType itemType,
            ItemMaster.ItemStatus itemStatus,
            String keyword
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (itemType != null) {
                predicates.add(cb.equal(root.get("itemType"), itemType));
            }
            if (itemStatus != null) {
                predicates.add(cb.equal(root.get("itemStatus"), itemStatus));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate idMatch = null;
                try {
                    idMatch = cb.equal(root.get("itemId"), Integer.parseInt(keyword.trim()));
                } catch (NumberFormatException ignored) {
                    // 숫자가 아닌 키워드는 코드/명칭/규격 검색만 적용한다.
                }
                Predicate codeMatch = cb.like(cb.lower(root.get("itemCode")), pattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("itemName")), pattern);
                Predicate specMatch = cb.like(cb.lower(root.get("spec")), pattern);
                Predicate typeMatch = cb.like(cb.lower(root.get("itemType").as(String.class)), pattern);
                Predicate unitMatch = cb.like(cb.lower(root.get("unit").as(String.class)), pattern);
                predicates.add(idMatch == null
                        ? cb.or(codeMatch, nameMatch, specMatch, typeMatch, unitMatch)
                        : cb.or(idMatch, codeMatch, nameMatch, specMatch, typeMatch, unitMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
