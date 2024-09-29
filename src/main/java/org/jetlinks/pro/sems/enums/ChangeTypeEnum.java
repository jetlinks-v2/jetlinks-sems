package org.jetlinks.pro.sems.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
public enum ChangeTypeEnum implements I18nEnumDict<String> {

    REPLACEMENT("坏表更换"),
    WRONGBINDING("错误绑定");
    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
