package com.ssafy.demo_app.domain.item.service;

import com.ssafy.demo_app.domain.bom.repository.BomStructureRepository;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InboundReceiptRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
import com.ssafy.demo_app.api.item.dto.ItemRequest;
import com.ssafy.demo_app.api.item.dto.ItemResponse;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.repository.ItemMasterRepository;
import com.ssafy.demo_app.domain.production.repository.WorkOrderRepository;
import com.ssafy.demo_app.domain.shipping.repository.OutboundShippingRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemMasterRepository itemMasterRepository;
    private final BomStructureRepository bomStructureRepository;
    private final CurrentInventoryRepository currentInventoryRepository;
    private final InboundReceiptRepository inboundReceiptRepository;
    private final InventoryTransactionHistoryRepository inventoryTransactionHistoryRepository;
    private final WorkOrderRepository workOrderRepository;
    private final OutboundShippingRepository outboundShippingRepository;

    @Override
    public List<ItemResponse> getItems(ItemMaster.ItemType itemType, String keyword) {
        List<ItemMaster> items = hasKeyword(keyword)
                ? searchItems(keyword)
                : getItemsByType(itemType);

        return items.stream()
                .filter(item -> itemType == null || item.getItemType() == itemType)
                .map(ItemResponse::from)
                .toList();
    }

    @Override
    public ItemResponse getItem(Integer itemId) {
        return ItemResponse.from(findItem(itemId));
    }

    @Override
    @Transactional
    public ItemResponse createItem(ItemRequest request) {
        ItemMaster item = new ItemMaster();
        item.setItemCode(generateItemCode(request.getItemType()));
        applyRequest(item, request);

        return ItemResponse.from(itemMasterRepository.save(item));
    }

    @Override
    @Transactional
    public ItemResponse updateItem(Integer itemId, ItemRequest request) {
        ItemMaster item = findItem(itemId);
        if (item.getItemType() != request.getItemType()) {
            item.setItemCode(generateItemCode(request.getItemType()));
        }
        applyRequest(item, request);

        return ItemResponse.from(itemMasterRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(Integer itemId, boolean force) {
        ItemMaster item = findItem(itemId);
        if (hasReferences(item)) {
            if (!force) {
                throw new BusinessException(ErrorCode.ITEM_HAS_REFERENCES);
            }
            deleteReferences(item);
        }
        itemMasterRepository.delete(item);
    }

    private ItemMaster findItem(Integer itemId) {
        return itemMasterRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }

    private void validateItemCodeNotUsed(String itemCode) {
        itemMasterRepository.findByItemCode(itemCode)
                .ifPresent(item -> {
                    throw new BusinessException(ErrorCode.ITEM_CODE_DUPLICATE);
                });
    }

    private void applyRequest(ItemMaster item, ItemRequest request) {
        item.setItemName(request.getItemName());
        item.setSpec(request.getSpec());
        item.setUnit(request.getUnit());
        item.setItemType(request.getItemType());
        item.setSafetyStock(request.getSafetyStock());
    }

    private List<ItemMaster> getItemsByType(ItemMaster.ItemType itemType) {
        return itemType == null
                ? itemMasterRepository.findAll()
                : itemMasterRepository.findByItemType(itemType);
    }

    private boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.isBlank();
    }

    private List<ItemMaster> searchItems(String keyword) {
        String trimmedKeyword = keyword.trim();
        Map<Integer, ItemMaster> matchedItems = new LinkedHashMap<>();

        parseItemId(trimmedKeyword)
                .flatMap(itemMasterRepository::findById)
                .ifPresent(item -> matchedItems.put(item.getItemId(), item));

        itemMasterRepository
                .findByItemNameContainingIgnoreCaseOrItemCodeContainingIgnoreCaseOrderByItemIdAsc(
                        trimmedKeyword,
                        trimmedKeyword
                )
                .forEach(item -> matchedItems.putIfAbsent(item.getItemId(), item));

        return List.copyOf(matchedItems.values());
    }

    private java.util.Optional<Integer> parseItemId(String keyword) {
        try {
            return java.util.Optional.of(Integer.parseInt(keyword));
        } catch (NumberFormatException exception) {
            return java.util.Optional.empty();
        }
    }

    private String generateItemCode(ItemMaster.ItemType itemType) {
        String prefix = itemType.name();
        String codePrefix = prefix + "-";
        int latestNo = itemMasterRepository
                .findByItemTypeAndItemCodeStartingWith(itemType, codePrefix)
                .stream()
                .map(ItemMaster::getItemCode)
                .mapToInt(itemCode -> extractCodeNo(codePrefix, itemCode))
                .max()
                .orElse(0);
        String itemCode = codePrefix + String.format("%04d", latestNo + 1);

        validateItemCodeNotUsed(itemCode);
        return itemCode;
    }

    private int extractCodeNo(String codePrefix, String itemCode) {
        if (!itemCode.startsWith(codePrefix)) {
            return 0;
        }

        try {
            return Integer.parseInt(itemCode.substring(codePrefix.length()));
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

    private void deleteReferences(ItemMaster item) {
        inventoryTransactionHistoryRepository.deleteByItem(item);
        outboundShippingRepository.deleteByItem(item);
        currentInventoryRepository.deleteByItem(item);
        inboundReceiptRepository.deleteByItem(item);
        workOrderRepository.deleteByItem(item);
        bomStructureRepository.deleteByParentItemOrChildItem(item, item);
    }
}
