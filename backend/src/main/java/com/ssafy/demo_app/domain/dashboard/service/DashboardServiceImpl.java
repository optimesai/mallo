package com.ssafy.demo_app.domain.dashboard.service;

import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import com.ssafy.demo_app.api.dashboard.dto.DashboardInsightResponse;
import com.ssafy.demo_app.api.dashboard.dto.DashboardMetricViewResponse;
import com.ssafy.demo_app.api.dashboard.dto.DashboardSummaryCardResponse;
import com.ssafy.demo_app.api.dashboard.dto.DashboardSummaryResponse;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.production.repository.ProductionExecutionRepository;
import com.ssafy.demo_app.domain.production.repository.WorkOrderRepository;
import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
import com.ssafy.demo_app.domain.shipping.repository.OutboundShippingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final String PERIOD_TODAY = "today";
    private static final String PERIOD_7D = "7d";
    private static final String PERIOD_30D = "30d";

    private final ProductionExecutionRepository productionExecutionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CurrentInventoryRepository currentInventoryRepository;
    private final InventoryTransactionHistoryRepository inventoryTransactionHistoryRepository;
    private final OutboundShippingRepository outboundShippingRepository;

    @Override
    public DashboardSummaryResponse getSummary(String period) {
        String normalizedPeriod = normalizePeriod(period);
        LocalDateTime fromDateTime = resolveFromDateTime(normalizedPeriod);

        List<Map<String, Object>> productionRows = getProductionRows(fromDateTime);
        List<Map<String, Object>> qualityRows = getQualityRows(fromDateTime);
        List<Map<String, Object>> inventoryRows = getInventoryRows(fromDateTime);
        List<Map<String, Object>> shippingRows = getShippingRows(fromDateTime);

        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setGeneratedAt(LocalDateTime.now());
        response.setPeriod(normalizedPeriod);
        response.setSummaryCards(getSummaryCards(fromDateTime, qualityRows));
        response.setMetricViews(List.of(
                metric("production", "생산", "라인별 생산량 비교", "마감된 작업지시의 마지막 공정 양품 수량을 공장/라인 단위로 비교합니다.",
                        chart("BAR", "lineName", List.of("totalQty"), "라인별 생산량", "라인별 수치 비교 데이터이므로 Bar 차트를 추천합니다."), productionRows),
                metric("quality", "품질", "제품별 불량률 비교", "양품/불량 실적을 기준으로 불량률이 높은 제품을 식별합니다.",
                        chart("BAR", "itemName", List.of("defectRate"), "제품별 불량률", "제품별 비율 비교 데이터이므로 Bar 차트를 추천합니다."), qualityRows),
                metric("inventory", "재고", "창고별 재고 회전 관찰", "출고 수량 대비 현재 재고 규모를 비교해 체류 위험을 확인합니다.",
                        chart("BAR", "warehouseName", List.of("turnoverRate"), "창고별 재고 회전율", "창고별 핵심 지표 비교 데이터이므로 Bar 차트를 추천합니다."), inventoryRows),
                metric("shipping", "출하", "거래처별 출하 대기 물량", "출하 지시 중 아직 완료되지 않은 물량을 거래처별로 비교합니다.",
                        chart("BAR", "partnerName", List.of("waitingQty"), "거래처별 출하 대기", "거래처별 대기 물량 순위를 비교하므로 Bar 차트를 추천합니다."), shippingRows)
        ));
        response.setInsights(getInsights(qualityRows, inventoryRows, shippingRows));
        return response;
    }

    private List<DashboardSummaryCardResponse> getSummaryCards(
            LocalDateTime fromDateTime,
            List<Map<String, Object>> qualityRows
    ) {
        BigDecimal avgDefectRate = average(qualityRows, "defectRate");
        long shortageCount = currentInventoryRepository.countItemsUnderSafetyStock();
        long finalProductionTotal = inventoryTransactionHistoryRepository.sumProductionReceiptQty(fromDateTime);
        long waitingShippingCount = outboundShippingRepository.countByStatus(OutboundShipping.ShippingStatus.READY);
        long activeWorkOrderCount = workOrderRepository.countByStatusIn(List.of(
                WorkOrder.OrderStatus.READY,
                WorkOrder.OrderStatus.RUN,
                WorkOrder.OrderStatus.HOLD
        ));

        return List.of(
                card("production-throughput", "라인 생산량", formatNumber(finalProductionTotal) + " EA", "선택 기간 마감 생산량", "success"),
                card("avg-defect-rate", "평균 불량률", avgDefectRate + "%", "제품별 불량률 평균", avgDefectRate.compareTo(BigDecimal.valueOf(3)) >= 0 ? "warning" : "success"),
                card("stock-shortage", "안전재고 미만", formatNumber(shortageCount) + " 품목", "현재고와 품목 안전재고 비교", shortageCount > 0 ? "danger" : "success"),
                card("shipping-backlog", "출하 대기", formatNumber(waitingShippingCount) + " 건", "READY 상태 출하 지시", waitingShippingCount > 0 ? "neutral" : "success"),
                card("active-work-orders", "진행 작업지시", formatNumber(activeWorkOrderCount) + " 건", "READY/RUN/HOLD 상태", "neutral")
        );
    }

    private List<Map<String, Object>> getProductionRows(LocalDateTime fromDateTime) {
        return inventoryTransactionHistoryRepository.aggregateProductionReceiptByLine(fromDateTime).stream()
                .map(row -> {
                    long goodQty = toLong(row[1]);
                    long defectQty = toLong(row[2]);
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("lineName", row[0]);
                    data.put("goodQty", goodQty);
                    data.put("defectQty", defectQty);
                    data.put("totalQty", goodQty);
                    return data;
                })
                .toList();
    }

    private List<Map<String, Object>> getQualityRows(LocalDateTime fromDateTime) {
        return productionExecutionRepository.aggregateQualityByItem(fromDateTime).stream()
                .map(row -> {
                    long goodQty = toLong(row[1]);
                    long defectQty = toLong(row[2]);
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("itemName", row[0]);
                    data.put("goodQty", goodQty);
                    data.put("defectQty", defectQty);
                    data.put("defectRate", percent(defectQty, goodQty + defectQty));
                    return data;
                })
                .toList();
    }

    private List<Map<String, Object>> getInventoryRows(LocalDateTime fromDateTime) {
        Map<String, Long> outboundQtyByWarehouse = new HashMap<>();
        inventoryTransactionHistoryRepository.aggregateOutboundQtyByWarehouse(fromDateTime)
                .forEach(row -> outboundQtyByWarehouse.put(String.valueOf(row[0]), toLong(row[1])));

        return currentInventoryRepository.aggregateCurrentQtyByWarehouse().stream()
                .map(row -> {
                    String warehouseName = String.valueOf(row[0]);
                    long currentQty = toLong(row[1]);
                    long outboundQty = outboundQtyByWarehouse.getOrDefault(warehouseName, 0L);
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("warehouseName", warehouseName);
                    data.put("currentQty", currentQty);
                    data.put("outboundQty", outboundQty);
                    data.put("turnoverRate", ratio(outboundQty, currentQty));
                    return data;
                })
                .toList();
    }

    private List<Map<String, Object>> getShippingRows(LocalDateTime fromDateTime) {
        return outboundShippingRepository.aggregateWaitingShippingByPartner(fromDateTime).stream()
                .map(row -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("partnerName", row[0]);
                    data.put("waitingQty", Math.max(toLong(row[1]), 0));
                    data.put("shippedQty", toLong(row[2]));
                    data.put("readyOrders", toLong(row[3]));
                    return data;
                })
                .toList();
    }

    private List<DashboardInsightResponse> getInsights(
            List<Map<String, Object>> qualityRows,
            List<Map<String, Object>> inventoryRows,
            List<Map<String, Object>> shippingRows
    ) {
        List<DashboardInsightResponse> insights = new ArrayList<>();

        qualityRows.stream()
                .filter(row -> toBigDecimal(row.get("defectRate")).compareTo(BigDecimal.valueOf(3)) >= 0)
                .findFirst()
                .ifPresent(row -> insights.add(insight(
                        "quality-risk",
                        row.get("itemName") + " 불량률 확인",
                        "선택 기간 불량률이 " + row.get("defectRate") + "%입니다.",
                        "warning",
                        "production_execution"
                )));

        long shortageCount = currentInventoryRepository.countItemsUnderSafetyStock();
        if (shortageCount > 0) {
            insights.add(insight(
                    "stock-risk",
                    "안전재고 미만 품목 " + formatNumber(shortageCount) + "건",
                    "현재고가 품목 안전재고보다 낮은 품목이 있습니다.",
                    "danger",
                    "current_inventory"
            ));
        }

        shippingRows.stream()
                .findFirst()
                .ifPresent(row -> insights.add(insight(
                        "shipping-risk",
                        row.get("partnerName") + " 출하 대기 집중",
                        "출하 대기 물량 " + formatNumber(toLong(row.get("waitingQty"))) + "EA를 확인해야 합니다.",
                        "neutral",
                        "outbound_shipping"
                )));

        if (insights.isEmpty()) {
            insights.add(insight(
                    "normal",
                    "우선 확인 대상 없음",
                    "선택 기간 기준 위험 기준을 초과한 지표가 없습니다.",
                    "success",
                    "dashboard"
            ));
        }

        return insights;
    }

    private DashboardMetricViewResponse metric(
            String id,
            String label,
            String title,
            String subtitle,
            AiChartResponse chart,
            List<Map<String, Object>> rows
    ) {
        DashboardMetricViewResponse response = new DashboardMetricViewResponse();
        response.setId(id);
        response.setLabel(label);
        response.setTitle(title);
        response.setSubtitle(subtitle);
        response.setChart(rows.isEmpty() ? AiChartResponse.none("표시할 데이터가 없습니다.") : chart);
        response.setRows(rows);
        return response;
    }

    private AiChartResponse chart(String type, String xKey, List<String> yKeys, String title, String reason) {
        AiChartResponse response = new AiChartResponse();
        response.setEnabled(true);
        response.setType(AiChartResponse.ChartType.valueOf(type));
        response.setXKey(xKey);
        response.setYKeys(yKeys);
        response.setTitle(title);
        response.setReason(reason);
        return response;
    }

    private DashboardSummaryCardResponse card(String id, String label, String value, String caption, String severity) {
        DashboardSummaryCardResponse response = new DashboardSummaryCardResponse();
        response.setId(id);
        response.setLabel(label);
        response.setValue(value);
        response.setCaption(caption);
        response.setSeverity(severity);
        return response;
    }

    private DashboardInsightResponse insight(String id, String title, String description, String severity, String source) {
        DashboardInsightResponse response = new DashboardInsightResponse();
        response.setId(id);
        response.setTitle(title);
        response.setDescription(description);
        response.setSeverity(severity);
        response.setSource(source);
        return response;
    }

    private String normalizePeriod(String period) {
        if (PERIOD_TODAY.equals(period) || PERIOD_30D.equals(period)) {
            return period;
        }
        return PERIOD_7D;
    }

    private LocalDateTime resolveFromDateTime(String period) {
        if (PERIOD_TODAY.equals(period)) {
            return LocalDate.now().atStartOfDay();
        }
        if (PERIOD_30D.equals(period)) {
            return LocalDate.now().minusDays(29).atStartOfDay();
        }
        return LocalDate.now().minusDays(6).atTime(LocalTime.MIN);
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<Map<String, Object>> rows, String key) {
        if (rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = rows.stream()
                .map(row -> toBigDecimal(row.get(key)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);
    }

    private String formatNumber(long value) {
        return String.format("%,d", value);
    }
}
