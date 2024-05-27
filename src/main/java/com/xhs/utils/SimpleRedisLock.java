package com.xhs.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + '-';
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
	//lua脚本，用于释放锁
	UNLOCK_SCRIPT = new DefaultRedisScript<>();
	UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));//加载lua脚本
	UNLOCK_SCRIPT.setResultType(Long.class);//设置返回值类型
    }


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
	String threadId = ID_PREFIX + Thread.currentThread().getId();
	Boolean success = stringRedisTemplate.opsForValue()
		//value 需要用线程唯一标识
		.setIfAbsent(LOCK_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
	return Boolean.TRUE.equals(success);
	//因为从Boolean到boolean的转换是自动装箱拆箱，为了避免空指针异常，这里使用Boolean.TRUE.equals(success)来判断
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
	//调用lua脚本释放锁
	stringRedisTemplate.execute(
		UNLOCK_SCRIPT,
		Collections.singletonList(LOCK_PREFIX + name),
		ID_PREFIX + Thread.currentThread().getId()
	);
	//为什么要用lua脚本释放锁呢？
	//因为释放锁的操作需要保证原子性，如果直接使用stringRedisTemplate.delete(LOCK_PREFIX + name)来删除锁，可能会出现误删的情况
	//比如：线程A获取到锁，但是还没有执行删除锁的操作，此时锁过期了，线程B获取到了锁，然后线程A执行删除锁的操作，这样就会把线程B的锁给删除了

    }
/*
  释放锁
 *//*
    @Override
    public void unlock() {
	//获得当前线程的唯一标识
	String threadId = ID_PREFIX + Thread.currentThread().getId();
	//获取锁的标识
	String id = stringRedisTemplate.opsForValue().get(LOCK_PREFIX + name);
	if (threadId.equals(id)) {
	    stringRedisTemplate.delete(LOCK_PREFIX + name);
	}


    }*/

}

