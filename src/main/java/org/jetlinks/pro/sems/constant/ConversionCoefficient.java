package org.jetlinks.pro.sems.constant;


import java.math.BigDecimal;

//水电气标准煤、碳排放折算系数
public class ConversionCoefficient {
    //水标准煤系数
    public static final BigDecimal WATER_STANDARD_COAL=BigDecimal.valueOf(0.086);
    //电标准煤系数
    public static final BigDecimal ELECTRICITY_STANDARD_COAL=BigDecimal.valueOf(0.404);
    //天然气标准煤系数
    public static final BigDecimal GAS_STANDARD_COAL=BigDecimal.valueOf(1.33);
    //水碳排放系数
    public static final BigDecimal WATER_DIOXIDE_EMISSION=BigDecimal.valueOf(0.91);
    //电碳排放系数
    public static final BigDecimal ELECTRICITY_DIOXIDE_EMISSION=BigDecimal.valueOf(0.785);
    //天然气碳排放系数
    public static final BigDecimal GAS_DIOXIDE_EMISSION=BigDecimal.valueOf(0.19);
}
