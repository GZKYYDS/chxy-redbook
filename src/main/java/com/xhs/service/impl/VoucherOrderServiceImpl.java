package com.xhs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhs.dto.Result;
import com.xhs.entity.SeckillVoucher;
import com.xhs.entity.VoucherOrder;
import com.xhs.mapper.VoucherOrderMapper;
import com.xhs.service.ISeckillVoucherService;
import com.xhs.service.IVoucherOrderService;
import com.xhs.utils.RedisIdWorker;
import com.xhs.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;
    /**
     * 秒杀代金券
     *
     * @param voucherId 代金券ID
     * @return Result
     */
    @Override
    @Transactional        //开启事务
    public Result seckillVoucher(Long voucherId) {
	//1.查询代金券信息
	SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
	//2.判断秒杀是否开始
	if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
	    return Result.fail("秒杀未开始");
	}
	//3.判断秒杀是否结束
	if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
	    return Result.fail("秒杀已结束");
	}
	//4.如果开始，判断库存是否充足
	if (voucher.getStock() < 1) {
	    return Result.fail("库存不足");
	}
	//用户id
	Long userId = UserHolder.getUser().getId();
	//创建一个分布式锁（可重入锁）
	RLock lock = redissonClient.getLock("lock:order:" + userId);
	//获取锁
	boolean isLock = lock.tryLock();//参数:等待时间，超时时间，时间单位 无参：立即返回
	//如果获取锁失败
	if (!isLock) {
	    return Result.fail("请勿重复下单");
	}
	//7.返回订单id
	//代理对象
	try {
	    IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
	    //获取锁之后才会创建事务，事务提交之后才会释放锁
	    //这样就保证了同一个用户只能秒杀一次，解决并发安全问题
	    return proxy.createVoucherOrder(voucherId);//返回订单id
	} finally {
	    //释放锁
	    lock.unlock();
	}

    }


    @Transactional        //开启事务
    public Result createVoucherOrder(Long voucherId) {
	//一人一单
	//用户id
	Long userId = UserHolder.getUser().getId();

	/*
	  由于要实现一人一单，所以这个userid应该是不同的，因此需要加锁
	  但是这里的锁是不够的，因为这个锁只能保证一个线程进入这个方法，但是不能保证一个用户只能秒杀一次
	  因此需要加锁的是userid，而不是这个方法
	  但是这里的userid是Long类型，是一个对象，所以不能直接加锁，需要加锁的是这个对象的值
	  因此初步判断需要加锁的是userid.toString()，但是底层toString是new一个新的对象，所以这里的userid还是不同的
	  因此需要加锁的是userid.toString().intern()，这样就能保证同一个用户只能秒杀一次

	 */
//	synchronized (userId.toString().intern()) {        //intern()方法返回字符串对象的规范化表示形式
	int count = query().eq("user_id", userId)
		.eq("voucher_id", voucherId)
		.count();
	if (count > 0) {
	    return Result.fail("每人限购一单");
	}

	//5.扣减库存
	boolean success = seckillVoucherService.update()
		.setSql("stock = stock - 1")
		.eq("voucher_id", voucherId)
		.gt("stock", 0)        //乐观锁
		.update();
	if (!success) {
	    return Result.fail("扣减库存失败");
	}
	//6.创建订单
	VoucherOrder voucherOrder = new VoucherOrder();
	//订单id
	Long orderId = redisIdWorker.nextId("order");
	voucherOrder.setId(orderId);

	voucherOrder.setUserId(userId);
	//代金券id
	voucherOrder.setVoucherId(voucherId);
	save(voucherOrder);        //保存订单
	return Result.ok(orderId);//返回订单id
    }
	/*
	由于事务是在方法上的，所以当这个方法执行完之后，事务就会提交，这个锁也会释放；
	然而这里的事务是由spring管理的，所以spring会在这个方法执行完之后，提交事务，释放锁
	那么这个锁已经释放了，意味着下一个线程可以进入这个方法，在上一个线程还没有执行完之前，下一个线程就可以进入这个方法
	而在spring未提交事务之前，这个方法是不会返回的，因此下一个线程就可以进入这个方法，这样就会导致一个用户可以秒杀多次
	 */
}

