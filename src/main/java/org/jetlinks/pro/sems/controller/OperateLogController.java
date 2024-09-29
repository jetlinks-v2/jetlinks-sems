package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.OperateLogEntity;
import org.jetlinks.pro.sems.service.OperateLogService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/operate/log")
@AllArgsConstructor
@Getter
@Tag(name = "能源操作信息表 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "operate-log", name = "能源操作信息")
public class OperateLogController implements AssetsHolderCrudController<OperateLogEntity,String> {
    private final OperateLogService service;

    private final QueryHelper queryHelper;
}