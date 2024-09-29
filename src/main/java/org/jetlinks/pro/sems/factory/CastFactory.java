package org.jetlinks.pro.sems.factory;

import org.jetlinks.pro.sems.strategy.cost.CostStrategy;

public interface CastFactory {

    CostStrategy getCostStrategy(String type);
}
