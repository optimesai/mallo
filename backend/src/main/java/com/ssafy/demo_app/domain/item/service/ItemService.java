package com.ssafy.demo_app.domain.item.service;

import com.ssafy.demo_app.api.item.dto.ItemRequest;
import com.ssafy.demo_app.api.item.dto.ItemResponse;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;

import java.util.List;

public interface ItemService {

    List<ItemResponse> getItems(ItemMaster.ItemType itemType, String keyword);

    ItemResponse getItem(Integer itemId);

    ItemResponse createItem(ItemRequest request);

    ItemResponse updateItem(Integer itemId, ItemRequest request);

    void deleteItem(Integer itemId, boolean force);
}
