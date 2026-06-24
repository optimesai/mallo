package com.ssafy.demo_app.api.item;

import com.ssafy.demo_app.api.item.dto.ItemRequest;
import com.ssafy.demo_app.api.item.dto.ItemDuplicateCheckResponse;
import com.ssafy.demo_app.api.item.dto.ItemReferenceResponse;
import com.ssafy.demo_app.api.item.dto.ItemResponse;
import com.ssafy.demo_app.api.item.dto.ItemStatsResponse;
import com.ssafy.demo_app.api.item.dto.ItemStatusUpdateRequest;
import com.ssafy.demo_app.api.item.dto.ItemUpdateRequest;
import com.ssafy.demo_app.api.item.dto.ItemUsageResponse;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.service.ItemService;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ItemController implements ItemApi {

    private final ItemService itemService;

    @Override
    public ResponseEntity<ApiResponse<PageResponse<ItemResponse>>> getItems(
            Pageable pageable,
            ItemMaster.ItemType itemType,
            ItemMaster.ItemStatus itemStatus,
            String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getItems(pageable, itemType, itemStatus, keyword)));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemStatsResponse>> getItemStats() {
        return ResponseEntity.ok(ApiResponse.success(itemService.getItemStats()));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemDuplicateCheckResponse>> checkDuplicates(
            String itemName,
            String spec,
            ItemMaster.Unit unit
    ) {
        return ResponseEntity.ok(ApiResponse.success(itemService.checkDuplicates(itemName, spec, unit)));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemResponse>> getItem(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getItem(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemReferenceResponse>> getItemReferences(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getItemReferences(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemUsageResponse>> getItemUsages(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getItemUsages(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(ItemRequest request) {
        ItemResponse response = itemService.createItem(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("품목이 등록되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemResponse>> updateItem(Integer id, ItemUpdateRequest request) {
        ItemResponse response = itemService.updateItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("품목이 수정되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemResponse>> updateItemStatus(Integer id, ItemStatusUpdateRequest request) {
        ItemResponse response = itemService.updateItemStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("품목 상태가 변경되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteItem(Integer id) {
        itemService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.success("품목이 삭제되었습니다."));
    }
}
