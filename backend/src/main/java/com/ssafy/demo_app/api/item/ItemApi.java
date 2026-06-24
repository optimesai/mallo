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
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Item Master API", description = "품목 마스터 CRUD 관리 API")
@RequestMapping("/api/items")
public interface ItemApi {

    @Operation(summary = "품목 목록 조회", description = "등록된 품목 마스터 목록을 페이징 조회합니다. itemType, itemStatus, keyword 필터링과 정렬이 가능합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<ItemResponse>>> getItems(
            Pageable pageable,
            @Parameter(description = "품목 분류 필터") @RequestParam(required = false) ItemMaster.ItemType itemType,
            @Parameter(description = "품목 상태 필터") @RequestParam(required = false) ItemMaster.ItemStatus itemStatus,
            @Parameter(description = "품목 ID, 품목명, 품목 코드, 규격 검색어") @RequestParam(required = false) String keyword
    );

    @Operation(summary = "품목 마스터 통계 조회", description = "전체 품목 수와 활성/비활성 품목 수를 전체 데이터 기준으로 조회합니다.")
    @GetMapping("/stats")
    ResponseEntity<ApiResponse<ItemStatsResponse>> getItemStats();

    @Operation(summary = "중복 품목 검증", description = "품목명, 규격, 단위가 같은 유사 품목을 조회합니다.")
    @GetMapping("/duplicates")
    ResponseEntity<ApiResponse<ItemDuplicateCheckResponse>> checkDuplicates(
            @RequestParam String itemName,
            @RequestParam(required = false) String spec,
            @RequestParam ItemMaster.Unit unit
    );

    @Operation(summary = "품목 단건 조회", description = "ID로 특정 품목 마스터 정보를 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<ItemResponse>> getItem(
            @Parameter(description = "품목 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "품목 참조 현황 조회", description = "품목 삭제/분류 변경 전 참조 중인 업무 데이터를 조회합니다.")
    @GetMapping("/{id}/references")
    ResponseEntity<ApiResponse<ItemReferenceResponse>> getItemReferences(
            @Parameter(description = "품목 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "품목 활용 정보 조회", description = "품목이 BOM, 재고, 작업지시, 출하, 수불에서 어떻게 활용되는지 조회합니다.")
    @GetMapping("/{id}/usages")
    ResponseEntity<ApiResponse<ItemUsageResponse>> getItemUsages(
            @Parameter(description = "품목 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "품목 등록", description = "품목명, 규격, 단위, 품목 분류, 안전 재고량을 등록합니다. 품목 코드는 선택 입력이며, 미입력 시 ITEM-0001 형식으로 자동 생성됩니다.")
    @PostMapping
    ResponseEntity<ApiResponse<ItemResponse>> createItem(
            @Valid @RequestBody ItemRequest request
    );

    @Operation(summary = "품목 수정", description = "ID에 해당하는 품목 마스터 정보를 수정합니다. 품목 코드는 수정되지 않습니다.")
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<ItemResponse>> updateItem(
            @Parameter(description = "품목 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody ItemUpdateRequest request
    );

    @Operation(summary = "품목 상태 변경", description = "품목을 활성화 또는 비활성화합니다.")
    @PatchMapping("/{id}/status")
    ResponseEntity<ApiResponse<ItemResponse>> updateItemStatus(
            @Parameter(description = "품목 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody ItemStatusUpdateRequest request
    );

    @Operation(summary = "품목 삭제", description = "ID에 해당하는 품목 마스터 정보를 삭제합니다. 참조 중인 품목은 삭제할 수 없으며 비활성화해야 합니다.")
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<Void>> deleteItem(
            @Parameter(description = "품목 ID", required = true) @PathVariable Integer id
    );
}
