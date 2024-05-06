package com.xhs.config;

import com.xhs.utils.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
	//登录拦截器
	registry.addInterceptor(new LoginInterceptor())
		.excludePathPatterns(	//排除拦截的路径
			"/shop/**",
			"/voucher/**",
			"/shop-type/**",
			"/upload/**",
			"/blog/hot",
			"/user/code",
			"/user/login"
		).order(1);//order表示拦截器的执行顺序，值越小越先执行
    }
}
