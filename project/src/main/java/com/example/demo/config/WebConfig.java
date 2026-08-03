package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 只攔「需要登入」的路徑，其餘（index、stores、login、register、公開 API...）全部放行
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                    "/profile",                     // 會員頁：沒登入不能看
                    "/api/me/**"                   // /api/me 底下全是個人資料，一律要登入
                );
    }
}