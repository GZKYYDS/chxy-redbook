package com.xhs.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 全局唯一ID生成器
 */
@Component
public class RedisIdWorker {

    // 开始时间截 (2024-05-16)
    private final long BEGIN_TIMESTAMP = 1715862606L;
    // 序列号占用的位数
    private final int COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public Long nextId(String keyPrefix) { // keyPrefix: 业务前缀
	//1.获取时间戳
	LocalDateTime now = LocalDateTime.now();
	long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
	long timestamp = nowSecond - BEGIN_TIMESTAMP; // 时间戳
	//2.生成序列号
	//2.1 获取当前日期（天）
	String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
	//利用redis的自增长特性，生成序列号
	long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

	//3.拼接并返回
	return timestamp << COUNT_BITS | count;
	//采用符号位0+31位时间戳+32位序列号实现拼接
	//long类型占64位，左移32位，右边补0（留出序列号位置），然后与count进行或运算

    }

    public static void main(String[] args) {

    }


}
