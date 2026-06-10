package com.ssafy.demo_app.domain.item.service;

import com.ssafy.demo_app.api.item.dto.ItemRequest;
import com.ssafy.demo_app.api.item.dto.ItemDuplicateCheckResponse;
import com.ssafy.demo_app.api.item.dto.ItemReferenceResponse;
import com.ssafy.demo_app.api.item.dto.ItemResponse;
import com.ssafy.demo_app.api.item.dto.ItemStatusUpdateRequest;
import com.ssafy.demo_app.api.item.dto.ItemUpdateRequest;
import com.ssafy.demo_app.api.item.dto.ItemUsageResponse;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.global.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ItemService {

    PageResponse<ItemResponse> getItems(
            Pageable pageable,
            ItemMaster.ItemType itemType,
            ItemMaster.ItemStatus itemStatus,
            String keyword
    );

    ItemResponse getItem(Integer itemId);

    ItemResponse createItem(ItemRequest request);

    ItemResponse updateItem(Integer itemId, ItemUpdateRequest request);

    ItemResponse updateItemStatus(Integer itemId, ItemStatusUpdateRequest request);

    ItemReferenceResponse getItemReferences(Integer itemId);

    ItemUsageResponse getItemUsages(Integer itemId);

    ItemDuplicateCheckResponse checkDuplicates(String itemName, String spec, ItemMaster.Unit unit);

    void deleteItem(Integer itemId);
}
