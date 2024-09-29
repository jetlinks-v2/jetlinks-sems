//package org.jetlinks.project.busi.authentication;
//
//import org.hswebframework.web.authorization.ReactiveAuthenticationManager;
//import org.hswebframework.web.authorization.token.UserTokenManager;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.reactive.function.client.WebClient;
//
//@Configuration
//public class AuthenticationConfiguration {
//    @Value("${systemIp.main}")
//    private String ip;
//
//    @Bean
//    public AuthenticationConfig authenticationConfig(WebClient.Builder builder, UserTokenManager userTokenManager, ReactiveAuthenticationManager defaultAuthenticationManager){
//        return new AuthenticationConfig(builder.clone().baseUrl("http://"+ip).build(),userTokenManager,defaultAuthenticationManager);
//    }
//}
