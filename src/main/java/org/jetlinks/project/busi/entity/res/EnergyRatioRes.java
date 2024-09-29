package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnergyRatioRes {

    @Schema(description = "场所名字")
    private String placeName;
    @Schema(description = "场所id")
    private String placeId;
    @Schema(description = "占比")
    private BigDecimal energyRadio;
}