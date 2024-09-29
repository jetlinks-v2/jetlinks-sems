package org.jetlinks.project.busi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

/**
 * @ClassName PeriodsEnum
 * @Author hky
 * @Time 2023/7/18 18:31
 * @Description 用电时尖峰平谷标识对应
 **/
@Getter
@AllArgsConstructor
public enum PeriodsEnum implements I18nEnumDict<Integer> {
    CUSP(1,"尖标识"),
    PEAK(2,"峰标识"),
    FLAT(3,"平标识"),
    VALLEY(4,"谷标识"),

;
    private final  Integer value;

    private final  String description;

    @Override
    public String getText() {
        return name();
    }

    public static PeriodsEnum of(Integer type){
        for (PeriodsEnum periodsEnum :values()){
            if (periodsEnum.getValue().equals(type)){
                return periodsEnum;
            }
        }

        throw new EnumConstantNotPresentException(PeriodsEnum.class,"枚举类型错误:"+type.toString());
    }
}
