//package org.jetlinks.project.busi.task;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.hswebframework.web.api.crud.entity.QueryParamEntity;
//import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
//import org.hswebframework.web.i18n.LocaleUtils;
//import org.hswebframework.web.system.authorization.api.entity.UserEntity;
//import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
//import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
//import org.jetlinks.pro.TimerSpec;
//import org.jetlinks.pro.assets.AssetsHolder;
//import org.jetlinks.pro.auth.assets.OrganizationAssetType;
//import org.jetlinks.pro.auth.entity.OrganizationEntity;
//import org.jetlinks.pro.auth.entity.OrganizationInfo;
//import org.jetlinks.pro.auth.entity.RoleInfo;
//import org.jetlinks.pro.auth.entity.UserDetail;
//import org.jetlinks.pro.auth.service.OrganizationService;
//import org.jetlinks.pro.auth.service.RoleService;
//import org.jetlinks.pro.auth.service.UserDetailService;
//import org.jetlinks.pro.auth.service.request.SaveUserRequest;
//import org.jetlinks.pro.cluster.reactor.FluxCluster;
//import org.jetlinks.project.busi.abutment.abutmentService;
//import org.jetlinks.project.busi.entity.UserAndVxUserIdEntity;
//import org.jetlinks.project.busi.service.UserAndVxUserIdService;
//import org.jetlinks.project.busi.utils.DateUtil;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.StringUtils;
//import reactor.core.Disposable;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import javax.annotation.PreDestroy;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.List;
//import java.util.function.Function;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class UserAndDeptTask implements CommandLineRunner {
//
//    private final abutmentService service;
//
//    private final ReactiveUserService reactiveUserService;
//
//    private final OrganizationService organizationService;
//
//    private final UserDetailService userDetailService;
//
//    private final UserAndVxUserIdService userAndVxUserIdService;
//
//    private final RoleService roleService;
//
//    private final ApplicationEventPublisher eventPublisher;
//
//    @Transactional
//    public  Flux<Object> abuntDeptAndUser(){
//        ArrayList<String> userList = new ArrayList<>();
//        userList.add("311");
//        userList.add("315");
//        userList.add("313");
//        userList.add("317");
//        userList.add("319");
//        userList.add("335");
//        userList.add("259");
//        userList.add("321");
//        log.info("开始同步用户！");
//        //先删除我们这边的组织
//        //1.先删组织
//        return organizationService
//            .createDelete()
//            .execute()
//            .flatMapMany(intNumber->{
//                //删除成功，插入组织
//                QueryParamEntity queryParamEntity = new QueryParamEntity();
//                queryParamEntity.setParallelPager(false);
//                queryParamEntity.setPaging(false);
//                return service.getDeptList()
//                    .flatMap(dept->{
//                        //转换数据
//                        OrganizationEntity organizationEntity = new OrganizationEntity();
//                        organizationEntity.setId(String.valueOf(dept.getDeptId()));
//                        organizationEntity.setName(dept.getDeptName());
//                        if(dept.getParentId()!=0){
//                            organizationEntity.setParentId(String.valueOf(dept.getParentId()));
//                        }
//
//                        return Mono.just(organizationEntity);
//                    })
//                    .collectList()
//                    .flatMapMany(list->{
//                        log.info("同步过来的部门原始数据："+list);
//                        return Flux.fromIterable(TreeSupportEntity.list2tree(list, OrganizationEntity::setChildren)) ;
//                    })
//                    .doOnNext(value->log.info("转换之后的数据:"+value))
//                    .concatMap(organizationService::save)
//                    .doOnNext(value->log.info("保存组织结果："+value))
//                    .thenMany(
//                        reactiveUserService.findUser(queryParamEntity)
//                            .filter(user->!"admin".equals(user.getUsername()))
//                            .map(UserEntity::getId)
//                            .concatMap(id->{
//                                List<String> deleteUserIdList = service.getDeleteUserIdList();
//                                log.info("需要删除的用户:"+deleteUserIdList);
//                                if(deleteUserIdList.contains(id)){
//                                    return reactiveUserService.deleteUser(id);
//                                }
//                                return Mono.empty();
//                            })
//                            .count()
//                            .flatMapMany(tag-> {
//                                //插入用户
//                                return service.getUserList()
//                                    .flatMap(user -> {
//                                        if (user.getUserId().equals("1") || user.getUserId().equals("217") || user.getUserId().equals("215")) {
//                                            return Mono.empty();
//                                        }
//                                        ArrayList<OrganizationInfo> organizationInfos = new ArrayList<>();
//                                        //数据转换
//                                        UserDetail userDetail = new UserDetail();
//                                        userDetail.setId(user.getUserId());
//                                        userDetail.setName(user.getNickName());
//                                        userDetail.setUsername(user.getUserName());
//                                        userDetail.setPassword("Admin123@");
//                                        userDetail.setEmail(user.getEmail());
//                                        userDetail.setTelephone(user.getPhonenumber());
//                                        userDetail.setStatus(user.getStatus().equals("0") ? Byte.valueOf("1") : Byte.valueOf("0"));
//                                        userDetail.setCreateTime(DateUtil.stringToDate(user.getCreateTime(), DateUtil.DATE_WITHSECOND_FORMAT).getTime());
//                                        userDetail.setCreatorName(user.getCreateBy());
//                                        //绑定组织
//                                        OrganizationInfo organizationInfo = new OrganizationInfo();
//                                        organizationInfo.setId(user.getDeptId());
//                                        organizationInfos.add(organizationInfo);
//                                        userDetail.setOrgList(organizationInfos);
//                                        //绑定默认角色
//                                        ArrayList<RoleInfo> roleInfos = new ArrayList<>();
//                                        RoleInfo roleInfo = new RoleInfo();
//                                        if (userList.contains(user.getUserId())) {
//                                            roleInfo.setId("1727590869686657024");
//                                            roleInfo.setName("基础角色");
//                                            roleInfos.add(roleInfo);
//                                        }
//                                        userDetail.setRoleList(roleInfos);
//                                        log.info("组装用户列表数据："+userDetail);
//                                        //保存用户企业微信id
//                                        if (user.getWxUserId() != null && !"".equals(user.getWxUserId())) {
//                                            UserAndVxUserIdEntity userAndVxUserIdEntity = new UserAndVxUserIdEntity();
//                                            userAndVxUserIdEntity.setUserId(user.getUserId());
//                                            userAndVxUserIdEntity.setVxUserId(user.getWxUserId());
//                                            return userAndVxUserIdService
//                                                .createQuery()
//                                                .where(UserAndVxUserIdEntity::getUserId, user.getUserId())
//                                                .fetch()
//                                                .hasElements()
//                                                .flatMap(v -> {
//                                                    if (v) {
//                                                        return Mono.just(userDetail);
//                                                    } else {
//                                                        return userAndVxUserIdService
//                                                            .save(userAndVxUserIdEntity)
//                                                            .thenReturn(userDetail);
//                                                    }
//                                                });
//                                        } else {
//                                            return Mono.just(userDetail);
//                                        }
//                                    })
//                                    .concatMap(user -> {
//
//                                        if(user.getUsername().equals("admin") ||user.getUsername().equals("super")){
//                                            user.setStatus(Byte.valueOf("0"));
//                                        }else {
//                                            user.setStatus(user.getStatus());
//                                        }
//
//                                        user.setCreateTime(user.getCreateTime());
//                                        user.setCreatorName(user.getCreatorName());
//
//                                        SaveUserRequest saveUserRequest = new SaveUserRequest();
//                                        saveUserRequest.setUser(user);
//                                        saveUserRequest.setOrgIdList(new HashSet<>(user.getOrgIdList()));
//                                        saveUserRequest.setRoleIdList(new HashSet<>(user.getRoleIdList()));
//                                        log.info("保存用户数据："+saveUserRequest);
//                                        return reactiveUserService.findById(user.getId())
//                                            .hasElement()
//                                            .flatMap(aBoolean -> {
//                                                if(!aBoolean){
//                                                    log.info("没有该用户，插入用户！");
//                                                    return this.assertPermission(saveUserRequest)
//                                                        .then(
//                                                            reactiveUserService
//                                                                .addUser(saveUserRequest.getUser().toUserEntity())
//                                                                .then(Mono.fromSupplier(saveUserRequest.getUser().toUserEntity()::getId))
//                                                                .doOnNext(value->log.info("新增的用户id:"+value))
//                                                                .flatMap(userId -> {
//                                                                    user.setId(userId);
//                                                                    //保存详情
//                                                                    return userDetailService
//                                                                        .save(user.toDetailEntity())
//                                                                        //绑定角色
//                                                                        .then(roleService.bindUser(Collections.singleton(userId), user.getRoleIdList(), Boolean.FALSE))
//                                                                        //绑定机构部门
//                                                                        .then(organizationService.bindUser(Collections.singleton(userId), user.getOrgIdList(), Boolean.FALSE))
//                                                                        .thenReturn(userId)
//                                                                        .doOnNext(value->log.info("新增用户成功，并且绑定了默认角色和部门"));
//                                                                })
//                                                        );
//
//                                                }else {
//                                                    log.info("存在用户，更新！");
//                                                    return this.assertPermission(saveUserRequest)
//                                                        .then(this.saveUser(saveUserRequest));
//                                                }
//                                            });
//                                    });
//
//                            })
//                    );
//            });
//    }
//
//    private Mono<Void> assertPermission(SaveUserRequest request) {
//        return Flux.merge(
//                AssetsHolder
//                    .assertPermission(
//                        Mono.justOrEmpty(request.getOrgIdList())
//                            .flatMapIterable(Function.identity()),
//                        OrganizationAssetType.organization,
//                        Function.identity()
//                    ),
//                AssetsHolder
//                    .assertPermission(
//                        Mono.justOrEmpty(request.getRoleIdList())
//                            .flatMapIterable(Function.identity()),
//                        OrganizationAssetType.role,
//                        Function.identity()
//                    )
//            )
//            .then();
//    }
//
//    @Transactional
//    public Mono<String> saveUser(SaveUserRequest request) {
//        request.validate();
//        UserDetail detail = request.getUser();
//        boolean isUpdate = StringUtils.hasText(detail.getId());
//        UserEntity entity = request.getUser().toUserEntity();
//        return reactiveUserService
//            .saveUser(Mono.just(entity))
//            .then(Mono.fromSupplier(entity::getId))
//            .doOnNext(value->log.info("更新用户id:"+value))
//            .flatMap(userId -> {
//                detail.setId(userId);
//                //保存详情
//                return userDetailService
//                    .save(detail.toDetailEntity())
//                    //绑定角色
//                    .then(roleService.bindUser(Collections.singleton(userId), request.getRoleIdList(),Boolean.FALSE))
//                    //绑定机构部门
//                    .then(organizationService.bindUser(Collections.singleton(userId), request.getOrgIdList(), isUpdate))
//                    .thenReturn(userId)
//                    .doOnNext(value->log.info("新增用户成功，并且绑定了默认角色和部门"));
//            })
//            //禁用上游产生的清空用户权限事件,因为可能会导致重复执行
//            .as(ClearUserAuthorizationCacheEvent::disable)
//            //只执行一次清空用户权限事件
//            .flatMap(userId -> ClearUserAuthorizationCacheEvent.of(userId).publish(eventPublisher).thenReturn(userId))
//            .as(LocaleUtils::transform)
//            .doOnNext(value->log.info("更新用户完成！"));
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
//        disposable =
//            FluxCluster
//                //不同的任务名不能相同
//                .schedule("user_dept_task", TimerSpec.cron("0 40 23 * * ?"), Flux.defer(this::abuntDeptAndUser));
//    }
//}
