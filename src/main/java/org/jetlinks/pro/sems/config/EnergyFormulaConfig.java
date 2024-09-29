package org.jetlinks.pro.sems.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**碳排放  标准煤 计算公式配置
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.8
 */
@Component
@ConfigurationProperties(prefix = "formula")
public class EnergyFormulaConfig {
    private Formula carboneMission;
    private Formula standardCoal;

    public Formula getCarboneMission() {
        return carboneMission;
    }

    public void setCarboneMission(Formula carboneMission) {
        this.carboneMission = carboneMission;
    }

    public Formula getStandardCoal() {
        return standardCoal;
    }

    public void setStandardCoal(Formula standardCoal) {
        this.standardCoal = standardCoal;
    }

    public static class Formula {
        private String water;
        private String electricity;
        private String gas;

        public String getWater() {
            return water;
        }

        public void setWater(String water) {
            this.water = water;
        }

        public String getElectricity() {
            return electricity;
        }

        public void setElectricity(String electricity) {
            this.electricity = electricity;
        }

        public String getGas() {
            return gas;
        }

        public void setGas(String gas) {
            this.gas = gas;
        }
    }
}
