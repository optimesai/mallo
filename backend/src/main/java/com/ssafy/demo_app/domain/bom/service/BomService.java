package com.ssafy.demo_app.domain.bom.service;

import com.ssafy.demo_app.api.bom.dto.BomRequest;
import com.ssafy.demo_app.api.bom.dto.BomResponse;
import com.ssafy.demo_app.api.bom.dto.BomReverseResponse;
import com.ssafy.demo_app.api.bom.dto.BomReverseTreeResponse;
import com.ssafy.demo_app.api.bom.dto.BomTreeResponse;

import java.util.List;

public interface BomService {
    List<BomResponse> getBoms(
            String parentKeyword,
            String childKeyword,
            String bomVersion
    );
    BomResponse getBom(Integer bomId);
    BomResponse createBom(BomRequest request);
    BomResponse updateBom(Integer bomId, BomRequest request);
    void deleteBom(Integer bomId);
    List<BomResponse> getBomsByParentKeyword(String keyword, String bomVersion);
    List<BomTreeResponse> getBomTreesByParentKeyword(String keyword, String bomVersion);
    List<BomReverseResponse> getParentsByChildKeyword(String keyword, String bomVersion);
    List<BomReverseTreeResponse> getParentTreesByChildKeyword(String keyword, String bomVersion);
    List<String> getBomVersionsByParentKeyword(String keyword);
    List<String> getBomVersionsByChildKeyword(String keyword);
}
