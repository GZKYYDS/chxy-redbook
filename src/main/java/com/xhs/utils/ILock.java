package com.xhs.utils;

public interface ILock {
    /**
     * 尝试获取锁
     *
     * @param timeoutSec 锁持有超时时间 过期自动释放
     * @return true 获取成功 false 获取失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
