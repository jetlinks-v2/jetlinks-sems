package org.jetlinks.pro.sems.iot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.pro.sems.entity.req.GetIotDeviceEnergy;
import org.jetlinks.pro.sems.entity.res.DeviceIotRes;
import org.jetlinks.pro.sems.entity.res.IotProductCategoryRes;
import org.jetlinks.pro.sems.entity.res.IotProductRes;
import org.jetlinks.pro.sems.enums.EnergyType;
import org.jetlinks.pro.sems.iot.util.HeaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IotService {
    private static HttpClient httpClient = HttpClientBuilder.create().build();
    private final static Map<String, Object> map = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(IotService.class);

    @Value("${systemIp.main:192.168.33.64}")
    private String ip;

    private JSONObject getTokenPost() {

        String url = "http://"+ip+":8800/token";
        HttpRequest request = new SimpleHttpRequest(url, httpClient);

        String body = "{\"expires\": -1}";
        request.headers(HeaderUtils.createToken(body, "mmrkNjSHXAGERMi3", "t3bFApKYtyWzbHKQT7tGdM7E"));
        request.requestBody(body);
        try {
            Response response = request.post();
            return JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }
    private void assertToken() {
        if (map.isEmpty()) {
            JSONObject token = getTokenPost();
            if (token.getInteger("status") == 200) {
                map.put("token", token.getString("result"));
            } else {
                throw new ServiceException("请求失败：" + token.getString("result"));
            }
        }
    }
    public String createToken() {
        String url = "http://"+ip+":8800/token";
        HttpRequest request = new SimpleHttpRequest(url, httpClient);

        String body = "{\"expires\": -1}";
        request.headers(HeaderUtils.createToken(body, "mmrkNjSHXAGERMi3", "t3bFApKYtyWzbHKQT7tGdM7E"));
        request.requestBody(body);
        Response response = null;
        try {
            response = request.post();
            JSONObject object = JSONObject.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            Object token = object.get("result");
            map.put("token", token);
            return token.toString();
        } catch (IOException e) {
            log.error("创建openAPI token失败", e);
        }
        return null;
    }

    public JSONObject post(String api, String body) {
        assertToken();
        String url = "http://"+ip+":8800" + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);
        request.header("X-Access-Token", map.get("token").toString());
        request.requestBody(body);
        try {
            Response response = request.post();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            if (401 == res.getInteger("status")) {
                request.header("X-Access-Token", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("status") == 500) {
//                throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }
    public JSONObject get(String api, Map params) {
        assertToken();
        String url = "http://"+ip+":8800" + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);

        request.header("X-Access-Token", map.get("token").toString());
        request.params(params);
        try {
            Response response = request.get();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            if (401 == res.getInteger("status")) {
                request.header("X-Access-Token", createToken());
                Response response1 = request.get();
                return JSON.parseObject(String.valueOf(JSON.parse(response1.asBytes())));
            }
            if (res.getInteger("status") == 500) {
//                throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 1。从iot获取所有设备列表
     * 2.查询指定设备信息
     * {  "terms": [
     *     {
     *       "column": "id",
     *       "value": "30069962",
     *       "termType": "eq"
     *     }
     *   ]}
     * @return
     */
    public List<DeviceIotRes> getDeviceIds(QueryParamEntity query){
        String api = "/device/instance/_query/no-paging";
        JSONObject res = post(api, JSON.toJSONString(query));
        List<DeviceIotRes> result = JSON.parseArray(res.getString("result"), DeviceIotRes.class);
        return result;
    }

    /**
     * 获取所有的产品
     */
    public List<IotProductRes> getProduct(Term term){
        QueryParamEntity query = new QueryParamEntity();
        query.setTerms(Collections.singletonList(term));
        query.setPaging(false);
        String api = "/device/product/_query/no-paging";
        JSONObject res = post(api,JSON.toJSONString(query));
        List<IotProductRes> result = JSON.parseArray(res.getString("result"), IotProductRes.class);
        return result;
    }

    /**
     * 获取所有的产品分类
     */
    public List<IotProductCategoryRes> getProductCategory(Term term){
        QueryParamEntity query = new QueryParamEntity();
        query.setTerms(Collections.singletonList(term));
        query.setPaging(false);
        String api = "/device/category/_query/no-paging";
        JSONObject res = post(api,JSON.toJSONString(query));
        List<IotProductCategoryRes> result = JSON.parseArray(res.getString("result"), IotProductCategoryRes.class);
        return result;
    }

    /**
     * 获取设备表数
     */
    public BigDecimal getMeterNumber(String deviceId, EnergyType[] energyType){
        String property =null;
        if(energyType[0].getValue().equals("water")){
            property="Flow";
        }else if(energyType[0].getValue().equals("electricity")){
            property="positiveActE";
        }else {
            property="Flow";
        }
        //String api = "/device/instance/"+ deviceId +"/property/"+property;
        String api="/device/instance/"+ deviceId +"/property/"+ property +"/_query";
        Map<String, Object> params = new HashMap<>();
        params.put("deviceId",deviceId);
        params.put("property",property);
        params.put("sorts[0].name","createTime");
        params.put("sorts[0].order","desc");
        JSONObject res = get(api, params);
        JSONObject data = JSONObject.parseObject(res.getJSONObject("result").getJSONArray("data").get(0).toString());
        BigDecimal number = data.getBigDecimal("value");
        return number;
    }

    /**
     * 获取设备属性的最新值
     */
    public Object getDevicePropertyCurrentValue(String deviceId,String property){
        String api = "/device/instance/"+ deviceId +"/property/"+ property  +"/_query";
        Map<String, Object> params = new HashMap<>();
        params.put("deviceId",deviceId);
        params.put("property",property);
        JSONObject res = get(api, params);
        if(property.equals("computeStatus") && res.getJSONObject("result").getJSONArray("data").isEmpty() ){
            return "4";
        }
        JSONObject data = JSONObject.parseObject(res.getJSONObject("result").getJSONArray("data").get(0).toString());
        Object auto = null;
        if(Objects.nonNull(data)){
            auto = data.get("value");
        }

        return auto;
    }

    /**
     * 获取设备网关在线状态
     */
    public String getDeviceState(String deviceId){
        String api = "/device/instance/"+ deviceId +"/state";
        Map<String, Object> params = new HashMap<>();
        params.put("deviceId",deviceId);
        JSONObject res = get(api, params);
        String state = res.getJSONObject("result").getString("value");
        String deviceStatus = "0";
        if("online".equals(state)){
            deviceStatus = "1";
        }

        return deviceStatus;
    }

    /**
     * 获取对应表的时间段内的能耗信息
     */
    public String getDevicePropety(GetIotDeviceEnergy getIotDeviceEnergy){
        String api = "/device/instance/"+getIotDeviceEnergy.getDeviceId()+"/property/"+getIotDeviceEnergy.getProperty()+"/_query";
        QueryParamEntity query = new QueryParamEntity();
        query.setPaging(false);
        query.setParallelPager(false);
        query.setOrderBy("timestamp asc");
        if(getIotDeviceEnergy.getStartTime()!=null && getIotDeviceEnergy.getEndTime()!=null){
            Term term = new Term();
            term.setColumn("timestamp");
            Long[] longs={getIotDeviceEnergy.getStartTime(),getIotDeviceEnergy.getEndTime()};
            term.setTermType("btw");
            term.setValue(longs);
            ArrayList<Term> terms = new ArrayList<>();
            terms.add(term);
            query.setTerms(terms);
        }
        JSONObject res = post(api,JSON.toJSONString(query));
        String data = res.getJSONObject("result").getJSONArray("data").toJSONString();
        return data;

    }


    /**
     * 获取电表相关的所有属性
     */
    public String getElectricityData(GetIotDeviceEnergy getIotDeviceEnergy){
        String api = "/device/instance/"+getIotDeviceEnergy.getDeviceId()+"/properties/_query/no-paging";
        QueryParamEntity query = new QueryParamEntity();
        query.setPaging(false);
        query.setParallelPager(false);
        query.setOrderBy("timestamp asc");
        Term term = new Term();
        term.setColumn("timestamp");
        Long[] longs={getIotDeviceEnergy.getStartTime(),getIotDeviceEnergy.getEndTime()};
        term.setTermType("btw");
        term.setValue(longs);
        ArrayList<Term> terms = new ArrayList<>();
        terms.add(term);
        query.setTerms(terms);
        JSONObject res = post(api,JSON.toJSONString(query));
        String data = res.getJSONArray("result").toJSONString();
        return data;

    }

}
