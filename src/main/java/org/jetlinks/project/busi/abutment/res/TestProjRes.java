package org.jetlinks.project.busi.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class TestProjRes {


    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "项目所选条目")
    private List<ItemDateRes> itemList;
}
