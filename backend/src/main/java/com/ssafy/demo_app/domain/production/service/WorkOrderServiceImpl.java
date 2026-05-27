package com.ssafy.demo_app.domain.production.service;

import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import com.ssafy.demo_app.domain.bom.repository.BomStructureRepository;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.production.repository.WorkOrderRepository;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final BomStructureRepository bomStructureRepository;
    private final UserRepository userRepository;
    private final CurrentInventoryRepository currentInventoryRepository;
    private final InventoryTransactionHistoryRepository transactionHistoryRepository;

    @Override
    @Transactional
    public void issueMaterials(Integer orderId, Integer workerId) {
        WorkOrder workOrder = workOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND));

        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (workOrder.getStatus() != WorkOrder.OrderStatus.READY) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_INVALID);
        }

        ItemMaster parentItem = workOrder.getItem();
        List<BomStructure> bomList = bomStructureRepository.findByParentItem(parentItem);
        if (bomList.isEmpty()) {
            throw new BusinessException(ErrorCode.BOM_NOT_FOUND);
        }

        // 1. Validate if there is enough stock for all items
        for (BomStructure bom : bomList) {
            ItemMaster childItem = bom.getChildItem();
            int requiredQty = bom.getQuantity()
                    .multiply(BigDecimal.valueOf(workOrder.getTargetQty()))
                    .intValue();

            List<CurrentInventory> inventories = currentInventoryRepository.findByItem(childItem);
            int totalAvailableQty = inventories.stream()
                    .mapToInt(CurrentInventory::getCurrentQty)
                    .sum();

            if (totalAvailableQty < requiredQty) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }

        // 2. Simple Stock deduction and Transaction History creation (No FIFO receipts sort)
        for (BomStructure bom : bomList) {
            ItemMaster childItem = bom.getChildItem();
            int requiredQty = bom.getQuantity()
                    .multiply(BigDecimal.valueOf(workOrder.getTargetQty()))
                    .intValue();

            int remainingQty = requiredQty;

            List<CurrentInventory> inventories = currentInventoryRepository.findByItem(childItem);
            for (CurrentInventory inventory : inventories) {
                if (remainingQty <= 0) break;
                if (inventory.getCurrentQty() > 0) {
                    int deductQty = Math.min(inventory.getCurrentQty(), remainingQty);
                    inventory.setCurrentQty(inventory.getCurrentQty() - deductQty);
                    currentInventoryRepository.save(inventory);

                    remainingQty -= deductQty;

                    InventoryTransactionHistory history = new InventoryTransactionHistory();
                    history.setItem(childItem);
                    history.setLocation(inventory.getLocation());
                    history.setTransactionType(InventoryTransactionHistory.TransactionType.PRODUCTION_ISSUE);
                    history.setQuantity(deductQty);
                    history.setReasonDesc("Production issue for WorkOrder: " + workOrder.getOrderNo());
                    history.setWorker(worker);
                    transactionHistoryRepository.save(history);
                }
            }

            if (remainingQty > 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }

        workOrder.setStatus(WorkOrder.OrderStatus.RUN);
        workOrderRepository.save(workOrder);
    }
}
