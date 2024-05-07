package com.xhs.config;

import com.xhs.Interceptor.LoginInterceptor;
import com.xhs.Interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
	//登录拦截器
	registry.addInterceptor(new LoginInterceptor())
		.excludePathPatterns(        //排除拦截的路径
			"/shop/**",
			"/voucher/**",
			"/shop-type/**",
			"/upload/**",
			"/blog/hot",
			"/user/code",
			"/user/login"
		).order(1);//order表示拦截器的执行顺序，值越小越先执行
	//刷新token拦截器
	registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate));
    }
}
