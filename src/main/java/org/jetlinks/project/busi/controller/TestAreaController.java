package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.TestAreaEntity;
import org.jetlinks.project.busi.service.TestAreaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sems/area")
@AllArgsConstructor
@Getter
@Tag(name = "场所列表") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Slf4j
@Resource(id = "test_area", name = "场所")
public class TestAreaController implements AssetsHolderCrudController<TestAreaEntity,String> {
    private final TestAreaService service;
}
