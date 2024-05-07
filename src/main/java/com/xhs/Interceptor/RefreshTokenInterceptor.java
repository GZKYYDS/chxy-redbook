package com.xhs.Interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.xhs.dto.UserDTO;
import com.xhs.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xhs.constants.RedisConstants.LOGIN_USER_KEY;
import static com.xhs.constants.RedisConstants.LOGIN_USER_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
	this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 前置拦截器
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
	//1.获取请求头中的token
	String token = request.getHeader("authorization");
	//2.判断token是否为空
	if (StrUtil.isBlank(token)) {
	    return true;
	}
	String key = LOGIN_USER_KEY + token;
	//3.基于token从redis中获取用户
	Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
	//4.判断用户是否存在
	if (userMap.isEmpty()) {
	    return true;        //用户不存在，放行
	}
	//5.将查询到的hash数据转为UserDTO
	UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
	//6.存在，保存用户信息到ThreadLocal
	UserHolder.saveUser(userDTO);
	//7.刷新token有效期
	stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.SECONDS);
	//8.放行
	return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
	HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
