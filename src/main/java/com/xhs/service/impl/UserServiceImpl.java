package com.xhs.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhs.dto.LoginFormDTO;
import com.xhs.dto.Result;
import com.xhs.dto.UserDTO;
import com.xhs.entity.User;
import com.xhs.mapper.UserMapper;
import com.xhs.service.IUserService;
import com.xhs.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.xhs.constants.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 发送手机验证码
     *
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
	// 发送短信验证码并保存验证码
	//1.校验手机号
	if (RegexUtils.isPhoneInvalid(phone)) {
	    //2.如果不符合要求，返回错误信息
	    return Result.fail("手机号格式不正确");
	}
	//3.如果符合生成验证码
	String code = RandomUtil.randomNumbers(6);
	//保存验证码
	session.setAttribute("code", code);
	//4.发送验证码
	log.debug("发送短信验证码成功，验证码：{}", code);        //模拟
	//返回
	return Result.ok();
    }

    /**
     * 登录功能
     *
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
	//1.校验手机号
	String phone = loginForm.getPhone();
	if (RegexUtils.isPhoneInvalid(phone)) {
	    //2.如果不符合要求，返回错误信息
	    return Result.fail("手机号格式不正确");
	}
	//2.校验验证码
	//从session中获取验证码
	Object cacheCode = session.getAttribute("code");
	String code = loginForm.getCode();
	//3.不一致返回错误信息
	if (cacheCode == null || !cacheCode.toString().equals(code)) {
	    return Result.fail("验证码错误");
	}
	//4.如果一致， 根据手机号查询用户信息
	User user = query().eq("phone", phone).one();

	//判断用户是否存在
	//5.如果不存在，创建新用户
	if (user == null) {
	    user = createUserWithPhone(phone);
	}
	//6.如果存在保存用户到session
	session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

	return Result.ok();
	//不需要返回凭证，因为后续的请求都会带上session  session原理是cookie
	//每一个session都有一个唯一的sessionid，这个sessionid会在响应头中返回给浏览器，浏览器会保存在cookie中
    }

    private User createUserWithPhone(String phone) {
	User user = new User();
	user.setPhone(phone);
	user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(6));
	save(user);//保存到数据库
	return user;
    }
}
