//package org.jetlinks.project.busi.task;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.jetlinks.pro.TimerSpec;
//import org.jetlinks.pro.cluster.reactor.FluxCluster;
//import org.jetlinks.project.busi.abutment.AbutmentCustService;
//import org.jetlinks.project.busi.abutment.res.ExperimentDeviceRes;
//import org.jetlinks.project.busi.abutment.res.ItemDateRes;
//import org.jetlinks.project.busi.entity.TestConfigEntity;
//import org.jetlinks.project.busi.entity.TestEnergyDetailEntity;
//import org.jetlinks.project.busi.entity.TestRecordEntity;
//import org.jetlinks.project.busi.service.TestConfigService;
//import org.jetlinks.project.busi.service.TestEnergyDetailService;
//import org.jetlinks.project.busi.service.TestRecordService;
//import org.jetlinks.project.busi.utils.DateUtil;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import reactor.core.Disposable;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import javax.annotation.PreDestroy;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class TestDataTask  implements CommandLineRunner {
//
//    private final AbutmentCustService abutmentCustService;
//
//    private final TestConfigService testConfigService;
//
//    private final TestRecordService testRecordService;
//
//    private final TestEnergyDetailService testEnergyDetailService;
//
//    public Flux<Object> getTestData() {
//        //时间控制
//        //当前时间定时任务时间
//        String todayStartTime = DateUtil.dateToString(DateUtil.getTodayStartTime(),DateUtil.DATE_SHORT_FORMAT);
//        String todayEndTime = DateUtil.dateToString(DateUtil.getTodayEndTime(),DateUtil.DATE_SHORT_FORMAT);
//
//        //获取时间范围内未开始的项目
//        return abutmentCustService.getTestProjId()
//            .flatMap(value -> {
//                //根据项目id获取条目相关信息(进行中的项目条目是确定的)
//                return abutmentCustService
//                    .getTestProj(value)
//                    .flatMapMany(list -> {
//                        if (list.getItemList() == null) {
//                            return Mono.empty();
//                        }
//                        //条目
//                        List<ItemDateRes> items = list.getItemList();
//                        return Flux.fromIterable(items)
//                            .filter(va -> va.getItemName() != null)
//                            .filter(va -> !va.getDeviceResList().isEmpty())
//                            .flatMap(va -> {
//                                //条目
//                                TestConfigEntity testConfigEntity = new TestConfigEntity();
//                                testConfigEntity.setTestName(va.getItemName());
//                                testConfigEntity.setId(va.getId());
//
//                                //记录
//                                TestRecordEntity testRecordEntity = new TestRecordEntity();
//                                //试验记录名称：
//                                testRecordEntity.setName("客户：{" + list.getCustomerName() + "}做的：{" + va.getItemName() + "}试验");
//                                testRecordEntity.setTestStartTime(Objects.requireNonNull(DateUtil.stringToDate(va.getExperimentStartTime(), DateUtil.DATE_WITHSECOND_FORMAT)).getTime());
//                                testRecordEntity.setTestEndTime(Objects.requireNonNull(DateUtil.stringToDate(va.getExperimentEndTime(), DateUtil.DATE_WITHSECOND_FORMAT)).getTime());
//                                testRecordEntity.setConfigId(testConfigEntity.getId());
//                                testRecordEntity.setTestName(testConfigEntity.getTestName());
//                                testRecordEntity.setTester(va.getPeopleName());
//                                testRecordEntity.setTestPeopleId(va.getPeopleIdList());
//                                testRecordEntity.setConfigId(va.getId());
//                                testRecordEntity.setTestProjId(value);
//                                return testConfigService.save(testConfigEntity)
//                                    .then(
//                                        //判断试验记录是否重复
//                                        testRecordService
//                                            .createQuery()
//                                            .where(TestRecordEntity::getName,testRecordEntity.getName())
//                                            .where(TestRecordEntity::getConfigId,testRecordEntity.getConfigId())
//                                            .where(TestRecordEntity::getTestProjId,testRecordEntity.getTestProjId())
//                                            .fetch()
//                                            .hasElements()
//                                            .flatMap(aBoolean -> {
//                                                if (aBoolean) {
//                                                    return Mono.empty();
//                                                } else {
//                                                    ArrayList<TestEnergyDetailEntity> insetDetail = new ArrayList<>();
//                                                    List<ExperimentDeviceRes> deviceResList = va.getDeviceResList();
//                                                    return testRecordService.insert(testRecordEntity)
//                                                        .then(
//                                                            Flux.fromIterable(deviceResList)
//                                                                .flatMap(experimentDeviceRes->{
//                                                                    TestEnergyDetailEntity testEnergyDetailEntity = new TestEnergyDetailEntity();
//                                                                    testEnergyDetailEntity.setTestRecordId(testRecordEntity.getId());
//                                                                    testEnergyDetailEntity.setDeviceId(experimentDeviceRes.getDeviceId());
//                                                                    testEnergyDetailEntity.setDeviceName(experimentDeviceRes.getDeviceName());
//                                                                    insetDetail.add(testEnergyDetailEntity);
//                                                                    return Mono.just(insetDetail);
//                                                                })
//                                                                .then(testEnergyDetailService.save(insetDetail))
//                                                        );
//                                                }
//                                            })
//                                    );
//                            });
//                    });
//            });
//    }
//
//    private Disposable disposable;
//
//    @PreDestroy
//    public void shutdown() {
//        //停止定时任务
//        if (disposable != null) {
//            disposable.dispose();
//        }
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        disposable = null;
////            FluxCluster
////                //不同的任务名不能相同
////                .schedule("test_data_task", TimerSpec.cron("0 0 0/3 * * ?"), Flux.defer(this::getTestData));
//    }
//}
