package com.xhs.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xhs.constants.RedisConstants.*;
import static com.xhs.constants.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
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
	stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
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
	// 1.校验手机号
	String phone = loginForm.getPhone();
	if (RegexUtils.isPhoneInvalid(phone)) {
	    // 2.如果不符合，返回错误信息
	    return Result.fail("手机号格式错误！");
	}
	// 3.从redis获取验证码并校验
	String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
	String code = loginForm.getCode();
	if (cacheCode == null || !cacheCode.equals(code)) {
	    // 不一致，报错
	    return Result.fail("验证码错误");
	}

	// 4.一致，根据手机号查询用户 select * from tb_user where phone = ?
	User user = query().eq("phone", phone).one();

	// 5.判断用户是否存在
	if (user == null) {
	    // 6.不存在，创建新用户并保存
	    user = createUserWithPhone(phone);
	}

	// 7.保存用户信息到 redis中
	// 7.1.随机生成token，作为登录令牌
	String token = UUID.randomUUID().toString(true);
	// 7.2.将User对象转为HashMap存储
	UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
	Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
		CopyOptions.create()
			.setIgnoreNullValue(true)
			.setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
	// 7.3.存储
	String tokenKey = LOGIN_USER_KEY + token;
	stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
	// 7.4.设置token有效期
	stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

	// 8.返回token
	return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
	User user = new User();
	user.setPhone(phone);
	user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(6));
	save(user);//保存到数据库
	return user;
    }
}
