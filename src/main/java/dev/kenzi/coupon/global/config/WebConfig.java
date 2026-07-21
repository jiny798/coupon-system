package dev.kenzi.coupon.global.config;

import dev.kenzi.coupon.auth.jwt.JwtTokenProvider;
import dev.kenzi.coupon.auth.support.AuthInterceptor;
import dev.kenzi.coupon.auth.support.LoginUserArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final LoginUserArgumentResolver loginUserArgumentResolver;

    public WebConfig(JwtTokenProvider jwtTokenProvider) {
        this.authInterceptor = new AuthInterceptor(jwtTokenProvider);
        this.loginUserArgumentResolver = new LoginUserArgumentResolver();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/users/signup", "/api/auth/login");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }
}
