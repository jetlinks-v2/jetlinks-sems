package org.jetlinks.project.busi.micro;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.hswebframework.web.crud.web.ResponseMessage;
import org.jetlinks.pro.microservice.ServicesRequesterManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

@Component
@AllArgsConstructor
public class RequestMicroHandle {

    private final ServicesRequesterManager requesterManager;


    /**
     * 请求其他服务
     * @param serviceName 服务名称
     * @param url 路径
     * @param method 方法类型
     * @param param 参数
     * @return
     */
    public Mono<ResponseMessage<Map<String, Object>>> sendService(String serviceName,String url,String method,JSONObject param){
        return requesterManager
            .getWebClient(serviceName)
            .flatMap(client ->{
                switch (method.toUpperCase()) {
                    case "GET":
                        return this.sendMicroGet(client, url, param);
                    case "POST":
                        return this.sendMicroPost(client, url, param);
                    case "PATCH":
                        return this.sendMicroPatch(client, url, param);
                    case "DELETE":
                        return this.sendMicroDelete(client, url, param);
                    case "PUT":
                        return this.sendMicroPut(client, url, param);
                    default:
                        return Mono.error(new IllegalArgumentException("Unsupported HTTP method: " + method));
                }
            });
    }

    /**
     * put请求
     * @param client webclient 服务
     * @param url 路径
     * @param param 参数
     * @return
     */
    public Mono<ResponseMessage<Map<String, Object>>> sendMicroPut(WebClient client,String url,JSONObject param){
        return client
            .put()
            .uri(url)
            .bodyValue(param)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ResponseMessage<Map<String, Object>>>() {
            });
    }

    /**
     * put请求
     * @param client webclient 服务
     * @param url 路径
     * @param param 参数
     * @return
     */
    public Mono<ResponseMessage<Map<String, Object>>> sendMicroPatch(WebClient client,String url,JSONObject param){
        return client
            .patch()
            .uri(url)
            .bodyValue(param)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ResponseMessage<Map<String, Object>>>() {
            });
    }


    /**
     * post请求
     * @param client webclient 服务
     * @param url 路径
     * @param param 参数
     * @return
     */
    public Mono<ResponseMessage<Map<String, Object>>> sendMicroPost(WebClient client,String url,JSONObject param){
        return client
            .post()
            .uri(url)
            .bodyValue(param)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ResponseMessage<Map<String, Object>>>() {
            });
    }

    /**
     * get请求
     * @param client webclient 服务
     * @param url 路径
     * @param param 参数
     * @return
     */
    public Mono<ResponseMessage<Map<String, Object>>> sendMicroGet(WebClient client,String url,JSONObject param){

        if (ObjectUtils.isNotEmpty(param)){
            String json = JSONObject.toJSONString(param);
            TreeMap<String,Object> map = JSONObject.parseObject(json, TreeMap.class);

            StringJoiner sb = new StringJoiner("&");
            for (Map.Entry<String, Object> entry : map.entrySet()) {

                sb.add(entry.getKey() + "=" + entry.getValue());

            }

            url = url + "?" + sb;
        }

        return client
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ResponseMessage<Map<String, Object>>>() {
            });
    }

    /**
     * delete请求
     * @param client webclient 服务
     * @param url 路径
     * @param param 参数
     * @return
     */
    public Mono<ResponseMessage<Map<String, Object>>> sendMicroDelete(WebClient client,String url,JSONObject param){

        if (ObjectUtils.isNotEmpty(param)){
            String json = JSONObject.toJSONString(param);
            TreeMap<String,Object> map = JSONObject.parseObject(json, TreeMap.class);

            StringJoiner sb = new StringJoiner("&");
            for (Map.Entry<String, Object> entry : map.entrySet()) {

                sb.add(entry.getKey() + "=" + entry.getValue());

            }

            url = url + "?" + sb;
        }

        return client
            .delete()
            .uri(url)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ResponseMessage<Map<String, Object>>>() {
            });
    }
}
