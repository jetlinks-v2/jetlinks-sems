package org.jetlinks.pro.sems.abutment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.pro.sems.abutment.res.*;
import org.jetlinks.pro.sems.iot.*;
import org.jetlinks.pro.sems.entity.TestAreaEntity;
import org.jetlinks.pro.sems.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Service
public class AbutmentCustService {

    private static HttpClient httpClient = HttpClientBuilder.create().build();
    private static final Logger log = LoggerFactory.getLogger(IotService.class);

    @Value("${systemIp.custom:10.130.179.23}")
    private  String ip;



    public JSONObject post(String api, String body) {

        String url = "http://"+ip+":8082" + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);
        request.header("khtyAuth", "iosaiosaioasasa11221s@5845%");
        request.requestBody(body);
        try {
            Response response = request.post();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));

            if (res.getInteger("code") == 500) {
                throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }
    public JSONObject get(String api, Map params) {
        String url = "http://"+ip+":8082" + api;
        HttpRequest request = new SimpleHttpRequest(url, httpClient);

        request.header("khtyAuth","iosaiosaioasasa11221s@5845%");
        request.params(params);
        try {
            Response response = request.get();
            JSONObject res = JSON.parseObject(String.valueOf(JSON.parse(response.asBytes())));
            if (res.getInteger("code") == 500) {
                throw new BusinessException(res.getString("message"));
            }
            return res;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }


    /**
     * 获取项目条目列表
     * @param
     * @return
     */
    public Flux<ExperimentitemItemRes> getItemList(){
        String api="/prod-api/remote/experimentitem/page";
        HashMap<String, Object> map = new HashMap<>();
        map.put("pageNum","1");
        map.put("pageSize","1000");
        JSONObject post = post(api, JSONObject.toJSONString(map));
        List<ExperimentitemItemRes> experimentitemItemResList = JSON.parseArray(post.get("rows").toString(), ExperimentitemItemRes.class);
        return Flux.fromIterable(experimentitemItemResList);
    }

    /**
     * 通过id集获取设备
     */
    public Flux<ExperimentDeviceRes> getDeviceByItemId(List<Integer> ids){
        String api="/prod-api/remote/detailDevice/ids";
        HashMap<String, Object> map = new HashMap<>();
        map.put("ids",ids);
        JSONObject post = post(api, JSONObject.toJSONString(map));
        List<ExperimentDeviceRes> experimentDeviceResList = JSON.parseArray(post.get("data").toString(), ExperimentDeviceRes.class);
        //如果ids接口没有返场地，就用下面的方法获取场地
//        if(!experimentDeviceResList.isEmpty()){
//            for (ExperimentDeviceRes experimentDeviceRes : experimentDeviceResList) {
//                return this.getDeviceByDeviceNo(experimentDeviceRes.getDeviceNo())
//                    .doOnNext(value->experimentDeviceRes.setExperimentAreaId(value.getExperimentAreaId()));
//            }
//        }

        return Flux.fromIterable(experimentDeviceResList);
    }

    public Flux<String> getReservationTestId(){
        String api="/prod-api/remote/experimentreservation/page";
        HashMap<String, Object> map = new HashMap<>();
        map.put("pageNum","1");
        map.put("pageSize","10000");
        //获取最近一个月的试验
        Date currentDateTime = DateUtil.getCurrentDateTime();
        String date = DateUtil.dateToString(currentDateTime, DateUtil.DATE_SHORT_FORMAT);
        //一个月后的日期
        String lastMonthDate = DateUtil.addMonth(currentDateTime, 1);
        map.put("experimentStartTime",date);
        map.put("experimentEndTime",lastMonthDate);
        JSONObject post = post(api, JSONObject.toJSONString(map));
        List<ProjIdRes> list = JSON.parseArray(post.get("rows").toString(), ProjIdRes.class);
        List<String> result = list.stream().map(ProjIdRes::getId).collect(Collectors.toList());
        return Flux.fromIterable(result);

    }


    /**
     * 根据设备id分页获取该设备的信息
     */
    public Flux<ExperimentDeviceRes> getDeviceByDeviceNo(String deviceNo){
        String api="/prod-api/remote/experimentdevice/page";
        HashMap<String, Object> map = new HashMap<>();
        map.put("deviceNo",deviceNo);
        map.put("pageNum","1");
        map.put("pageSize","100");
        JSONObject post = post(api, JSONObject.toJSONString(map));
        List<ExperimentDeviceRes> experimentDeviceResList = JSON.parseArray(post.get("data").toString(), ExperimentDeviceRes.class);
        return Flux.fromIterable(experimentDeviceResList);
    }


    /**
     * 获取试验场地
     * @return
     */
    public Flux<TestAreaEntity> getTestSite(){
        String api="/prod-api/remote/experimentarea/page";
        HashMap<String, Object> map = new HashMap<>();
        map.put("pageNo","1");
        map.put("pageSize","100");
        JSONObject post = post(api, JSONObject.toJSONString(map));
        List<TestAreaEntity> tesrSiteRes = JSON.parseArray(post.get("rows").toString(), TestAreaEntity.class);
        //查询场所类型(还没有接口)
//        for (TestAreaEntity tesrSiteRe : tesrSiteRes) {
//
//        }
        return Flux.fromIterable(tesrSiteRes) ;
    }





    /**
     * 查询试验项目详情
     */
    public Mono<TestProjRes> getTestProj(String id){
        String api="/prod-api/remote/experimentProject/getInfo/"+id;
        JSONObject jsonObject = get(api, new HashMap());
        TestProjRes testProjRes = JSON.parseObject(jsonObject.get("data").toString(), TestProjRes.class);
            return Mono.just(testProjRes)
                .flatMap(value->{
                    List<ItemDateRes> items = value.getItemList();
                    if(!items.isEmpty()){
                        return Flux.fromIterable(items)
                            .flatMap(va->{
                                //填充设备
                                if(!va.getDeviceIdList().isEmpty()){
                                    return this.getDeviceByItemId(va.getDeviceIdList())
                                        .collectList()
                                        .switchIfEmpty(Mono.just(new ArrayList<>()))
                                        .doOnNext(va::setDeviceResList)
                                        .then(
                                            this.getItemNameById(va.getExperimentItemId())
                                                .doOnNext(li->va.setItemName(li.getItemName()))
                                                .then()
                                        )
                                        .thenReturn(value);
                                }
                                return Mono.just(value);
                            }).then(Mono.just(value));
                    }
                    return Mono.just(value);
                });

    }

    /**
     * 查询预约试验项目详情
     */
    public Mono<ReservationTestDetailRes> getReservationTestProj(String id){
        String api="/prod-api/remote/getReservationInfo/"+id;
        JSONObject jsonObject = get(api, new HashMap());
        ReservationTestDetailRes reservationTestDetailRes= JSON.parseObject(jsonObject.get("data").toString(), ReservationTestDetailRes.class);
        return Mono.just(reservationTestDetailRes)
            .flatMap(value->{
                List<ItemDateRes> items = value.getItemList();
                if(!items.isEmpty()){
                    return Flux.fromIterable(items)
                        .flatMap(va->{
                            //填充设备
                            if(!va.getDeviceIdList().isEmpty()){
                                return this.getDeviceByItemId(va.getDeviceIdList())
                                    .collectList()
                                    .switchIfEmpty(Mono.just(new ArrayList<>()))
                                    .doOnNext(va::setDeviceResList)
                                    .then(
                                        this.getItemNameById(va.getExperimentItemId())
                                            .doOnNext(li->va.setItemName(li.getItemName()))
                                            .then()
                                    )
                                    .thenReturn(value);
                            }
                            return Mono.just(value);
                        }).then(Mono.just(value));
                }
                return Mono.just(value);
            });

    }



    /**
     * 根据时间筛选试验项目
     */
    public Flux<String> getTestProjId(){
        String api="/prod-api/remote/experimentProject/page";
        HashMap<String, Object> map = new HashMap<>();
        map.put("projectStatus","1");
        map.put("pageNum","1");
        map.put("pageSize","1000");
        JSONObject post = post(api, JSONObject.toJSONString(map));
        List<ProjIdRes> list = JSON.parseArray(post.get("rows").toString(), ProjIdRes.class);
        List<String> result = list.stream().map(ProjIdRes::getId).collect(Collectors.toList());
        return Flux.fromIterable(result);
    }

    /**
     * 根据条目id获取条目信息
     */
    public Mono<ItemListRes> getItemNameById(String id){
        String api="/prod-api/remote/detailItem/"+id;
        JSONObject jsonObject = get(api, new HashMap());
        ItemListRes itemListRes = JSON.parseObject(jsonObject.get("data").toString(), ItemListRes.class);
        return Mono.just(itemListRes);
    }



}
