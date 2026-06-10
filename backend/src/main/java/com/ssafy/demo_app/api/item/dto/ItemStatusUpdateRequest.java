package com.ssafy.demo_app.api.item.dto;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "품목 상태 변경 요청 객체")
public class ItemStatusUpdateRequest {

    @Schema(description = "품목 상태", example = "INACTIVE")
    @NotNull(message = "품목 상태는 필수입니다.")
    private ItemMaster.ItemStatus itemStatus;
}
