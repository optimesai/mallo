package com.ssafy.demo_app.api.partner.dto;

import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "거래처 마스터 생성/수정 요청 객체")
public class PartnerRequest {

    @Schema(description = "거래처 고유 코드", example = "SUP-POSCO-01")
    @NotBlank(message = "거래처 코드는 필수입니다.")
    @Size(max = 50, message = "거래처 코드는 50자 이하여야 합니다.")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "거래처 코드는 대문자 영문, 숫자, 하이픈만 사용할 수 있습니다.")
    private String partnerCode;

    @Schema(description = "거래처명", example = "(주)포스코 인터내셔널")
    @NotBlank(message = "거래처명은 필수입니다.")
    @Size(max = 100, message = "거래처명은 100자 이하여야 합니다.")
    private String partnerName;

    @Schema(description = "거래처 구분", example = "SUPPLIER")
    @NotNull(message = "거래처 구분은 필수입니다.")
    private PartnerMaster.PartnerType partnerType;

    @Schema(description = "사업자등록번호", example = "123-45-67890")
    @Size(max = 50, message = "사업자등록번호는 50자 이하여야 합니다.")
    private String businessNo;

    @Schema(description = "대표자명", example = "홍길동")
    @Size(max = 50, message = "대표자명은 50자 이하여야 합니다.")
    private String representative;

    @Schema(description = "담당자 연락처", example = "02-3457-1114")
    @Size(max = 50, message = "담당자 연락처는 50자 이하여야 합니다.")
    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,50}$", message = "담당자 연락처 형식이 올바르지 않습니다.")
    private String contactPhone;
}
