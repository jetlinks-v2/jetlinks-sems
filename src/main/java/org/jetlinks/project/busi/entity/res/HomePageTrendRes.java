package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomePageTrendRes {
    @Schema(description = "数据list")
    private List<HomePageTrendInfo> homePageTrendInfoList;
    @Schema(description = "峰值")
    private BigDecimal peakNumber;
    @Schema(description = "均值")
    private BigDecimal average;
    @Schema(description = "类型")
    private String type;
}
