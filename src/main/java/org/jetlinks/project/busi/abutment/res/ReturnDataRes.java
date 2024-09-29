package org.jetlinks.project.busi.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReturnDataRes {

    @Schema(description = "用量")
    private BigDecimal number;

    @Schema(description = "费用")
    private BigDecimal cost;
}
