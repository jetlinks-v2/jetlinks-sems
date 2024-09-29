package org.jetlinks.project.busi.factory;

import org.jetlinks.project.busi.strategy.cost.CostStrategy;

public interface CastFactory {

    CostStrategy  getCostStrategy(String type);
}
