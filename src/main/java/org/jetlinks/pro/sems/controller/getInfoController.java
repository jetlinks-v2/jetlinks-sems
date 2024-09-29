package org.jetlinks.pro.sems.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.sems.abutment.req.EquipmentReq;
import org.jetlinks.pro.sems.abutment.res.*;
import org.jetlinks.pro.sems.entity.*;
import org.jetlinks.pro.sems.entity.res.TestRecordEnergyRes;
import org.jetlinks.pro.sems.abutment.AbutmentCustService;
import org.jetlinks.pro.sems.abutment.abutmentService;
import org.jetlinks.pro.sems.service.*;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.iot.IotService;
import org.jetlinks.pro.sems.task.TestEnergyTask;
import org.jetlinks.pro.sems.utils.DateUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("")
@AllArgsConstructor
@Getter
@Tag(name = "对接系统 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Slf4j
public class getInfoController {

    private final abutmentService service;

    private final AreaInfoService areaInfoService;

    private final AlarmRecordsService alarmRecordsService;

    private final DeviceService deviceService;



    private final AbutmentCustService abutmentCustService;

    private final TestAreaService testAreaService;

    private final TestConfigService testConfigService;

    private final TestRecordService testRecordService;

    private final TestEnergyDetailService testEnergyDetailService;



    private final UserAndVxUserIdService userAndVxUserIdService;



    private final TestEnergyTask testEnergyTask;

    private final AlarmRuleService alarmRuleService;

    private final ApplicationEventPublisher eventPublisher;

    private final IotService iotService;


    @Operation(summary = "获取用户权限等信息")
    @GetMapping("/getInfo")
    public Mono<Object> getInfo(String accessToken) {
        String api = "/api/system/user/getInfo";

        JSONObject post = service.postGetInfo(api, accessToken);
        return Mono.just(post);
    }

    @Operation(summary = "根据角色获取本系统的菜单权限")
    @GetMapping("/getMenuByRoleId")
    public Mono<Object> abutment(String roleId, String token) {
        String api = "/api/system/menu/roleMenuTreeselect/" + roleId;
        JSONObject jsonObject = service.getMenuByRoleId(api, token);
        List<String> strings = JSON.parseArray(jsonObject.get("menus").toString(), String.class);
        for (String string : strings) {
            if (JSON.parseObject(string).get("label").equals("能源管理  EEM")) {
                String string1 = JSON.parseObject(string).get("children").toString();
                List<MenueRes> menueRes = JSONObject.parseObject(string1, new TypeReference<List<MenueRes>>() {
                });

                return Mono.just(menueRes);
            }
        }
        return Mono.empty();
    }



    @Operation(summary = "获取菜单")
    @GetMapping("/menu")
    public Mono<Object> getMenu(String accessToken) {
        String api = "/system/menu/getRouters";
        JSONObject jsonObject = service.getMenu(api, accessToken);
        return Mono.just(jsonObject);
    }

    @Operation(summary = "获取报警数量")
    @GetMapping("/getNum")
    public synchronized Mono<Object> getAlarmNum(){
        String api="/api/message/message/record/countUnreadMessage";

        try {
            JSONObject jsonObject = service.get(api, new HashMap<>());
            if(jsonObject.get("data")==null){
                return Mono.empty();
            }
            AlarmCountRes data1 = JSON.parseObject(jsonObject.get("data").toString(), AlarmCountRes.class);
            if(data1==null){
                return Mono.empty();
            }else {
                return Mono.just(data1);
            }
        }catch (Throwable e){
            log.error("error: {}",e.getMessage(),e);
            e.printStackTrace();
            return Mono.empty();
        }
    }


    @Operation(summary = "获取需要的设备")
    @PostMapping("/getDevice")
    @Transactional
    public synchronized Flux<EquipmentListRes>  getDevice(@RequestBody EquipmentReq equipmentReq) {
        //用于存需要的设备的类型
        ArrayList<String> needDeviceTypeList= new ArrayList<>();
        if(equipmentReq.getEqType() != null) {
            //查询表
            switch (equipmentReq.getEqType()) {
                case "water":
                    needDeviceTypeList.add("水表");
                    break;
                case "electricity":
                    needDeviceTypeList.add("电表");
                    break;
                case "gas":
                    needDeviceTypeList.add("气表");
                    break;
            }
        }
//        }else {
//            //查询设备
//            needDeviceTypeList.add("风机");
//            needDeviceTypeList.add("大风机");
//            needDeviceTypeList.add("能源设备");
//            needDeviceTypeList.add("电机设备");
//        }
        try {
            return service.getDeviceType()
                .filter(v->{
                    if(needDeviceTypeList.isEmpty()){
                        return true;
                    }else {
                        return needDeviceTypeList.contains(v.getTypeName());
                    }
                })
                .map(EquipTypeListRes::getTypeId)
                .collectList()
                .flatMapMany(list->{
                    return service.getDeviceInfo(new EquipmentReq())
                        .filter(i->
                            list.contains(i.getEquipmentTypeId())
                        );
                });
        }catch (Throwable e){
            log.error("error: {}",e.getMessage(),e);
            e.printStackTrace();
            return Flux.empty();
        }
    }

    @Operation(summary = "获取需要的设备分页")
    @PostMapping("/getDevicePage")
    @Transactional
    public synchronized Mono<PagerResult<EquipmentListRes>> getDeviceByPage(@RequestBody EquipmentReq equipmentReq) {
        //用于存需要的设备的类型
        ArrayList<String> needDeviceTypeList= new ArrayList<>();

        if(equipmentReq.getEqType() ==null || "".equals(equipmentReq.getEqType())){
            //查询所有表
            needDeviceTypeList.add("水表");
            needDeviceTypeList.add("电表");
            needDeviceTypeList.add("气表");
        }else {
            switch (equipmentReq.getEqType()){
                case "water":needDeviceTypeList.add("水表");break;
                case "electricity":needDeviceTypeList.add("电表");break;
                case "gas":needDeviceTypeList.add("气表");break;
            }
        }



        try {
            return service.getDeviceType()
                .filter(v->needDeviceTypeList.contains(v.getTypeName()))
                .map(EquipTypeListRes::getTypeId)
                .collectList()
                .flatMap(list->{
                    EquipmentReq equipmentReqQuery = new EquipmentReq();
                    if(equipmentReq.getEquipmentName()!= null){
                        equipmentReqQuery.setEquipmentName(equipmentReq.getEquipmentName());
                    }
                    if(equipmentReq.getEquipmentCode()!= null){
                        equipmentReqQuery.setEquipmentCode(equipmentReq.getEquipmentCode());
                    }
                    return service.getDeviceInfo(equipmentReqQuery)
                        .filter(i->
                            list.contains(i.getEquipmentTypeId())
                        )
                        .flatMap(va->{
                            return areaInfoService
                                .findById(va.getEquipmentLocation())
                                .map(AreaInfoEntity::getAreaName)
                                .doOnNext(va::setEquipmentLocationName)
                                .thenReturn(va);
                        })
                        .collectList()
                        .flatMap(lists->{
                            if(equipmentReq.getIds()!= null){
                                List<String> ids = equipmentReq.getIds();
                                List<EquipmentListRes> selectList = lists.stream().filter(i->ids.contains(i.getEquipmentCode())).collect(Collectors.toList());
                                List<EquipmentListRes> notSelectList = lists.stream().filter(i->!ids.contains(i.getEquipmentCode())).collect(Collectors.toList());
                                selectList.addAll(notSelectList);
                                lists=selectList;
                            }

                            int pageNo = equipmentReq.getPageIndex();
                            int pageSize = equipmentReq.getPageSize();
                            int total = lists.size();
                            //总页数
                            int pageSum = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;

                            //分页
                            List<EquipmentListRes> subList = lists.stream().skip((pageNo - 1) * pageSize).limit(pageSize).
                                collect(Collectors.toList());
                            QueryParamEntity queryParamEntity = new QueryParamEntity();
                            queryParamEntity.setPageSize(pageSize);
                            queryParamEntity.setPageIndex(pageNo);
                            return Mono.just(PagerResult.of(lists.size(),subList,queryParamEntity));
                        });
                });
        }catch (Throwable e){
            log.error("error: {}",e.getMessage(),e);
            e.printStackTrace();
            return Mono.empty();
        }

    }



    @Operation(summary = "更新试验数据")
    @PostMapping("/updateTestData")
    @Authorize(ignore = true)
    public Mono<Void> updateData(@RequestBody UpdateTestRecordRes updateTestRecordRes){
        //判断是开始还是结束
        if(updateTestRecordRes.getExperimentEndTime()!= null && !"".equals(updateTestRecordRes.getExperimentEndTime())){
            String updateEndTime = updateTestRecordRes.getExperimentEndTime();
            String updateStartTime=updateTestRecordRes.getExperimentStartTime();
            return testRecordService
                .createUpdate()
                .set(TestRecordEntity::getTestStartTime,DateUtil.stringToDate(updateStartTime,DateUtil.DATE_WITHSECOND_FORMAT).getTime())
                .set(TestRecordEntity::getTestEndTime,DateUtil.stringToDate(updateEndTime,DateUtil.DATE_WITHSECOND_FORMAT).getTime())
                .set(TestRecordEntity::getItemStatus,"2")
                .where(TestRecordEntity::getItemId,updateTestRecordRes.getExperimentItemId())
                .where(TestRecordEntity::getTestProjId,updateTestRecordRes.getProjId())
                .execute()
                .flatMap(e -> testRecordService
                    .createQuery()
                    .and(TestRecordEntity::getItemId,updateTestRecordRes.getExperimentItemId())
                    .and(TestRecordEntity::getTestProjId,updateTestRecordRes.getProjId())
                    .fetchOne()
                    .flatMap(testEnergyTask::getVoidMono));
        }else if(updateTestRecordRes.getExperimentStartTime()!= null){
            //如果是开始，去拉取该项目下面的条目数据
            String updateStartTime=updateTestRecordRes.getExperimentStartTime();
            String projId = updateTestRecordRes.getProjId();
            String itemId = updateTestRecordRes.getExperimentItemId();
            return this.getItemData(itemId,projId,updateStartTime)
                       .flatMap(e ->testRecordService
                           .createQuery()
                           .and(TestRecordEntity::getItemId,updateTestRecordRes.getExperimentItemId())
                           .and(TestRecordEntity::getTestProjId,updateTestRecordRes.getProjId())
                           .fetchOne()
                           .flatMap(testEnergyTask::getVoidMono));
        }
        return Mono.empty();
    }

//    //同步场所
//    @Operation(summary = "同步场所")
//    @GetMapping("/test/Area")
//    public Mono<SaveResult> getTest(){
//        return abutmentCustService.getTestSite()
//            .collectList()
//            .flatMap(testAreaService::save);
//    }


    //同步条目
//    @Operation(summary = "同步条目")
//    @GetMapping("/test/item")
//    public Flux<Integer> getTestItem(){
//        return abutmentCustService.getItemList()
//            .flatMap(va->{
//                TestConfigEntity testConfigEntity = new TestConfigEntity();
//                testConfigEntity.setId(va.getId());
//                testConfigEntity.setTestName(va.getItemName());
//                testConfigEntity.setStatus(va.getStatus());
//                return testConfigService.insert(testConfigEntity);
//            });
//    }

    public Mono<Object> getItemData(String itemId, String projId,String startDate) {
                return abutmentCustService
                    .getTestProj(projId)
                    .flatMap(list -> {
                        if (list.getItemList() == null) {
                            return Mono.empty();
                        }
                        //条目
                        List<ItemDateRes> items = list.getItemList();
                        List<ItemDateRes> itemDateResStream = items.stream().filter(i -> i.getId().equals(itemId)).filter(i->i.getItemName()!= null).filter(i->!i.getDeviceResList().isEmpty()).collect(Collectors.toList());
                        if(!itemDateResStream.isEmpty()){
                            return Mono.just(itemDateResStream.get(0))
                                .flatMap(va -> {
                                    //条目
                                    TestConfigEntity testConfigEntity = new TestConfigEntity();
                                    testConfigEntity.setTestName(va.getItemName());
                                    testConfigEntity.setId(va.getExperimentItemId());

                                    //记录
                                    TestRecordEntity testRecordEntity = new TestRecordEntity();
                                    //试验记录名称：
                                    testRecordEntity.setName("客户：{" + list.getCustomerName() + "}做的：{" + va.getItemName() + "}试验");
                                    testRecordEntity.setTestStartTime(Objects.requireNonNull(DateUtil.stringToDate(startDate, DateUtil.DATE_WITHSECOND_FORMAT)).getTime());
                                    testRecordEntity.setTestEndTime(Objects.requireNonNull(DateUtil.stringToDate(va.getExperimentEndTime(), DateUtil.DATE_WITHSECOND_FORMAT)).getTime());
                                    testRecordEntity.setConfigId(va.getExperimentItemId());
                                    testRecordEntity.setTestName(testConfigEntity.getTestName());
                                    testRecordEntity.setTester(va.getPeopleName());
                                    testRecordEntity.setTestPeopleId(va.getPeopleIdList());
                                    testRecordEntity.setTestProjId(projId);
                                    testRecordEntity.setItemStatus(va.getItemStatus());
                                    testRecordEntity.setItemId(va.getId());
                                    testRecordEntity.setRecordType("0");
                                    //判断是否有重复条目
                                    return testConfigService.findById(testConfigEntity.getId())
                                        .hasElement()
                                        .flatMap(aBoolean -> {
                                            if(!aBoolean){
                                                return testConfigService.insert(testConfigEntity);
                                            }
                                            return Mono.just(-1);
                                        })
                                        .then(
                                            //判断试验记录是否重复
                                            testRecordService
                                                .createQuery()
                                                .where(TestRecordEntity::getName,testRecordEntity.getName())
                                                .where(TestRecordEntity::getItemId,testRecordEntity.getItemId())
                                                .where(TestRecordEntity::getTestProjId,testRecordEntity.getTestProjId())
                                                .fetch()
                                                .hasElements()
                                                .flatMap(aBoolean -> {
                                                    if (aBoolean) {
                                                        return Mono.just(-1);
                                                    } else {
                                                        ArrayList<TestEnergyDetailEntity> insetDetail = new ArrayList<>();
                                                        List<ExperimentDeviceRes> deviceResList = va.getDeviceResList();
                                                        return testRecordService.insert(testRecordEntity)
                                                            .then(
                                                                Flux.fromIterable(deviceResList)
                                                                    .flatMap(experimentDeviceRes->{
                                                                        //判断是否共用设备
                                                                        DeviceInfoEntity deviceInfo = new DeviceInfoEntity();
                                                                        TestEnergyDetailEntity testEnergyDetailEntity = new TestEnergyDetailEntity();
                                                                        return deviceService
                                                                            .createQuery()
                                                                            .where(DeviceInfoEntity::getDeviceId,experimentDeviceRes.getDeviceId())
                                                                            .where(DeviceInfoEntity::getStatus,"0")
                                                                            .fetchOne()
                                                                            .switchIfEmpty(Mono.just(deviceInfo))
                                                                            .flatMap(device->{
                                                                                testEnergyDetailEntity.setShareDevice("0");
                                                                                if(device.getShareDevice()!= null){
                                                                                    testEnergyDetailEntity.setShareDevice(device.getShareDevice());
                                                                                }
                                                                                testEnergyDetailEntity.setTestRecordId(testRecordEntity.getId());
                                                                                testEnergyDetailEntity.setDeviceId(experimentDeviceRes.getDeviceId());
                                                                                testEnergyDetailEntity.setDeviceName(experimentDeviceRes.getDeviceName());
                                                                                insetDetail.add(testEnergyDetailEntity);
                                                                                return Mono.just(insetDetail);
                                                                            });
                                                                    })
                                                                    .then(testEnergyDetailService.save(insetDetail))
                                                            );
                                                    }
                                                })
                                        );
                                });
                        }
                        return Mono.empty();


                    });
    }



    @Operation(summary = "测试项目同步")
    @GetMapping("/testProj")
    public Flux<Object> getTestData() {
        //时间控制
        //当前时间定时任务时间
        String todayStartTime = DateUtil.dateToString(DateUtil.getTodayStartTime(),DateUtil.DATE_SHORT_FORMAT);
        String todayEndTime = DateUtil.dateToString(DateUtil.getTodayEndTime(),DateUtil.DATE_SHORT_FORMAT);

        //获取时间范围内未开始的项目
        return abutmentCustService.getTestProjId()
            .flatMap(value -> {
                //根据项目id获取条目相关信息(进行中的项目条目是确定的)
                return abutmentCustService
                    .getTestProj(value)
                    .flatMapMany(list -> {
                        if (list.getItemList() == null) {
                            return Mono.empty();
                        }
                        //条目
                        List<ItemDateRes> items = list.getItemList();
                        return Flux.fromIterable(items)
                            .filter(va -> va.getItemName() != null)
                            .filter(va -> !va.getDeviceResList().isEmpty())
                            .flatMap(va -> {
                                //条目
                                TestConfigEntity testConfigEntity = new TestConfigEntity();
                                testConfigEntity.setTestName(va.getItemName());
                                testConfigEntity.setId(va.getExperimentItemId());

                                //记录
                                TestRecordEntity testRecordEntity = new TestRecordEntity();
                                //试验记录名称：
                                testRecordEntity.setName("客户：{" + list.getCustomerName() + "}做的：{" + va.getItemName() + "}试验");
                                testRecordEntity.setTestStartTime(Objects.requireNonNull(DateUtil.stringToDate(va.getExperimentStartTime(), DateUtil.DATE_WITHSECOND_FORMAT)).getTime());
                                testRecordEntity.setTestEndTime(Objects.requireNonNull(DateUtil.stringToDate(va.getExperimentEndTime(), DateUtil.DATE_WITHSECOND_FORMAT)).getTime());
                                testRecordEntity.setConfigId(testConfigEntity.getId());
                                testRecordEntity.setTestName(testConfigEntity.getTestName());
                                testRecordEntity.setTester(va.getPeopleName());
                                testRecordEntity.setTestPeopleId(va.getPeopleIdList());
                                testRecordEntity.setConfigId(va.getId());
                                testRecordEntity.setTestProjId(value);
                                return testConfigService.save(testConfigEntity)
                                    .then(
                                        //判断试验记录是否重复
                                         testRecordService
                                            .createQuery()
                                            .where(TestRecordEntity::getName,testRecordEntity.getName())
                                            .where(TestRecordEntity::getConfigId,testRecordEntity.getConfigId())
                                            .where(TestRecordEntity::getTestProjId,testRecordEntity.getTestProjId())
                                            .fetch()
                                            .hasElements()
                                            .flatMap(aBoolean -> {
                                                if (aBoolean) {
                                                    return Mono.empty();
                                                } else {
                                                    ArrayList<TestEnergyDetailEntity> insetDetail = new ArrayList<>();
                                                    List<ExperimentDeviceRes> deviceResList = va.getDeviceResList();
                                                    return testRecordService.insert(testRecordEntity)
                                                            .then(
                                                                Flux.fromIterable(deviceResList)
                                                                .flatMap(experimentDeviceRes->{
                                                                            TestEnergyDetailEntity testEnergyDetailEntity = new TestEnergyDetailEntity();
                                                                            testEnergyDetailEntity.setTestRecordId(testRecordEntity.getId());
                                                                            testEnergyDetailEntity.setDeviceId(experimentDeviceRes.getDeviceId());
                                                                            testEnergyDetailEntity.setDeviceName(experimentDeviceRes.getDeviceName());
                                                                            insetDetail.add(testEnergyDetailEntity);
                                                                            return Mono.just(insetDetail);
                                                                        })
                                                                .then(testEnergyDetailService.save(insetDetail))
                                                            );
                                                }
                                            })
                                    );
                            });
                    });
            });
    }

    @Operation(summary = "测试预约项目同步")
    @GetMapping("/reservationTestProj")
    public Flux<Object> getreservationData() {

        //获取时间范围内未开始的项目
        return abutmentCustService.getReservationTestId()
            .flatMap(value -> {
                //根据预约项目id获取详细信息
                return abutmentCustService
                    .getReservationTestProj(value)
                    .flatMapMany(list -> {
                        if (list.getItemList() == null) {
                            return Mono.empty();
                        }
                        //条目
                        List<ItemDateRes> items = list.getItemList();
                        return Flux.fromIterable(items)
                            .filter(va -> va.getItemName() != null)
                            .filter(va -> !va.getDeviceResList().isEmpty())
                            .flatMap(va -> {
                                //条目
                                TestConfigEntity testConfigEntity = new TestConfigEntity();
                                testConfigEntity.setTestName(va.getItemName());
                                testConfigEntity.setId(va.getExperimentItemId());

                                //记录
                                TestRecordEntity testRecordEntity = new TestRecordEntity();
                                //试验记录名称：
                                testRecordEntity.setName("客户：{" + list.getCustomerName() + "}预约的：{" + va.getItemName() + "}试验");
                                testRecordEntity.setTestStartTime(Objects.requireNonNull(DateUtil.stringToDate(va.getExperimentStartTime(), DateUtil.DATE_WITHSECOND_FORMAT)).getTime());
                                testRecordEntity.setTestEndTime(Objects.requireNonNull(DateUtil.stringToDate(va.getExperimentEndTime(), DateUtil.DATE_WITHSECOND_FORMAT)).getTime());
                                testRecordEntity.setConfigId(va.getExperimentItemId());
                                testRecordEntity.setTestName(testConfigEntity.getTestName());
                                testRecordEntity.setTester(va.getPeopleName());
                                testRecordEntity.setTestPeopleId(va.getPeopleIdList());
                                testRecordEntity.setTestProjId(value);
                                testRecordEntity.setItemId(va.getId());
                                testRecordEntity.setCancelStatus(list.getCancelStatus());
                                testRecordEntity.setRecordType("1");
                                //判断是否有重复条目
                                TestRecordEntity testRecordEntityEmpty = new TestRecordEntity();
                                return testConfigService.findById(testConfigEntity.getId())
                                    .hasElement()
                                    .flatMap(aBoolean -> {
                                        if(!aBoolean){
                                            return testConfigService.insert(testConfigEntity);
                                        }
                                        return Mono.just(-1);
                                    })
                                    .then(
                                        //判断试验记录是否重复

                                        testRecordService
                                            .createQuery()
                                            .where(TestRecordEntity::getName,testRecordEntity.getName())
                                            .where(TestRecordEntity::getItemId,testRecordEntity.getItemId())
                                            .where(TestRecordEntity::getTestProjId,testRecordEntity.getTestProjId())
                                            .fetchOne()
                                            .switchIfEmpty(Mono.just(testRecordEntityEmpty))
                                            .flatMap(record -> {
                                                if (record != null) {
                                                    //如果有记录则更新


                                                    //第一步删除该记录对应的设备
                                                    //第二步更新记录
                                                    //第三步插入记录对应的设备
                                                    List<ExperimentDeviceRes> deviceResList = va.getDeviceResList();
                                                    ArrayList<TestEnergyDetailEntity> insetDetail = new ArrayList<>();
                                                    return testEnergyDetailService
                                                        .createDelete()
                                                        .where(TestEnergyDetailEntity::getTestRecordId,record.getId())
                                                        .execute()
                                                        .then(
                                                            testRecordService
                                                                .updateById(record.getId(),testRecordEntity)
                                                                .then(
                                                                    Flux.fromIterable(deviceResList)
                                                                        .flatMap(experimentDeviceRes->{
                                                                            TestEnergyDetailEntity testEnergyDetailEntity = new TestEnergyDetailEntity();
                                                                            testEnergyDetailEntity.setTestRecordId(testRecordEntity.getId());
                                                                            testEnergyDetailEntity.setDeviceId(experimentDeviceRes.getDeviceId());
                                                                            testEnergyDetailEntity.setDeviceName(experimentDeviceRes.getDeviceName());
                                                                            insetDetail.add(testEnergyDetailEntity);
                                                                            return Mono.just(insetDetail);
                                                                        })
                                                                        .then(testEnergyDetailService.save(insetDetail))
                                                                )
                                                        );
                                                } else {
                                                    ArrayList<TestEnergyDetailEntity> insetDetail = new ArrayList<>();
                                                    List<ExperimentDeviceRes> deviceResList = va.getDeviceResList();
                                                    return testRecordService.insert(testRecordEntity)
                                                        .then(
                                                            Flux.fromIterable(deviceResList)
                                                                .flatMap(experimentDeviceRes->{
                                                                    TestEnergyDetailEntity testEnergyDetailEntity = new TestEnergyDetailEntity();
                                                                    testEnergyDetailEntity.setTestRecordId(testRecordEntity.getId());
                                                                    testEnergyDetailEntity.setDeviceId(experimentDeviceRes.getDeviceId());
                                                                    testEnergyDetailEntity.setDeviceName(experimentDeviceRes.getDeviceName());
                                                                    insetDetail.add(testEnergyDetailEntity);
                                                                    return Mono.just(insetDetail);
                                                                })
                                                                .then(testEnergyDetailService.save(insetDetail))
                                                        );
                                                }
                                            })
                                    );
                            });
                    });
            });
    }

    /**
     * 预估未来一个月的试验能耗
     */

    @GetMapping("/getEstimateTest")
    @Operation(summary = "测试预估未来一个月试验能耗")
    public Mono<Map> estimateTest(){
        long startTime = DateUtil.getCurrentDateTime().getTime();
        //一个月后的时间
        String s = DateUtil.addMonth(DateUtil.getCurrentDateTime(), 1);
        long endTime = DateUtil.stringToDate(s, DateUtil.DATE_WITHSECOND_FORMAT).getTime();

        //获取未来一个月的预约数据
        return testRecordService
            .createQuery()
            .where(TestRecordEntity::getRecordType,"1")
            .where(TestRecordEntity::getCancelStatus,"0")
            .gte(TestRecordEntity::getTestStartTime,startTime)
            .lte(TestRecordEntity::getTestEndTime,endTime)
            .or()
            .gte(TestRecordEntity::getTestEndTime,startTime)
            .lte(TestRecordEntity::getTestEndTime,endTime)
            .fetch()
            .collect(Collectors.groupingBy(TestRecordEntity::getConfigId))
            .flatMap(ma->{
                return this.getLastTest()
                    .collectList()
                    .flatMap(list->{
                        Map<String, TestRecordEnergyRes> collect = list.stream().collect(Collectors.toMap(TestRecordEnergyRes::getConfigId, Function.identity()));
                        HashMap<String, BigDecimal> resultMap = new HashMap<>();
                        BigDecimal  estimateElectricity =BigDecimal.ZERO;
                        BigDecimal estimateWater =BigDecimal.ZERO;
                        BigDecimal estimateGas =BigDecimal.ZERO;
                        for (Map.Entry<String, List<TestRecordEntity>> stringListEntry : ma.entrySet()) {
                            if(collect.get(stringListEntry.getKey())!=null) {
                                //计算能耗
                                //未莱一个月做了多少次这个条目的试验
                                int size = stringListEntry.getValue().size();
                                TestRecordEnergyRes testRecordEnergyRes = collect.get(stringListEntry.getKey());
                                //预估电能耗
                                 estimateElectricity = estimateElectricity.add(testRecordEnergyRes.getElectricityEnergy().multiply(BigDecimal.valueOf(size))) ;
                                //预估水
                                estimateWater = estimateWater.add(testRecordEnergyRes.getWaterEnergy().multiply(BigDecimal.valueOf(size)));
                                //预估气
                                 estimateGas = estimateGas.add(testRecordEnergyRes.getElectricityEnergy().multiply(BigDecimal.valueOf(size)));
                            }
                        }
                        resultMap.put("electricity",estimateElectricity);
                        resultMap.put("water",estimateWater);
                        resultMap.put("gas",estimateGas);
                        return Mono.just(resultMap);
                    });
            });
    }



    /**
     * 获取过去各条目的能耗
     * @return
     */
    public Flux<TestRecordEnergyRes> getLastTest(){
        //1.获取过去所有试验记录的能耗
        return testRecordService
            .createQuery()
            .where(TestRecordEntity::getRecordType,"0")
            .fetch()
            .flatMap(record->{
                TestRecordEnergyRes testRecordEnergyRes = new TestRecordEnergyRes();
                testRecordEnergyRes.setRecordId(record.getId());
                testRecordEnergyRes.setConfigId(record.getConfigId());
                testRecordEnergyRes.setConfigName(record.getTestName());
                return testEnergyDetailService
                    .createQuery()
                    .where(TestEnergyDetailEntity::getTestRecordId,record.getId())
                    .fetch()
                    .collectList()
                    .flatMapMany(list->{
                        //电
                        BigDecimal electricityEnergy = list.stream().map(TestEnergyDetailEntity::getElectricity).reduce(BigDecimal.ZERO, BigDecimal::add);
                        testRecordEnergyRes.setElectricityEnergy(electricityEnergy);
                        //水
                        BigDecimal waterEnergy = list.stream().map(TestEnergyDetailEntity::getWater).reduce(BigDecimal.ZERO, BigDecimal::add);
                        testRecordEnergyRes.setWaterEnergy(waterEnergy);
                        //气
                        BigDecimal gasEnergy = list.stream().map(TestEnergyDetailEntity::getGas).reduce(BigDecimal.ZERO, BigDecimal::add);
                        testRecordEnergyRes.setGasEnergy(gasEnergy);
                        return Mono.just(testRecordEnergyRes);
                    });

            }).collectList()
            .flatMapMany(value->{
                //按条目分组
                TestRecordEnergyRes testRecordEnergyRes ;
                ArrayList<TestRecordEnergyRes> result = new ArrayList<>();

                Map<String, List<TestRecordEnergyRes>> collectMapByItem = value.stream().collect(Collectors.groupingBy(TestRecordEnergyRes::getConfigId));
                for (Map.Entry<String, List<TestRecordEnergyRes>> stringListEntry : collectMapByItem.entrySet()) {
                    testRecordEnergyRes = new TestRecordEnergyRes();
                    testRecordEnergyRes.setConfigId(stringListEntry.getKey());
                    int size = stringListEntry.getValue().size();
                    BigDecimal lastTotalElectricity = stringListEntry.getValue().stream().map(TestRecordEnergyRes::getElectricityEnergy).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal lastTotalWater = stringListEntry.getValue().stream().map(TestRecordEnergyRes::getWaterEnergy).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal lastTotalGas = stringListEntry.getValue().stream().map(TestRecordEnergyRes::getGasEnergy).reduce(BigDecimal.ZERO, BigDecimal::add);

                    testRecordEnergyRes.setElectricityEnergy(lastTotalElectricity.divide(BigDecimal.valueOf(size),2, RoundingMode.HALF_UP));
                    testRecordEnergyRes.setWaterEnergy(lastTotalWater.divide(BigDecimal.valueOf(size),2, RoundingMode.HALF_UP));
                    testRecordEnergyRes.setGasEnergy(lastTotalGas.divide(BigDecimal.valueOf(size),2, RoundingMode.HALF_UP));
                    result.add(testRecordEnergyRes);
                }
                return Flux.fromIterable(result);
            });
    }



    @Operation(summary = "同步区域")
    @GetMapping("/testRegion")
    public Mono<Void>  abuntData() {
        Flux<LocationRes> deviceLocation = service.getDeviceLocation();
        return deviceLocation
            .flatMap(value -> {
                AreaInfoEntity areaInfoEntity = new AreaInfoEntity();
                if (value.getLocationPid() != null) {
                    areaInfoEntity.setParentId(value.getLocationPid());
                }
                areaInfoEntity.setId(value.getLocationId());
                areaInfoEntity.setAreaName(value.getLocationName());
                areaInfoEntity.setAddr(value.getDetailedAddress());
                areaInfoEntity.setState(value.getEnable().toString().equals("1") ? "0" : "1");
                return Mono.just(areaInfoEntity);
            })
            .collectList()
            .flatMapMany(list -> {
                return Flux.fromIterable(TreeSupportEntity.list2tree(list, AreaInfoEntity::setChildren));
            })
            .flatMap(value -> {

                return areaInfoService.save(value);

            }).then();
    }


//    @Operation(summary = "更新设备信息")
//    @GetMapping("/updateDeviceInfo")
//    public Flux<Object> updateDeviceInfo(){
//        //获取设备生命周期的设备
//        return service.getDeviceInfo(new EquipmentReq())
//            .flatMap(device->{
//                //获取设备负责人
//                if(device.getHead()!=null){
//                    UserEntity userEntity = new UserEntity();
//                    return userService
//                        .findByUsername(device.getHead())
//                        .switchIfEmpty(Mono.just(userEntity))
//                        .flatMap(value->{
//                            if(value.getName() != null){
//                                device.setHead(value.getName());
//                                return Mono.just(device);
//                            }else {
//                                return Mono.just(device);
//                            }
//                        });
//
//                }else {
//                    return Mono.just(device);
//                }
//            }).flatMap(device->{
//                return deviceService
//                    .createUpdate()
//                    //设备名称
//                    .set(DeviceInfoEntity::getDeviceName,device.getEquipmentName())
//                    //设备负责人
//                    .set(DeviceInfoEntity::getDuty,device.getHead())
//                    //设备位置
//                    .set(DeviceInfoEntity::getAreaId,device.getEquipmentLocation())
//                    //出厂编码
//                    .set(DeviceInfoEntity::getFactoryNumber,device.getFactoryCode())
//                    .where(DeviceInfoEntity::getDeviceId,device.getEquipmentCode())
//                    .execute();
//            });
//    }

}
