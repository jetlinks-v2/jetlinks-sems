package org.jetlinks.pro.sems.factory;

import org.jetlinks.pro.sems.strategy.peak.PeakAnalysisStrategy;

/**
 * @ClassName PeakFactory
 * @Author hky
 * @Time 2023/7/13 23:05
 * @Description 峰值分析工厂
 **/
public interface PeakFactory {

    PeakAnalysisStrategy getPeakStrategy(String flag);
}
