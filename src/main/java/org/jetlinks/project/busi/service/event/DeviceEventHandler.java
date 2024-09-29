package org.jetlinks.project.busi.service.event;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.*;
import org.jetlinks.project.busi.entity.DeviceInfoEntity;
import org.jetlinks.project.busi.iot.IotService;
import org.jetlinks.project.busi.service.DeviceService;
import org.jetlinks.project.busi.service.IotAccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeviceEventHandler implements CommandLineRunner {

    private final DeviceService service;

    private final IotService iotService;

    private final IotAccessService iotAccessService;

    private final ReactiveRedisTemplate<Object,Object> redis;

    @Value("${systemIp.main}")
    private String ip;

    private MqttClient mqttClient;


    @EventListener
    public void handleUpdateEvent(EntityBeforeModifyEvent<DeviceInfoEntity> event){

        event.async(this.sendUpdateNotify(event.getAfter()));

    }

    public Mono<Void> sendUpdateNotify(List<DeviceInfoEntity> deviceInfoList){

        return Flux.fromIterable(deviceInfoList)
                   .flatMap(device -> service
                       .findById(device.getId())
                       .flatMap(deviceInfoEntity -> {
                           if(!device.getSubscribeStatus().equals(deviceInfoEntity.getSubscribeStatus())) {
                               subscribe(mqttClient);
                           }
                           return Mono.empty();
                       }))
                   .then();
    }

    public void run(String... args) throws Exception {
        MqttClientOptions options = new MqttClientOptions();
//        String clientId = iotService.createToken();
        options.setClientId("clientId");
        options.setCleanSession(true);
        //设置超时时间，单位为秒
        options.setConnectTimeout(1000);
        //设置心跳时间 单位为秒，表示服务器每隔1.5*20秒的时间向客户端发送心跳判断客户端是否在线
        options.setKeepAliveInterval(2000);
        options.setMaxMessageSize(100000);
        mqttClient = MqttClient.create(Vertx.vertx(), options);
//        connectConfig(mqttClient);
    }

    private void subscribe(MqttClient client) {
        Map<String, Integer> topics = new HashMap<>();
        topics.put("/device/+/+/message/property/report",2);
        topics.put("/device/+/+/online",2);
        topics.put("/device/+/+/offline",2);
        client.subscribe(topics, e -> {
            if (e.succeeded()) {
                log.info("===>subscribe success: {}", e.result());
            } else {
                log.error("===>subscribe fail: ", e.cause());
            }
        }).publishHandler(message ->{
            JSONObject device = JSONObject.parseObject(String.valueOf(message.payload()));
            String messageType = device.getString("messageType");
            if(messageType.equals("ONLINE") || messageType.equals("OFFLINE")){
                iotAccessService.getDeviceState(String.valueOf(message.payload())).subscribe();
            }
            if(messageType.equals("REPORT_PROPERTY")){
                iotAccessService.getDeviceDataReport(String.valueOf(message.payload())).subscribe(); }
        }).closeHandler(closeStatus ->{
            log.info("client已断开连接");
//            connectConfig(client);

        }).exceptionHandler(exceptionMessage->{
            log.info("exception报错:{}",exceptionMessage.getMessage());
        });
    }

    private MqttClient connectConfig(MqttClient client){
        client.connect(11883,ip,e->{
            if(e.succeeded()){
                log.info("MQTT连接成功");
                subscribe(client);
            }
            if(e.failed()){
                log.info("MQTT连接失败，尝试重连");
//                connectConfig(client);
            }
        });
        return  client;
    }

    //删除事件(更新redis)
    @EventListener
    public void handleDeleteEvent(EntityDeletedEvent<DeviceInfoEntity> event){

        event.async(this.sendDeleteNotify(event.getEntity()));

    }

    public  Flux<Object> sendDeleteNotify(List<DeviceInfoEntity> deviceInfoList){
        return Flux.fromIterable(deviceInfoList)
            .flatMap(device->{
                return redis.delete("device:"+device.getDeviceId());
            });
    }

}
