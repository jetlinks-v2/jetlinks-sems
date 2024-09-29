package org.jetlinks.project.busi.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.EnergyGatherEntity;
import org.jetlinks.project.busi.service.ElectricityConsumeService;
import org.jetlinks.project.busi.service.EnergyGatherService;
import org.jetlinks.project.busi.service.GasConsumeService;
import org.jetlinks.project.busi.service.WaterConsumeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sems/energy/gather")
@AllArgsConstructor
@Getter
@Tag(name = "能源采集1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
public class EnergyGatherController implements AssetsHolderCrudController<EnergyGatherEntity,String> {

    private EnergyGatherService service;

    private final QueryHelper queryHelper;

    private final WaterConsumeService waterConsumeService;

    private final ElectricityConsumeService electricityConsumeService;

    private final GasConsumeService gasConsumeService;


}
