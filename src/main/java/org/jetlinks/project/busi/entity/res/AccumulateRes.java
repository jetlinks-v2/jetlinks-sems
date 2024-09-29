package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import reactor.core.publisher.Flux;

@Data
public class AccumulateRes {

    @Schema(description = "累计水能")
    WaterConsumeLossRes water;

    @Schema(description = "累计气能")
    GasConsumeLossRes gas;

    @Schema(description = "累计电能")
    ElectricityConsumeLossRes electricity;
}
