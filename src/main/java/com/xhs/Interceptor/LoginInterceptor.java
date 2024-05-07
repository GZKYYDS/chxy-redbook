package com.xhs.Interceptor;


import com.xhs.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {


    //前置拦截器
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
	//1.判断是否需要拦截
	//有用户信息，放行
	//没有用户信息，拦截
	if (UserHolder.getUser() == null) {
	    response.setStatus(401);
	    return false;
	}
	return true;
    }

    //后置拦截器
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
	//移除用户的原因：线程池的线程是复用的，如果不移除，会导致线程之间的用户信息混乱
	UserHolder.removeUser();
    }
}
