//package org.jetlinks.project.busi.authentication;
//
//import lombok.Data;
//
//import org.apache.commons.collections4.CollectionUtils;
//import org.hswebframework.web.authorization.Authentication;
//import org.hswebframework.web.authorization.DefaultDimensionType;
//import org.hswebframework.web.authorization.Dimension;
//import org.hswebframework.web.authorization.Permission;
//import org.hswebframework.web.authorization.simple.SimpleAuthentication;
//import org.hswebframework.web.authorization.simple.SimpleDimension;
//import org.hswebframework.web.authorization.simple.SimplePermission;
//import org.hswebframework.web.authorization.simple.SimpleUser;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Data
//public class ThirdPartyUserInfo {
//    private ThirdUser user;
//
//    // [ user:add,device:* ]
//    private List<String> permissions;
//    private List<String> roles;
//
//
//    private List<Permission> parsePermission() {
//        if (CollectionUtils.isEmpty(permissions)) {
//            return Collections.emptyList();
//        }
//
//        Map<String, Set<String>> permissions = new HashMap<>();
//
//        for (String permission : this.permissions) {
//            String[] arr = permission.split(":");
//            String id = arr[0];
//            Set<String> actions = permissions.computeIfAbsent(id, ignore -> new HashSet<>());
//            if (arr.length > 1) {
//                String[] act = Arrays.copyOfRange(arr, 1, arr.length);
//                actions.addAll(Arrays.asList(act));
//            }
//        }
//        return permissions
//            .entrySet()
//            .stream()
//            .map(e -> SimplePermission
//                .builder()
//                .id(e.getKey())
//                .name(e.getKey())
//                .actions(e.getValue())
//                .build())
//            .collect(Collectors.toList());
//
//    }
//
//    private List<Dimension> parseDimension() {
//        List<Dimension> dimensions = new ArrayList<>();
//        if (CollectionUtils.isNotEmpty(roles)) {
//            dimensions.addAll(
//                roles.stream()
//                     .map(id -> SimpleDimension.of(id, id, DefaultDimensionType.role, Collections.emptyMap()))
//                     .collect(Collectors.toList())
//            );
//
//        }
//
//        return dimensions;
//    }
//
//    public Authentication toAuthentication() {
//        SimpleAuthentication authentication = new SimpleAuthentication();
//        authentication.setUser(SimpleUser
//                                   .builder()
//                                   .id(Objects.nonNull(user)?user.getUserId():"未登录")
//                                   .name(Objects.nonNull(user)?user.getUserId():"未登录")
//                                   .username(Objects.nonNull(user)?user.getUserId():"未登录")
//                                   .userType("third-party")
//                                   .build());
//
//        if(Objects.nonNull(parsePermission())){
//            authentication.setPermissions(parsePermission());
//            authentication.setDimensions(parseDimension());
//        }
//
//        return authentication;
//    }
//}
