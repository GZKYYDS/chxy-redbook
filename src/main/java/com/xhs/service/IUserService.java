package com.xhs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhs.dto.LoginFormDTO;
import com.xhs.dto.Result;
import com.xhs.entity.User;

import javax.servlet.http.HttpSession;

/**
 * 用户服务接口
 */
public interface IUserService extends IService<User> {

    /**
     * 发送手机验证码
     *
     * @param phone
     * @param session
     * @return
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 登录功能
     *
     * @param loginForm
     * @param session
     * @return
     */
    Result login(LoginFormDTO loginForm, HttpSession session);
}
