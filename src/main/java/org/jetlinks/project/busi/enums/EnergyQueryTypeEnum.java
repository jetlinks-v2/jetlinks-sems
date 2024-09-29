package org.jetlinks.project.busi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@AllArgsConstructor
@Getter
public enum EnergyQueryTypeEnum implements I18nEnumDict<String> {

    area("按区域"),
    test("按实验");
    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
