package com.ssafy.demo_app.api.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "품목 마스터 전체 통계 응답 객체")
public class ItemStatsResponse {

    private long totalCount;
    private long activeCount;
    private long inactiveCount;
}
