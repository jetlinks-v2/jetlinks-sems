package org.jetlinks.pro.sems.service;

import lombok.AllArgsConstructor;
import org.jetlinks.pro.sems.config.EnergyFormulaConfig;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.8
 */
@Service
@AllArgsConstructor
public class CalculationService {

    private final EnergyFormulaConfig energyFormulaConfig;



    /**获得碳排放公式
     *<pre>{@code
     *
     *}</pre>
     * @param
     * @return
     * @see
     */
    public  Expression getCarboneMissionFormula(Integer type){
        String formula;
        //根据不同的类型选择公式
        if(type == 1) {
            formula = energyFormulaConfig.getCarboneMission().getWater();
        }else if(type == 2){
            formula = energyFormulaConfig.getCarboneMission().getElectricity();
        }else {
            formula = energyFormulaConfig.getCarboneMission().getGas();
        }
        ExpressionParser parser = new SpelExpressionParser();
        return parser.parseExpression(formula);
    }


    /**获得标准煤公式
     *<pre>{@code
     *
     *}</pre>
     * @param
     * @return
     * @see
     */
    public  Expression getStandardCoalFormula(Integer type){
        String formula;
        //根据不同的类型选择公式
        if(type == 1) {
            formula = energyFormulaConfig.getStandardCoal().getWater();
        }else if(type == 2){
            formula = energyFormulaConfig.getStandardCoal().getElectricity();
        }else {
            formula = energyFormulaConfig.getStandardCoal().getGas();
        }
        ExpressionParser parser = new SpelExpressionParser();
        return parser.parseExpression(formula);
    }


    /**计算
     *<pre>{@code
     *
     *}</pre>
     * @param
     * @return
     * @see
     */
    public BigDecimal Calculate(BigDecimal x,Expression expression) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("x", x);
        BigDecimal result = expression.getValue(context, BigDecimal.class);
        return result;
    }
}
