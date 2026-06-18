package com.ssafy.demo_app.domain.bom.service;

import com.ssafy.demo_app.api.bom.dto.BomBulkRequest;
import com.ssafy.demo_app.api.bom.dto.BomGroupResponse;
import com.ssafy.demo_app.api.bom.dto.BomRequest;
import com.ssafy.demo_app.api.bom.dto.BomResponse;
import com.ssafy.demo_app.api.bom.dto.BomReverseResponse;
import com.ssafy.demo_app.api.bom.dto.BomReverseTreeResponse;
import com.ssafy.demo_app.api.bom.dto.BomStatusUpdateRequest;
import com.ssafy.demo_app.api.bom.dto.BomTreeResponse;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.global.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BomService {
    PageResponse<BomGroupResponse> getBomGroups(
            Pageable pageable,
            String parentKeyword,
            String childKeyword,
            String bomVersion
    );
    List<BomResponse> getBoms(
            String parentKeyword,
            String childKeyword,
            String bomVersion
    );
    List<BomResponse> getBomGroup(Integer parentItemId, String bomVersion);
    BomResponse getBom(Integer bomId);
    List<BomResponse> createBoms(BomBulkRequest request);
    BomResponse createBom(BomRequest request);
    BomResponse updateBom(Integer bomId, BomRequest request);
    void deleteBom(Integer bomId);
    BomResponse updateBomStatus(Integer bomId, BomStatusUpdateRequest request);
    List<BomResponse> getBomsByParentKeyword(String keyword, String bomVersion);
    List<BomTreeResponse> getBomTreesByParentKeyword(String keyword, String bomVersion);
    List<BomReverseResponse> getParentsByChildKeyword(String keyword, String bomVersion);
    List<BomReverseTreeResponse> getParentTreesByChildKeyword(String keyword, String bomVersion);
    List<String> getBomVersionsByParentKeyword(String keyword);
    List<String> getBomVersionsByChildKeyword(String keyword);
    void validateActiveBomVersion(ItemMaster parentItem, String bomVersion);
    List<BomRequirement> calculateMaterialRequirements(ItemMaster parentItem, String bomVersion, Integer targetQty);
}
