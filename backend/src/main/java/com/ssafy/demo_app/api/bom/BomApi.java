package com.ssafy.demo_app.api.bom;

import com.ssafy.demo_app.api.bom.dto.BomRequest;
import com.ssafy.demo_app.api.bom.dto.BomResponse;
import com.ssafy.demo_app.api.bom.dto.BomReverseResponse;
import com.ssafy.demo_app.api.bom.dto.BomReverseTreeResponse;
import com.ssafy.demo_app.api.bom.dto.BomTreeResponse;
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

@Tag(name = "BOM API", description = "제품별 BOM 부품명세서 관리 및 역조회 API")
@RequestMapping("/api/boms")
public interface BomApi {

    @Operation(summary = "BOM 목록 조회", description = "BOM 목록을 조회합니다. parentKeyword, childKeyword, bomVersion 필터링이 가능합니다. keyword는 품목 ID, 품목명, 품목 코드를 검색합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<List<BomResponse>>> getBoms(
            @Parameter(description = "부모 품목 ID, 품목명, 품목 코드 검색어") @RequestParam(required = false) String parentKeyword,
            @Parameter(description = "자식 품목 ID, 품목명, 품목 코드 검색어") @RequestParam(required = false) String childKeyword,
            @Parameter(description = "BOM 버전") @RequestParam(required = false) String bomVersion
    );

    @Operation(
            summary = "BOM 단건 상세 조회",
            description = "BOM 목록에서 선택한 row의 bomId로 상세 정보를 조회하는 보조 API입니다. 사용자가 품목명으로 직접 검색하는 용도는 BOM 목록/부모/자식 조회 API를 사용합니다."
    )
    @GetMapping("/details/{bomId}")
    ResponseEntity<ApiResponse<BomResponse>> getBom(
            @Parameter(description = "BOM ID", required = true) @PathVariable Integer bomId
    );

    @Operation(summary = "BOM 등록", description = "부모 품목, 자식 품목, 소요량, 버전을 등록합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<BomResponse>> createBom(
            @Valid @RequestBody BomRequest request
    );

    @Operation(summary = "BOM 수정", description = "BOM ID에 해당하는 구성 정보를 수정합니다.")
    @PutMapping("/{bomId}")
    ResponseEntity<ApiResponse<BomResponse>> updateBom(
            @Parameter(description = "BOM ID", required = true) @PathVariable Integer bomId,
            @Valid @RequestBody BomRequest request
    );

    @Operation(summary = "BOM 삭제", description = "BOM ID에 해당하는 구성 정보를 하드 삭제합니다.")
    @DeleteMapping("/{bomId}")
    ResponseEntity<ApiResponse<Void>> deleteBom(
            @Parameter(description = "BOM ID", required = true) @PathVariable Integer bomId
    );

    @Operation(summary = "부모 품목 기준 BOM 조회", description = "부모 품목 ID, 품목명, 품목 코드 중 하나로 직접 하위 BOM 목록을 조회합니다. bomVersion은 /api/boms/parents/versions에서 받은 목록 중 선택한 값을 전달합니다.")
    @GetMapping("/parents")
    ResponseEntity<ApiResponse<List<BomResponse>>> getBomsByParentKeyword(
            @Parameter(description = "부모 품목 ID, 품목명, 품목 코드 검색어", required = true) @RequestParam String keyword,
            @Parameter(description = "BOM 버전") @RequestParam(required = false) String bomVersion
    );

    @Operation(summary = "BOM 하향 트리 조회", description = "부모 품목 ID, 품목명, 품목 코드 중 하나로 원자재까지 내려가는 계층형 BOM을 조회합니다. bomVersion은 /api/boms/parents/versions에서 받은 목록 중 선택한 값을 전달합니다.")
    @GetMapping("/parents/tree")
    ResponseEntity<ApiResponse<List<BomTreeResponse>>> getBomTreesByParentKeyword(
            @Parameter(description = "부모 품목 ID, 품목명, 품목 코드 검색어", required = true) @RequestParam String keyword,
            @Parameter(description = "BOM 버전") @RequestParam(required = false) String bomVersion
    );

    @Operation(summary = "부모 품목 기준 BOM 버전 목록 조회", description = "부모 품목 ID, 품목명, 품목 코드 중 하나로 등록된 BOM 버전 목록을 조회합니다. 프론트에서 부모 기준 BOM 조회/하향 트리 조회의 드롭다운 선택지로 사용합니다.")
    @GetMapping("/parents/versions")
    ResponseEntity<ApiResponse<List<String>>> getBomVersionsByParentKeyword(
            @Parameter(description = "부모 품목 ID, 품목명, 품목 코드 검색어", required = true) @RequestParam String keyword
    );

    @Operation(summary = "자식 품목 기준 BOM 역조회", description = "자식 품목 ID, 품목명, 품목 코드 중 하나로 해당 품목이 사용되는 상위 품목 목록을 조회합니다. bomVersion은 /api/boms/children/versions에서 받은 목록 중 선택한 값을 전달합니다.")
    @GetMapping("/children/parents")
    ResponseEntity<ApiResponse<List<BomReverseResponse>>> getParentsByChildKeyword(
            @Parameter(description = "자식 품목 ID, 품목명, 품목 코드 검색어", required = true) @RequestParam String keyword,
            @Parameter(description = "BOM 버전") @RequestParam(required = false) String bomVersion
    );

    @Operation(summary = "자식 품목 기준 BOM 버전 목록 조회", description = "자식 품목 ID, 품목명, 품목 코드 중 하나로 해당 품목이 사용된 BOM 버전 목록을 조회합니다. 프론트에서 자식 기준 역조회/상향 트리 조회의 드롭다운 선택지로 사용합니다.")
    @GetMapping("/children/versions")
    ResponseEntity<ApiResponse<List<String>>> getBomVersionsByChildKeyword(
            @Parameter(description = "자식 품목 ID, 품목명, 품목 코드 검색어", required = true) @RequestParam String keyword
    );

    @Operation(summary = "BOM 상향 역조회 트리", description = "자식 품목 ID, 품목명, 품목 코드 중 하나로 최종 상위 제품까지의 연결 구조를 조회합니다. bomVersion은 /api/boms/children/versions에서 받은 목록 중 선택한 값을 전달합니다.")
    @GetMapping("/children/parents/tree")
    ResponseEntity<ApiResponse<List<BomReverseTreeResponse>>> getParentTreesByChildKeyword(
            @Parameter(description = "자식 품목 ID, 품목명, 품목 코드 검색어", required = true) @RequestParam String keyword,
            @Parameter(description = "BOM 버전") @RequestParam(required = false) String bomVersion
    );
}
