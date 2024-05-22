package com.xhs.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_PREFIX = "lock:";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
	this.name = name;
	this.stringRedisTemplate = stringRedisTemplate;
    }


    /**
     * 尝试获取锁
     *
     * @param timeoutSec 锁持有超时时间 过期自动释放
     * @return true 获取成功 false 获取失败
     */
    @Override
    public boolean tryLock(long timeoutSec) {
	long threadId = Thread.currentThread().getId();
	Boolean success = stringRedisTemplate.opsForValue()
		//value 需要用线程唯一标识
		.setIfAbsent(LOCK_PREFIX + name, threadId + "", timeoutSec, TimeUnit.SECONDS);
	return Boolean.TRUE.equals(success);
	//因为从Boolean到boolean的转换是自动装箱拆箱，为了避免空指针异常，这里使用Boolean.TRUE.equals(success)来判断
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
	stringRedisTemplate.delete(LOCK_PREFIX + name);

    }
}