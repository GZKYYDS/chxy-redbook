package com.xhs.utils;


import com.xhs.dto.UserDTO;
import com.xhs.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {
    //前置拦截器
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
	//1.获取session中的用户信息
	Object user = request.getSession().getAttribute("user");
	//2.判断用户是否存在
	if (user == null) {
	    //3.不存在 拦截 返回401
	    response.setStatus(401);
	    return false;
	}
	//4.存在 放行 保存用户信息到threadLocal
	UserHolder.saveUser((UserDTO) user);
	return true;//放行
    }

    //后置拦截器
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
	//移除用户的原因：线程池的线程是复用的，如果不移除，会导致线程之间的用户信息混乱
	UserHolder.removeUser();
    }
}
