package org.jetlinks.pro.sems.abutment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.pro.sems.abutment.req.EquipmentReq;
import org.jetlinks.pro.sems.abutment.req.MessageContentReq;
import org.jetlinks.pro.sems.abutment.req.VXSendReq;
import org.jetlinks.pro.sems.abutment.res.*;
import org.jetlinks.pro.sems.entity.req.ThreePresentReq;
import org.jetlinks.pro.sems.iot.*;
import org.jetlinks.pro.sems.entity.AlarmRecordsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class abutmentService {

    private static HttpClient httpClient = HttpClientBuilder.create().build();
    private final static Map<String, Object> map = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(IotService.class);

    @Value("${systemIp.main:10.130.179.17}")
    private String ip;


    private JSONObject getTokenPost() {

        String url = "http://"+ip+"/api/auth/oauth/getToken";
        HttpRequest request = new SimpleHttpRequest(url, httpClient);

        HashMap<String, Object> param = new HashMap<>();
        param.put("client_id","ruoyi");
        param.put("client_secret","123456");
        param.put("grant_type","client_credentials");
        request.params(param);
        try {
            Response response = request.get();
            return JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }
    public void assertToken() {
            JSONObject token = getTokenPost();
            if (token.getInteger("code") == 200) {
                map.put("token", token.getString("access_token"));
            } else {
                throw new ServiceException("请求失败：" + token.getString("access_token"));
            }

    }
    public String createToken() {
        String url = "http://"+ip+"/api/auth/oauth/getToken";
        HttpRequest request = new SimpleHttpRequest(url, httpClient);

        HashMap<String, Object> param = new HashMap<>();

        param.put("client_secret","123456");
        param.put("client_id","ruoyi");
        param.put("grant_type","client_credentials");
        request.params(param);
        Response response = null;
        try {
            response = request.get();
            JSONObject object = JSONObject.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            Object token = object.get("access_token");
            return token.toString();
        } catch (IOException e) {
            log.error("创建openAPI token失败", e);
        }
        return null;
    }

    public JSONObject post(String api, String body) {
        assertToken();
        String url = "http://"+ip + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);
        request.header("Authorization", "Bearer "+map.get("token").toString());
        request.requestBody(body);
        try {
            Response response = request.post();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            log.info("请求结果："+res);
            if (400001 == res.getInteger("code")) {
                request.header("Authorization", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("code") == 500) {
               throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public JSONObject postStatus(String api, String body) {
        assertToken();
        String url = "http://"+ip + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);
        request.header("Authorization", "Bearer "+map.get("token").toString());
        request.requestBody(body);
        try {
            Response response = request.post();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            log.info("请求结果："+res);
            if (400001 == res.getInteger("status")) {
                request.header("Authorization", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("status") == 500) {
                throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public JSONObject postGetInfo(String api, String body) {
        String url = "http://"+ip + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);
       request.header("Authorization", body);
        HashMap<String, Object> map = new HashMap<>();
        map.put("moduleCode","sems");
        request.params(map);
        try {
            Response response = request.get();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            if (400001 == res.getInteger("code")) {
                request.header("Authorization", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("code") == 500) {
                throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }



    public JSONObject getMenu(String api, String accessToken) {
        String url = "http://"+ip+":8080" + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);

        request.header("Authorization", accessToken);
        try {
            Response response = request.get();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            if (400001 == res.getInteger("code")) {
                request.header("Authorization", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("code") == 500) {
//                throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public JSONObject get(String api, Map<String,Object> params) {
        assertToken();
        String url = "http://"+ip + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);

        request.header("Authorization", "Bearer "+map.get("token").toString());
        request.params(params);
        try {
            Response response = request.get();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            if (400001 == res.getInteger("code")) {
                request.header("Authorization", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("code") == 500) {
//                throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    //根据角色id获取菜单权限
    public JSONObject getMenuByRoleId(String api,String token) {
        //assertToken();
        String url = "http://"+ip + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);

        request.header("Authorization", token);
        try {
            Response response = request.get();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            if (400001 == res.getInteger("code")) {
                request.header("Authorization", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("code") == 500) {
//                throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public JSONObject getLocationNoParams(String api) {
        assertToken();
        String url = "http://"+ip+ api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);
        request.header("Authorization","Bearer "+map.get("token").toString() );
        try {
            Response response = request.get();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            if (400001 == res.getInteger("code")) {
                request.header("Authorization", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("code") == 500) {
//                throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public JSONObject postDevice(String api, String body) {
        assertToken();
        String url = "http://"+ip + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);
        request.header("Authorization", "Bearer "+map.get("token").toString());
        request.requestBody(body);
        try {
            Response response = request.post();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            if (400001 == res.getInteger("code")) {
                request.header("Authorization", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("code") == 500) {
                //throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public JSONObject postDeviceType(String api, String body) {
        assertToken();
        String url = "http://"+ip + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);
        request.header("Authorization", "Bearer "+map.get("token").toString());
        request.requestBody(body);
        try {
            Response response = request.get();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            if (400001 == res.getInteger("code")) {
                request.header("Authorization", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("code") == 500) {
                //throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }


    /**
     * 获取设备台账
     */

    public synchronized Flux<EquipmentListRes> getDeviceInfo(EquipmentReq equipmentReq){
        String api="/api/elm/api/equipment/list";
        //去掉删除的
        equipmentReq.setEnable(1);
        JSONObject post = postDevice(api,JSON.toJSONString(equipmentReq));
        if(post.get("rows")==null){
            ArrayList<EquipmentListRes> equipmentListRes = new ArrayList<>();
            return Flux.fromIterable(equipmentListRes);
        }
        List<EquipmentListRes> equipmentListResList = JSON.parseArray(post.get("rows").toString(), EquipmentListRes.class);
        if(equipmentListResList==null || equipmentListResList.isEmpty() ){
            ArrayList<EquipmentListRes> equipmentListRes = new ArrayList<>();
            return Flux.fromIterable(equipmentListRes);
        }
        return Flux.fromIterable(equipmentListResList) ;
    }

    /**
     * 获取设备类型
     */

    public synchronized  Flux<EquipTypeListRes> getDeviceType(){
        String api="/api/elm/api/equipType/list";
        JSONObject post = postDeviceType(api,null );

        if(post.get("data")==null){
            ArrayList<EquipTypeListRes> equipmentListRes = new ArrayList<>();
            return Flux.fromIterable(equipmentListRes);
        }
        List<EquipTypeListRes> equipmentTypeResList = JSON.parseArray(post.get("data").toString(), EquipTypeListRes.class);
        if(equipmentTypeResList==null || equipmentTypeResList.isEmpty()){
            ArrayList<EquipTypeListRes> equipmentListRes = new ArrayList<>();
            return Flux.fromIterable(equipmentListRes);
        }

        return Flux.fromIterable(equipmentTypeResList) ;
    }

    /**
     * 获取设备位置
     */

    public Flux<LocationRes> getDeviceLocation(){
        String api="/api/elm/api/location/list";
        JSONObject locationObject = getLocationNoParams(api);
        if(locationObject.get("data")==null){
            ArrayList<LocationRes> locationRes = new ArrayList<>();
            return Flux.fromIterable(locationRes);
        }
        List<LocationRes> data = JSON.parseArray(locationObject.get("data").toString(), LocationRes.class);
        if(data==null || data.isEmpty()){
            ArrayList<LocationRes> locationRes = new ArrayList<>();
            return Flux.fromIterable(locationRes);
        }
        return Flux.fromIterable(data);
    }

    /**
     * 消息推送
     */

    public Mono<Object> messagePush(MessageContentReq messageContentReq){
        String api="/api/message/sendInnerMessage";
        if("0".equals(messageContentReq.getAlarmType())){
            JSONObject post = post(api, JSON.toJSONString(messageContentReq));
            return Mono.just(post);
        }
        return Mono.empty();
    }


    public Flux<UserReturnRes> getUserList(){
        String api="/api/system/user/noPageList";
        JSONObject jsonObject = get(api, new HashMap<>());
        List<UserReturnRes> userReturnResList = JSON.parseArray(JSON.toJSONString(jsonObject.get("data")), UserReturnRes.class);
        return Flux.fromIterable(userReturnResList);
    }


    /**
     * 获取用户列表id
     * @return
     */
    public List<String> getDeleteUserIdList(){
        String api="/api/system/user/noPageList";
        JSONObject jsonObject = get(api, new HashMap<>());
        List<UserReturnRes> userReturnResList = JSON.parseArray(JSON.toJSONString(jsonObject.get("data")), UserReturnRes.class);
        List<String> userIds = userReturnResList.stream().filter(i->i.getDelFlag().equals("1")).map(UserReturnRes::getUserId).collect(Collectors.toList());
        return userIds;
    }

    public Flux<UserReturnRes> getUserByName(String userName){
        String api="/api/system/user/noPageList";
        HashMap<String, Object> map = new HashMap<>();
        map.put("userName",userName);
        JSONObject jsonObject = get(api, map);
        List<UserReturnRes> userReturnResList = JSON.parseArray(JSON.toJSONString(jsonObject.get("data")), UserReturnRes.class);
        return Flux.fromIterable(userReturnResList);
    }

    public Flux<DeptReturnRes> getDeptList(){
        String api="/api/system/dept/list";
        JSONObject jsonObject = get(api, new HashMap<>());
        List<DeptReturnRes> userReturnResList = JSON.parseArray(JSON.toJSONString(jsonObject.get("data")), DeptReturnRes.class);
        return Flux.fromIterable(userReturnResList);
    }

    /**
     * 获取消息模板
     * @return
     */
    public List<VXTemplateRes> getTemplate(AlarmRecordsEntity alarmRecord,Integer type){
        //根据不同的告警获取模板
        String name=null;
            if("0".equals(alarmRecord.getAlarmType())){
                //按设备
                if(!alarmRecord.getRuleType().isEmpty()){
                    name="设备功率超标告警";
                }else {
                        name="设备能耗超标告警事件";
                }
            }else if("1".equals(alarmRecord.getAlarmType())) {
                //按试验
                    name="试验能耗超标告警事件";

            }else {
                    name="试验场所能耗超标告警";
            }
        String api="/api/message/messageTemplate/query";
        VXSendReq vxSendReq = new VXSendReq();
        vxSendReq.setAppSn("EEM");
        vxSendReq.setSendChannel(type);
        vxSendReq.setName(name);
        JSONObject result = postStatus(api, JSONObject.toJSONString(vxSendReq));
        List<VXTemplateRes> vxTemplateRes = JSON.parseArray(JSON.toJSONString(result.get("data")), VXTemplateRes.class);
        return vxTemplateRes;
    }


    public Mono<JSONObject> senVxMesssage(VXSendReq vxSendReq){
        String api="/api/message/messageTemplate/sendChannelMessage";
        JSONObject result = postStatus(api, JSONObject.toJSONString(vxSendReq));
        return Mono.just(result);
    }





    //创建告警待办
    public Mono<Object> createAlarmWait(ThreePresentReq req){
        String api="/api/elm/threePreview/alarmTodo/save";
        JSONObject jsonObject = post(api, JSON.toJSONString(req));;
        return Mono.just(jsonObject);
    }
    //删除告警待办
    public Mono<Object>  delAlarmWait(ThreePresentReq req){
        String api="/api/elm/threePreview/alarmTodo/delete/"+req.getCode();
        JSONObject jsonObject = get(api, new HashMap<>());
        return Mono.just(jsonObject);
    }
    //创建告警台账
    public Mono<Object>  createAlarmLedger(ThreePresentReq req){
        String api="/api/elm/threePreview/alarmRecord/save";
        JSONObject jsonObject = post(api, JSON.toJSONString(req));
        return Mono.just(jsonObject);
    }
    //修改告警台账
    public Mono<Object>  alterAlarmLedger(ThreePresentReq req){
        String api="/api/elm/threePreview/alarmRecord/update";
        JSONObject jsonObject = post(api, JSON.toJSONString(req));;
        return Mono.just(jsonObject);
    }
}
