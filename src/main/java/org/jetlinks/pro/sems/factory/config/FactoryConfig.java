package org.jetlinks.pro.sems.factory.config;

import org.jetlinks.pro.sems.factory.CastFactory;
import org.jetlinks.pro.sems.factory.PeakFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName PeakAnalysisFactory
 * @Author hky
 * @Time 2023/7/12 17:44
 * @Description
 **/

@Configuration
@SuppressWarnings("all")
public class FactoryConfig {

    @Bean
    public FactoryBean<Object> IOTFactoryRegister(){
        ServiceLocatorFactoryBean factoryBean = new ServiceLocatorFactoryBean();
        factoryBean.setServiceLocatorInterface(PeakFactory.class);
        return factoryBean;
    }

    @Bean
    public FactoryBean<Object> IOTFactoryRegisterByCast(){
        ServiceLocatorFactoryBean factoryBean = new ServiceLocatorFactoryBean();
        factoryBean.setServiceLocatorInterface(CastFactory.class);
        return factoryBean;
    }

}
