package org.jetlinks.project.busi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
public enum DeviceType implements I18nEnumDict<String> {

    userInformation("用户信息传输"),
    waterLevelMonitoring("水位监测"),
    waterPressureMonitoring("水压监测");

    private final String text;


    @Override
    public String getValue() {
        return name();
    }
}
