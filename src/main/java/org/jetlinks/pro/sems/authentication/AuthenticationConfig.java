//package org.jetlinks.project.busi.authentication;
//
//import com.github.benmanes.caffeine.cache.Cache;
//import com.github.benmanes.caffeine.cache.Caffeine;
//import com.google.common.net.HttpHeaders;
//import lombok.Setter;
//import org.hswebframework.web.authorization.Authentication;
//import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
//import org.hswebframework.web.authorization.ReactiveAuthenticationManager;
//import org.hswebframework.web.authorization.ReactiveAuthenticationSupplier;
//import org.hswebframework.web.authorization.token.*;
//import org.jetlinks.pro.openapi.OpenApiClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.util.StringUtils;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilterChain;
//import reactor.core.publisher.Mono;
//import reactor.util.context.Context;
//
//import javax.annotation.Nonnull;
//import java.time.Duration;
//import java.util.*;
//
//public class AuthenticationConfig implements WebFilter, ReactiveAuthenticationSupplier {
//
//    private final WebClient client;
//
//    private final ReactiveAuthenticationManager defaultAuthenticationManager;
//
//    private final UserTokenManager userTokenManager;
//
//    private final Map<String, ThirdPartReactiveAuthenticationManager> thirdPartAuthenticationManager = new HashMap();
//
//    //token有效期
//    @Setter
//    private Duration expires = Duration.ofMinutes(30);
//
//    //系统直接访问IAM接口的认证key,不同IMA系统可能认证方式不同
//    @Setter
//    private String accessKey = "";
//
//    private final Cache<String, Mono<Authentication>> tokenCache =
//        Caffeine.newBuilder()
//                .softValues()
//                .build();
//
//    public AuthenticationConfig(WebClient client,UserTokenManager userTokenManager, ReactiveAuthenticationManager defaultAuthenticationManager) {
//        this.client = client;
//        this.defaultAuthenticationManager = defaultAuthenticationManager;
//        this.userTokenManager = userTokenManager;
//        //注册到Holder
//        ReactiveAuthenticationHolder.addSupplier(this);
//    }
//
//    @Override
//    @Nonnull
//    public Mono<Void> filter(@Nonnull ServerWebExchange exchange, @Nonnull WebFilterChain chain) {
//        String token = exchange
//            .getRequest()
//            .getHeaders()
//            .getFirst(HttpHeaders.AUTHORIZATION);
//
//        //从header中获取token,格式: Authorization: Bearer {token}
//        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
//            return chain.filter(exchange);
//        }
//
//        token = token.substring(7);
//
////        return this
////            //根据token获取用户信息
////            .requestUserInfoByToken(token)
////            .flatMap(auth -> chain
////                .filter(exchange)
////                //传入上下文到上游
////                .contextWrite(ctx -> ctx.put(AuthenticationConfig.class, auth)));
//        return chain
//            .filter(exchange)
//            //传入上下文到上游
//            .contextWrite(Context.of(ParsedToken.class, token));
//    }
//
//    private Mono<Authentication> requestUserInfoByToken(String token) {
//
//        //缓存token的权限信息,避免每次请求都去IAM获取
//        //缓存时间根据实际需求调整，缓存时间过长可能导致用户权限变更后无法及时生效a
//        return tokenCache.get(token, _token -> this
//            .requestUserInfoByToken0(_token)
//            .cache(v -> expires,
//                   err -> Duration.ofMinutes(10),
//                   () -> Duration.ofMinutes(10)));
//    }
//
//    private Mono<Authentication> requestUserInfoByToken0(String token) {
//        return client
//            .get()
//            .uri("/api/system/user/getInfo")
//            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
//            .retrieve()
//            .bodyToMono(ThirdPartyUserInfo.class)
//            .map(ThirdPartyUserInfo::toAuthentication);
//    }
//
////    @Override
////    public Mono<Authentication> get(String userId) {
////        return Mono.empty();
////    }
////
////    @Override
////    public Mono<Authentication> get() {
////        return Mono.deferContextual(ctx -> Mono.justOrEmpty(ctx.getOrEmpty(AuthenticationConfig.class)));
////    }
//
//    @Override
//    public Mono<Authentication> get(String userId) {
//        if (userId == null) {
//            return null;
//        }
//        return this.defaultAuthenticationManager.getByUserId(userId);
//    }
//
//    @Override
//    public Mono<Authentication> get() {
//        return Mono
//            .deferContextual(context -> context
//                .<ParsedToken>getOrEmpty(ParsedToken.class)
//                .map(t -> this.requestUserInfoByToken0(t.getToken())
//                              .flatMap(e ->{
//                                 if(e.getUser().getUsername().equals("未登录")){
//                                     return userTokenManager.signOutByToken(t.getToken()).flatMap(a->Mono.empty());
//                                 }
//                                  return userTokenManager
//                                      .getByToken(t.getToken())
//                                      .switchIfEmpty(userTokenManager.signIn(t.getToken(), "Bearer", e.getUser().getId(), 600000))
//                                      .flatMap(token -> {
//                                          //已过期则返回空
//                                          if (token.isExpired()) {
//                                              return Mono.empty();
//                                          }
//                                          if(!token.validate()){
//                                              return Mono.empty();
//                                          }
//                                          Mono<Void> before = userTokenManager.touch(token.getToken());
//                                          if (token instanceof AuthenticationUserToken) {
//                                              return before.thenReturn(((AuthenticationUserToken) token).getAuthentication());
//                                          }
//
//                                          return this.defaultAuthenticationManager.getByUserId(token.getUserId());
//                                      });
//                              })
//    )
//                .orElse(Mono.empty()));
//
//    }
//
//}
