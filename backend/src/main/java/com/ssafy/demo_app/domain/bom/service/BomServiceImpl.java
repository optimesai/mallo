package com.ssafy.demo_app.domain.bom.service;

import com.ssafy.demo_app.api.bom.dto.BomBulkLineRequest;
import com.ssafy.demo_app.api.bom.dto.BomBulkRequest;
import com.ssafy.demo_app.api.bom.dto.BomGroupResponse;
import com.ssafy.demo_app.api.bom.dto.BomRequest;
import com.ssafy.demo_app.api.bom.dto.BomResponse;
import com.ssafy.demo_app.api.bom.dto.BomReverseResponse;
import com.ssafy.demo_app.api.bom.dto.BomReverseTreeResponse;
import com.ssafy.demo_app.api.bom.dto.BomStatusUpdateRequest;
import com.ssafy.demo_app.api.bom.dto.BomTreeResponse;
import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import com.ssafy.demo_app.domain.bom.repository.BomStructureRepository;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.repository.ItemMasterRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import com.ssafy.demo_app.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BomServiceImpl implements BomService {

    private static final String DEFAULT_BOM_VERSION = "v1.0";

    private final BomStructureRepository bomStructureRepository;
    private final ItemMasterRepository itemMasterRepository;

    @Override
    public PageResponse<BomGroupResponse> getBomGroups(
            Pageable pageable,
            String parentKeyword,
            String childKeyword,
            String bomVersion
    ) {
        return PageResponse.from(bomStructureRepository.searchBomGroups(
                normalize(parentKeyword),
                normalize(childKeyword),
                normalize(bomVersion),
                BomStructure.BomStatus.ACTIVE,
                pageable
        ));
    }

    @Override
    public List<BomResponse> getBoms(
            String parentKeyword,
            String childKeyword,
            String bomVersion
    ) {
        return bomStructureRepository.searchBoms(
                        normalize(parentKeyword),
                        normalize(childKeyword),
                        normalize(bomVersion)
                )
                .stream()
                .map(BomResponse::from)
                .toList();
    }

    @Override
    public List<BomResponse> getBomGroup(Integer parentItemId, String bomVersion) {
        ItemMaster parentItem = findItem(parentItemId);
        return bomStructureRepository.findByParentItemAndBomVersionOrderByBomIdAsc(
                        parentItem,
                        normalizeVersion(bomVersion)
                )
                .stream()
                .map(BomResponse::from)
                .toList();
    }

    @Override
    public BomResponse getBom(Integer bomId) {
        return BomResponse.from(findBom(bomId));
    }

    @Override
    @Transactional
    public List<BomResponse> createBoms(BomBulkRequest request) {
        ItemMaster parentItem = findItem(request.getParentItemId());
        String bomVersion = normalizeVersion(request.getBomVersion());
        Set<Integer> childItemIds = new HashSet<>();
        List<BomStructure> boms = new ArrayList<>();

        for (BomBulkLineRequest line : request.getLines()) {
            if (!childItemIds.add(line.getChildItemId())) {
                throw new BusinessException(ErrorCode.BOM_DUPLICATE);
            }
            ItemMaster childItem = findItem(line.getChildItemId());
            validateBom(parentItem, childItem, line.getQuantity(), bomVersion, null);

            BomStructure bom = new BomStructure();
            bom.setParentItem(parentItem);
            bom.setChildItem(childItem);
            bom.setQuantity(line.getQuantity());
            bom.setBomVersion(bomVersion);
            boms.add(bom);
        }

        return bomStructureRepository.saveAll(boms)
                .stream()
                .map(BomResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public BomResponse createBom(BomRequest request) {
        ItemMaster parentItem = findItem(request.getParentItemId());
        ItemMaster childItem = findItem(request.getChildItemId());
        String bomVersion = normalizeVersion(request.getBomVersion());

        validateBom(parentItem, childItem, request.getQuantity(), bomVersion, null);

        BomStructure bom = new BomStructure();
        bom.setParentItem(parentItem);
        bom.setChildItem(childItem);
        bom.setQuantity(request.getQuantity());
        bom.setBomVersion(bomVersion);

        return BomResponse.from(bomStructureRepository.save(bom));
    }

    @Override
    @Transactional
    public BomResponse updateBom(Integer bomId, BomRequest request) {
        BomStructure bom = findBom(bomId);
        ItemMaster parentItem = findItem(request.getParentItemId());
        ItemMaster childItem = findItem(request.getChildItemId());
        String bomVersion = normalizeVersion(request.getBomVersion());

        validateBom(parentItem, childItem, request.getQuantity(), bomVersion, bomId);

        bom.setParentItem(parentItem);
        bom.setChildItem(childItem);
        bom.setQuantity(request.getQuantity());
        bom.setBomVersion(bomVersion);

        return BomResponse.from(bomStructureRepository.save(bom));
    }

    @Override
    @Transactional
    public void deleteBom(Integer bomId) {
        BomStructure bom = findBom(bomId);
        bom.setBomStatus(BomStructure.BomStatus.INACTIVE);
        bomStructureRepository.save(bom);
    }

    @Override
    @Transactional
    public BomResponse updateBomStatus(Integer bomId, BomStatusUpdateRequest request) {
        BomStructure bom = findBom(bomId);
        bom.setBomStatus(request.getBomStatus());
        return BomResponse.from(bomStructureRepository.save(bom));
    }

    @Override
    public List<BomResponse> getBomsByParentKeyword(String keyword, String bomVersion) {
        return findItemsByKeyword(keyword)
                .stream()
                .flatMap(parentItem -> findBomsByParent(parentItem, bomVersion).stream())
                .map(BomResponse::from)
                .toList();
    }

    @Override
    public List<BomTreeResponse> getBomTreesByParentKeyword(String keyword, String bomVersion) {
        return findItemsByKeyword(keyword)
                .stream()
                .filter(item -> item.getItemType() != ItemMaster.ItemType.RAW)
                .map(parentItem -> BomTreeResponse.root(
                        parentItem,
                        buildDownwardChildren(parentItem, bomVersion, new HashSet<>())
                ))
                .toList();
    }

    @Override
    public List<BomReverseResponse> getParentsByChildKeyword(String keyword, String bomVersion) {
        return findItemsByKeyword(keyword)
                .stream()
                .flatMap(childItem -> findBomsByChild(childItem, bomVersion).stream())
                .map(BomReverseResponse::from)
                .toList();
    }

    @Override
    public List<BomReverseTreeResponse> getParentTreesByChildKeyword(String keyword, String bomVersion) {
        return findItemsByKeyword(keyword)
                .stream()
                .map(childItem -> BomReverseTreeResponse.root(
                        childItem,
                        buildUpwardParents(childItem, bomVersion, new HashSet<>())
                ))
                .toList();
    }

    @Override
    public List<String> getBomVersionsByParentKeyword(String keyword) {
        return findItemsByKeyword(keyword)
                .stream()
                .flatMap(parentItem -> findBomsByParent(parentItem, null).stream())
                .map(BomStructure::getBomVersion)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public List<String> getBomVersionsByChildKeyword(String keyword) {
        return findItemsByKeyword(keyword)
                .stream()
                .flatMap(childItem -> bomStructureRepository.findDistinctActiveBomVersionsByChildItem(childItem).stream())
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public void validateActiveBomVersion(ItemMaster parentItem, String bomVersion) {
        if (findBomsByParent(parentItem, bomVersion).isEmpty()) {
            throw new BusinessException(ErrorCode.BOM_NOT_FOUND);
        }
    }

    @Override
    public List<BomRequirement> calculateMaterialRequirements(
            ItemMaster parentItem,
            String bomVersion,
            Integer targetQty
    ) {
        String normalizedBomVersion = normalizeVersion(bomVersion);
        if (findBomsByParent(parentItem, normalizedBomVersion).isEmpty()) {
            throw new BusinessException(ErrorCode.BOM_NOT_FOUND);
        }
        Map<Integer, RequirementAccumulator> requirements = new LinkedHashMap<>();
        collectRequirements(
                parentItem,
                normalizedBomVersion,
                1,
                new HashSet<>(),
                requirements
        );
        if (requirements.isEmpty()) {
            throw new BusinessException(ErrorCode.BOM_NOT_FOUND);
        }

        return requirements.values()
                .stream()
                .map(accumulator -> new BomRequirement(
                        accumulator.item(),
                        accumulator.quantity(),
                        calculateRequiredQty(accumulator.quantity(), targetQty)
                ))
                .toList();
    }

    private void collectRequirements(
            ItemMaster parentItem,
            String bomVersion,
            Integer parentQuantity,
            Set<Integer> visitedItemIds,
            Map<Integer, RequirementAccumulator> requirements
    ) {
        if (!visitedItemIds.add(parentItem.getItemId())) {
            throw new BusinessException(ErrorCode.BOM_CYCLE_DETECTED);
        }

        List<BomStructure> childBoms = findBomsByParent(parentItem, bomVersion);
        if (childBoms.isEmpty()) {
            addRequirement(requirements, parentItem, parentQuantity);
            return;
        }

        for (BomStructure bom : childBoms) {
            ItemMaster childItem = bom.getChildItem();
            Integer nextQuantity = parentQuantity * bom.getQuantity();
            if (childItem.getItemType() == ItemMaster.ItemType.HALF
                    && !findBomsByParent(childItem, bomVersion).isEmpty()) {
                collectRequirements(
                        childItem,
                        bomVersion,
                        nextQuantity,
                        new HashSet<>(visitedItemIds),
                        requirements
                );
                continue;
            }
            addRequirement(requirements, childItem, nextQuantity);
        }
    }

    private void addRequirement(
            Map<Integer, RequirementAccumulator> requirements,
            ItemMaster item,
            Integer quantity
    ) {
        RequirementAccumulator current = requirements.get(item.getItemId());
        if (current == null) {
            requirements.put(item.getItemId(), new RequirementAccumulator(item, quantity));
            return;
        }
        requirements.put(item.getItemId(), new RequirementAccumulator(item, current.quantity() + quantity));
    }

    private Integer calculateRequiredQty(Integer bomQuantity, Integer targetQty) {
        return bomQuantity * targetQty;
    }

    private record RequirementAccumulator(
            ItemMaster item,
            Integer quantity
    ) {
    }

    private List<BomTreeResponse> buildDownwardChildren(
            ItemMaster parentItem,
            String bomVersion,
            Set<Integer> visitedItemIds
    ) {
        if (!visitedItemIds.add(parentItem.getItemId())) {
            throw new BusinessException(ErrorCode.BOM_CYCLE_DETECTED);
        }

        return findBomsByParent(parentItem, bomVersion)
                .stream()
                .map(bom -> BomTreeResponse.of(
                        bom.getChildItem(),
                        bom.getQuantity(),
                        bom.getBomVersion(),
                        buildDownwardChildren(bom.getChildItem(), bomVersion, new HashSet<>(visitedItemIds))
                ))
                .toList();
    }

    private List<BomReverseTreeResponse> buildUpwardParents(
            ItemMaster childItem,
            String bomVersion,
            Set<Integer> visitedItemIds
    ) {
        if (!visitedItemIds.add(childItem.getItemId())) {
            throw new BusinessException(ErrorCode.BOM_CYCLE_DETECTED);
        }

        return findBomsByChild(childItem, bomVersion)
                .stream()
                .map(bom -> BomReverseTreeResponse.of(
                        bom.getParentItem(),
                        bom.getQuantity(),
                        bom.getBomVersion(),
                        buildUpwardParents(bom.getParentItem(), bomVersion, new HashSet<>(visitedItemIds))
                ))
                .toList();
    }

    private void validateBom(
            ItemMaster parentItem,
            ItemMaster childItem,
            Integer quantity,
            String bomVersion,
            Integer updatingBomId
    ) {
        if (parentItem.getItemId().equals(childItem.getItemId())) {
            throw new BusinessException(ErrorCode.BOM_SELF_REFERENCE);
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.BOM_INVALID_QUANTITY);
        }
        if (parentItem.getItemType() == ItemMaster.ItemType.RAW || childItem.getItemType() == ItemMaster.ItemType.FG) {
            throw new BusinessException(ErrorCode.BOM_INVALID_ITEM_TYPE);
        }
        if (isDuplicate(parentItem, childItem, bomVersion, updatingBomId)) {
            throw new BusinessException(ErrorCode.BOM_DUPLICATE);
        }
        if (createsCycle(parentItem, childItem, bomVersion, updatingBomId, new HashSet<>())) {
            throw new BusinessException(ErrorCode.BOM_CYCLE_DETECTED);
        }
    }

    private boolean isDuplicate(
            ItemMaster parentItem,
            ItemMaster childItem,
            String bomVersion,
            Integer updatingBomId
    ) {
        if (updatingBomId == null) {
            return bomStructureRepository.existsByParentItemAndChildItemAndBomVersion(
                    parentItem,
                    childItem,
                    bomVersion
            );
        }
        return bomStructureRepository.existsByParentItemAndChildItemAndBomVersionAndBomIdNot(
                parentItem,
                childItem,
                bomVersion,
                updatingBomId
        );
    }

    private boolean createsCycle(
            ItemMaster targetParentItem,
            ItemMaster currentItem,
            String bomVersion,
            Integer updatingBomId,
            Set<Integer> visitedItemIds
    ) {
        if (currentItem.getItemId().equals(targetParentItem.getItemId())) {
            return true;
        }
        if (!visitedItemIds.add(currentItem.getItemId())) {
            return false;
        }

        return findBomsByParent(currentItem, bomVersion)
                .stream()
                .filter(bom -> updatingBomId == null || !bom.getBomId().equals(updatingBomId))
                .anyMatch(bom -> createsCycle(
                        targetParentItem,
                        bom.getChildItem(),
                        bomVersion,
                        updatingBomId,
                        new HashSet<>(visitedItemIds)
                ));
    }

    private List<BomStructure> findBomsByParent(ItemMaster parentItem, String bomVersion) {
        if (hasText(bomVersion)) {
            return bomStructureRepository.findByParentItemAndBomVersionAndBomStatusOrderByBomIdAsc(
                    parentItem,
                    trimRequired(bomVersion),
                    BomStructure.BomStatus.ACTIVE
            );
        }
        return bomStructureRepository.findByParentItemAndBomStatusOrderByBomIdAsc(
                parentItem,
                BomStructure.BomStatus.ACTIVE
        );
    }

    private List<BomStructure> findBomsByChild(ItemMaster childItem, String bomVersion) {
        if (hasText(bomVersion)) {
            return bomStructureRepository.findByChildItemAndBomVersionAndBomStatusOrderByBomIdAsc(
                    childItem,
                    trimRequired(bomVersion),
                    BomStructure.BomStatus.ACTIVE
            );
        }
        return bomStructureRepository.findByChildItemAndBomStatusOrderByBomIdAsc(
                childItem,
                BomStructure.BomStatus.ACTIVE
        );
    }

    private BomStructure findBom(Integer bomId) {
        return bomStructureRepository.findById(bomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOM_NOT_FOUND));
    }

    private ItemMaster findItem(Integer itemId) {
        return itemMasterRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }

    private List<ItemMaster> findItemsByKeyword(String keyword) {
        List<ItemMaster> items = itemMasterRepository.findAll()
                .stream()
                .filter(item -> matchesItemKeyword(item, keyword))
                .toList();
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        return items;
    }

    private String normalizeVersion(String bomVersion) {
        return hasText(bomVersion) ? trimRequired(bomVersion) : DEFAULT_BOM_VERSION;
    }

    private String normalize(String value) {
        return hasText(value) ? trimRequired(value) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean matchesItemKeyword(ItemMaster item, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = trimRequired(keyword).toLowerCase();
        return item.getItemCode().toLowerCase().contains(normalizedKeyword)
                || item.getItemName().toLowerCase().contains(normalizedKeyword)
                || item.getItemId().toString().equals(normalizedKeyword);
    }

    private String trimRequired(String value) {
        return value.trim();
    }
}
