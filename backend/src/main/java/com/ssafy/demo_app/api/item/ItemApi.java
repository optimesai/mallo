package com.ssafy.demo_app.api.item;

import com.ssafy.demo_app.api.item.dto.ItemRequest;
import com.ssafy.demo_app.api.item.dto.ItemResponse;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Item Master API", description = "품목 마스터 CRUD 관리 API")
@RequestMapping("/api/items")
public interface ItemApi {

    @Operation(summary = "품목 목록 조회", description = "등록된 품목 마스터 목록을 조회합니다. itemType으로 RAW, HALF, FG 필터링이 가능하고, keyword로 품목 ID, 품목명, 품목 코드를 검색할 수 있습니다.")
    @GetMapping
    ResponseEntity<ApiResponse<List<ItemResponse>>> getItems(
            @Parameter(description = "품목 분류 필터") @RequestParam(required = false) ItemMaster.ItemType itemType,
            @Parameter(description = "품목 ID, 품목명, 품목 코드 검색어") @RequestParam(required = false) String keyword
    );

    @Operation(summary = "품목 단건 조회", description = "ID로 특정 품목 마스터 정보를 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<ItemResponse>> getItem(
            @Parameter(description = "품목 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "품목 등록", description = "품목명, 규격, 단위, 품목 분류, 안전 재고량을 등록합니다. 품목 코드는 품목 분류 기준으로 자동 생성됩니다.")
    @PostMapping
    ResponseEntity<ApiResponse<ItemResponse>> createItem(
            @Valid @RequestBody ItemRequest request
    );

    @Operation(summary = "품목 수정", description = "ID에 해당하는 품목 마스터 정보를 수정합니다.")
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<ItemResponse>> updateItem(
            @Parameter(description = "품목 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody ItemRequest request
    );

    @Operation(summary = "품목 삭제", description = "ID에 해당하는 품목 마스터 정보를 삭제합니다. 다른 기능에서 참조 중이면 기본 삭제는 차단되며, 확인 후 force=true로 재요청하면 관련 참조 데이터와 함께 삭제합니다.")
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<Void>> deleteItem(
            @Parameter(description = "품목 ID", required = true) @PathVariable Integer id,
            @Parameter(description = "참조 데이터가 있어도 강제 삭제할지 여부") @RequestParam(defaultValue = "false") boolean force
    );
}
