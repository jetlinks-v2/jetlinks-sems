package org.jetlinks.project.busi.entity.req;

import lombok.Data;
import org.jetlinks.project.busi.entity.TestConfigDeviceEntity;

import java.util.List;

/**
 * 绑定设备 Req
 * */
@Data
public class TestBindDeviceReq {

    /**
     * 试验ID
     * */
    private String configId;

    /**
     * 设备ID数组
     * */
    private List<TestConfigDeviceEntity> deviceList;
}
