package org.jetlinks.project.busi.config;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class getFileUrl {

    @Value("${server.port}")
    private String port;


    @GetMapping("/get/file/url")
    public String url(){
        return "http://localhost:"+port;
    }
}
