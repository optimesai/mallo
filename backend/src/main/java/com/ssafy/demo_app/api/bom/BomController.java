package com.ssafy.demo_app.api.bom;

import com.ssafy.demo_app.api.bom.dto.BomRequest;
import com.ssafy.demo_app.api.bom.dto.BomResponse;
import com.ssafy.demo_app.api.bom.dto.BomReverseResponse;
import com.ssafy.demo_app.api.bom.dto.BomReverseTreeResponse;
import com.ssafy.demo_app.api.bom.dto.BomTreeResponse;
import com.ssafy.demo_app.domain.bom.service.BomService;
import com.ssafy.demo_app.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BomController implements BomApi {

    private final BomService bomService;

    @Override
    public ResponseEntity<ApiResponse<List<BomResponse>>> getBoms(
            String parentKeyword,
            String childKeyword,
            String bomVersion
    ) {
        return ResponseEntity.ok(ApiResponse.success(bomService.getBoms(
                parentKeyword,
                childKeyword,
                bomVersion
        )));
    }

    @Override
    public ResponseEntity<ApiResponse<BomResponse>> getBom(Integer bomId) {
        return ResponseEntity.ok(ApiResponse.success(bomService.getBom(bomId)));
    }

    @Override
    public ResponseEntity<ApiResponse<BomResponse>> createBom(BomRequest request) {
        BomResponse response = bomService.createBom(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("BOM이 등록되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<BomResponse>> updateBom(Integer bomId, BomRequest request) {
        BomResponse response = bomService.updateBom(bomId, request);
        return ResponseEntity.ok(ApiResponse.success("BOM이 수정되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteBom(Integer bomId) {
        bomService.deleteBom(bomId);
        return ResponseEntity.ok(ApiResponse.success("BOM이 삭제되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponse<List<BomResponse>>> getBomsByParentKeyword(
            String keyword,
            String bomVersion
    ) {
        return ResponseEntity.ok(ApiResponse.success(bomService.getBomsByParentKeyword(keyword, bomVersion)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<BomTreeResponse>>> getBomTreesByParentKeyword(
            String keyword,
            String bomVersion
    ) {
        return ResponseEntity.ok(ApiResponse.success(bomService.getBomTreesByParentKeyword(keyword, bomVersion)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<String>>> getBomVersionsByParentKeyword(
            String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(bomService.getBomVersionsByParentKeyword(keyword)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<BomReverseResponse>>> getParentsByChildKeyword(
            String keyword,
            String bomVersion
    ) {
        return ResponseEntity.ok(ApiResponse.success(bomService.getParentsByChildKeyword(keyword, bomVersion)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<String>>> getBomVersionsByChildKeyword(String keyword) {
        return ResponseEntity.ok(ApiResponse.success(bomService.getBomVersionsByChildKeyword(keyword)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<BomReverseTreeResponse>>> getParentTreesByChildKeyword(
            String keyword,
            String bomVersion
    ) {
        return ResponseEntity.ok(ApiResponse.success(bomService.getParentTreesByChildKeyword(keyword, bomVersion)));
    }
}
