package org.jetlinks.pro.sems.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
public enum EnergyType implements I18nEnumDict<String> {

    water("水"),
    electricity("电"),
    gas("气");


    private final String text;

    @Override
    public String getValue() {
        return name();
    }

    public static EnergyType of(String name){
        for (EnergyType value :values()){
            if (name.equals(value.name())){
                return value;
            }
        }


        throw new EnumConstantNotPresentException(EnergyType.class,"没有找到:"+name);
    }
}
