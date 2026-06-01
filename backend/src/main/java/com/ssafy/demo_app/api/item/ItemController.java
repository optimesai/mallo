package com.ssafy.demo_app.api.item;

import com.ssafy.demo_app.api.item.dto.ItemRequest;
import com.ssafy.demo_app.api.item.dto.ItemResponse;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.service.ItemService;
import com.ssafy.demo_app.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ItemController implements ItemApi {

    private final ItemService itemService;

    @Override
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getItems(ItemMaster.ItemType itemType, String keyword) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getItems(itemType, keyword)));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemResponse>> getItem(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getItem(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(ItemRequest request) {
        ItemResponse response = itemService.createItem(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("품목이 등록되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<ItemResponse>> updateItem(Integer id, ItemRequest request) {
        ItemResponse response = itemService.updateItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("품목이 수정되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteItem(Integer id, boolean force) {
        itemService.deleteItem(id, force);
        return ResponseEntity.ok(ApiResponse.success("품목이 삭제되었습니다."));
    }
}
