package com.ssafy.demo_app.api.partner;

import com.ssafy.demo_app.api.partner.dto.PartnerDuplicateCheckResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerStatusUpdateRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerSuppliedItemResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerUsageResponse;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Partner Master API", description = "거래처 마스터 CRUD 관리 API")
@RequestMapping("/api/partners")
public interface PartnerApi {

    @Operation(summary = "거래처 목록 조회", description = "등록된 거래처 마스터 목록을 조회합니다. partnerType으로 SUPPLIER, CUSTOMER 필터링이 가능하고, keyword로 거래처 ID, 거래처 코드, 거래처명, 사업자등록번호를 검색할 수 있습니다.")
    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<PartnerResponse>>> getPartners(
            Pageable pageable,
            @Parameter(description = "거래처 구분 필터") @RequestParam(required = false) PartnerMaster.PartnerType partnerType,
            @Parameter(description = "거래처 상태 필터") @RequestParam(required = false) PartnerMaster.PartnerStatus partnerStatus,
            @Parameter(description = "사업자등록번호 등록 여부") @RequestParam(required = false) Boolean hasBusinessNo,
            @Parameter(description = "거래처 ID, 거래처 코드, 거래처명, 사업자등록번호 검색어") @RequestParam(required = false) String keyword
    );

    @Operation(summary = "거래처 코드 중복 확인", description = "거래처 코드가 이미 사용 중인지 확인합니다.")
    @GetMapping("/duplicates")
    ResponseEntity<ApiResponse<PartnerDuplicateCheckResponse>> checkDuplicate(
            @Parameter(description = "거래처 코드", required = true) @RequestParam String partnerCode
    );

    @Operation(summary = "거래처 검색 조회", description = "거래처 ID, 거래처 코드, 거래처명 중 하나를 입력해 해당 거래처를 조회합니다. 여러 건이 매칭되면 모두 반환합니다.")
    @GetMapping("/{searchValue}")
    ResponseEntity<ApiResponse<List<PartnerResponse>>> searchPartners(
            @Parameter(description = "거래처 ID, 거래처 코드 또는 거래처명", required = true) @PathVariable String searchValue
    );

    @Operation(summary = "거래처 등록", description = "거래처 코드, 거래처명, 거래처 구분, 사업자등록번호, 대표자명, 담당자 연락처를 등록합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<PartnerResponse>> createPartner(
            @Valid @RequestBody PartnerRequest request
    );

    @Operation(summary = "거래처 수정", description = "ID에 해당하는 거래처 마스터 정보를 수정합니다.")
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<PartnerResponse>> updatePartner(
            @Parameter(description = "거래처 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody PartnerRequest request
    );

    @Operation(summary = "거래처 상태 변경", description = "거래처를 활성 또는 비활성 상태로 변경합니다.")
    @PatchMapping("/{id}/status")
    ResponseEntity<ApiResponse<PartnerResponse>> updatePartnerStatus(
            @Parameter(description = "거래처 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody PartnerStatusUpdateRequest request
    );

    @Operation(summary = "거래처 사용 현황 조회", description = "입고/출하 참조 건수, 최근 사용일, 삭제 가능 여부를 조회합니다.")
    @GetMapping("/{id}/usage")
    ResponseEntity<ApiResponse<PartnerUsageResponse>> getPartnerUsage(
            @Parameter(description = "거래처 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "공급사 실제 공급 품목 이력 조회", description = "공급사의 입고 이력을 기준으로 실제 공급한 품목 요약을 조회합니다.")
    @GetMapping("/{id}/supplied-items")
    ResponseEntity<ApiResponse<List<PartnerSuppliedItemResponse>>> getSuppliedItems(
            @Parameter(description = "거래처 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "거래처 삭제", description = "ID에 해당하는 거래처 마스터 정보를 삭제합니다. 입고 또는 출하 이력에서 참조 중이면 삭제할 수 없습니다.")
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<Void>> deletePartner(
            @Parameter(description = "거래처 ID", required = true) @PathVariable Integer id
    );
}
