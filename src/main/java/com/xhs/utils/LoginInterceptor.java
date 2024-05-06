package com.xhs.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.xhs.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xhs.constants.RedisConstants.LOGIN_USER_KEY;
import static com.xhs.constants.RedisConstants.LOGIN_USER_TTL;

public class LoginInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
	this.stringRedisTemplate = stringRedisTemplate;
    }

    //前置拦截器
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
	// 1.获取请求头中的token
	String token = request.getHeader("authorization");
	if (StrUtil.isBlank(token)) {
	    return true;
	}
	// 2.基于TOKEN获取redis中的用户
	String key  = LOGIN_USER_KEY + token;
	Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
	// 3.判断用户是否存在
	if (userMap.isEmpty()) {
	    return true;
	}
	// 5.将查询到的hash数据转为UserDTO
	UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
	// 6.存在，保存用户信息到 ThreadLocal
	UserHolder.saveUser(userDTO);
	// 7.刷新token有效期
	stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
	// 8.放行
	return true;
    }

    //后置拦截器
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
	//移除用户的原因：线程池的线程是复用的，如果不移除，会导致线程之间的用户信息混乱
	UserHolder.removeUser();
    }
}
