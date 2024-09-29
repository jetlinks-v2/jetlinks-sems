package org.jetlinks.pro.sems.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
public enum EnergyDataBaseNameEnum implements I18nEnumDict<String> {
    water("sems_water_consume"),
    electricity("sems_electricity_consume"),
    gas("sems_gas_consume");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
