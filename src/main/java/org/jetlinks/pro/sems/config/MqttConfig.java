//package org.jetlinks.project.busi.config;
//
//import com.alibaba.fastjson.JSONObject;
//import io.vertx.core.Vertx;
//import io.vertx.mqtt.MqttClient;
//import io.vertx.mqtt.MqttClientOptions;
//import lombok.extern.slf4j.Slf4j;
//import org.jetlinks.project.busi.iot.IotService;
//import org.jetlinks.project.busi.service.IotAccessService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Slf4j
//public class MqttConfig {
//
//    @Autowired
//    private IotService iotService;
//    @Autowired
//    private IotAccessService iotAccessService;
//    @Value("${systemIp.main}")
//    private String ip;
//
//
////    public void run(String... args) throws Exception {
////        MqttClientOptions options = new MqttClientOptions();
////        String clientId = iotService.createToken();
////        options.setClientId(clientId);
////        options.setCleanSession(true);
////        //设置超时时间，单位为秒
////        options.setConnectTimeout(1000);
////        //设置心跳时间 单位为秒，表示服务器每隔1.5*20秒的时间向客户端发送心跳判断客户端是否在线
////        options.setKeepAliveInterval(2000);
////        options.setMaxMessageSize(100000);
////        MqttClient mqttClient = MqttClient.create(Vertx.vertx(), options);
////        connectConfig(mqttClient);
////     }
//
//    private void subscribe(MqttClient client) {
//        Map<String, Integer> topics = new HashMap<>();
//        topics.put("/device/+/+/message/property/report",2);
//        topics.put("/device/+/+/online",2);
//        topics.put("/device/+/+/offline",2);
//        client.subscribe(topics, e -> {
//            if (e.succeeded()) {
//                log.info("===>subscribe success: {}", e.result());
//            } else {
//                log.error("===>subscribe fail: ", e.cause());
//            }
//        }).publishHandler(message ->{
//            JSONObject device = JSONObject.parseObject(String.valueOf(message.payload()));
//            String messageType = device.getString("messageType");
//            if(messageType.equals("ONLINE") || messageType.equals("OFFLINE")){
//                iotAccessService.getDeviceState(String.valueOf(message.payload())).subscribe();
//            } else {
//                iotAccessService.getDeviceDataReport(String.valueOf(message.payload())).subscribe();
//            }
//        }).closeHandler(closeStatus ->{
//            log.info("client已断开连接");
////            connectConfig(client);
//
//        }).exceptionHandler(exceptionMessage->{
//            log.info("exception报错:{}",exceptionMessage.getMessage());
//        });
//    }
//
//    private MqttClient connectConfig(MqttClient client){
//        client.connect(11883,ip,e->{
//            if(e.succeeded()){
//                log.info("MQTT连接成功");
//                subscribe(client);
//            }
//            if(e.failed()){
//                log.info("MQTT连接失败，尝试重连");
////                connectConfig(client);
//            }
//        });
//        return  client;
//    }
//}
