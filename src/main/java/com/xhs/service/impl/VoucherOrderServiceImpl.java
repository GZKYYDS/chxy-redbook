package com.xhs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhs.dto.Result;
import com.xhs.entity.VoucherOrder;
import com.xhs.mapper.VoucherOrderMapper;
import com.xhs.service.ISeckillVoucherService;
import com.xhs.service.IVoucherOrderService;
import com.xhs.utils.RedisIdWorker;
import com.xhs.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.*;


@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
	//lua脚本，用于释放锁
	SECKILL_SCRIPT = new DefaultRedisScript<>();
	SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));//加载lua脚本
	SECKILL_SCRIPT.setResultType(Long.class);//设置返回值类型
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);//阻塞队列
    //线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    /*线程池 用于处理订单 用于处理阻塞队列中的订单 保证订单的顺序
    为什么要用static final修饰呢？因为这个线程池是共享的，不会因为对象的创建而创建多个线程池*/

    @PostConstruct        //在构造方法之后执行
    private void init() {
	SECKILL_ORDER_EXECUTOR.execute(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable {
	@Override
	public void run() {
	    while (true) {

		try {
		    //获取消息队列中的订单信息
		    VoucherOrder voucherOrder = orderTasks.take();
		    //创建订单
		    handleVoucherOrder(voucherOrder);


		} catch (Exception e) {
		    log.error("处理订单异常", e);

		}

	    }
	}
    }

    /**
     * 处理订单
     *
     * @param voucherOrder 订单信息
     */
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
	//用户id
	Long userId = UserHolder.getUser().getId();
	//创建一个分布式锁（可重入锁）
	RLock lock = redissonClient.getLock("lock:order:" + userId);
	//获取锁
	boolean isLock = lock.tryLock();//参数:等待时间，超时时间，时间单位 无参：立即返回
	//如果获取锁失败
	if (!isLock) {
	    log.error("不能重复下单");
	}
	//7.返回订单id
	//代理对象
	try {
	    //获取不了代理对象，因为这个方法是在run方法中执行的，而run方法是在VoucherOrderHandler中执行的
	    //作为子线程，无法通过AopContext.currentProxy()获取代理对象，因此要在主线程中获取代理对象
//	    IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
	    proxy.createVoucherOrder(voucherOrder);//返回订单id
	} finally {
	    //释放锁
	    lock.unlock();
	}
    }

    //代理对象
    private IVoucherOrderService proxy;

    /**
     * 秒杀代金券
     *
     * @param voucherId 代金券ID
     * @return Result
     */
    @Override
    @Transactional        //开启事务
    public Result seckillVoucher(Long voucherId) {
	Long userId = UserHolder.getUser().getId();
	//订单id
	Long orderId = redisIdWorker.nextId("order");
	//1.执行lua脚本
	Long result = stringRedisTemplate.execute(
		SECKILL_SCRIPT,
		Collections.emptyList(),
		voucherId.toString(),
		userId.toString(),
		orderId.toString()
	);
	//2.判断是否为0
	int r = Objects.requireNonNull(result).intValue();
	//3.不为0 没有购买资格
	if (r != 0) {
	    return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
	}

	//4.为0，有购买资格，将下单信息保存到阻塞队列
	VoucherOrder voucherOrder = new VoucherOrder();

	//用户id
	voucherOrder.setUserId(userId);
	//代金券id
	voucherOrder.setVoucherId(voucherId);
	//放入阻塞队列
	orderTasks.add(voucherOrder);
	//获取代理对象
	//为什么在这里获取代理对象呢？因为这个方法是在主线程中执行的，只有在主线程中才能获取代理对象
	//获取代理对象的目的：在主线程中获取代理对象，然后在子线程中执行代理对象的方法
	//这样就能保证在子线程中执行的方法是代理对象的方法，这样就能获取代理对象
	proxy = (IVoucherOrderService) AopContext.currentProxy();
	return Result.ok(orderId);
    }

    /* */

    /**
     * 秒杀代金券
     *
     * @param voucherOrder 代金券ID
     *//**//*
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

    }*/
    @Transactional        //开启事务
    public void createVoucherOrder(VoucherOrder voucherOrder) {
	//一人一单
	//用户id
	Long userId = voucherOrder.getUserId();
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
		.eq("voucher_id", voucherOrder)
		.count();
	if (count > 0) {
	    log.error("每人限购一单");
	    return;
	}

	//5.扣减库存
	boolean success = seckillVoucherService.update()
		.setSql("stock = stock - 1")
		.eq("voucher_id", voucherOrder)
		.gt("stock", 0)        //乐观锁
		.update();
	if (!success) {
	    log.error("扣减库存失败");
	    return;
	}
	//6.保存订单
	save(voucherOrder);        //保存订单
	//不需要返回 异步处理订单
    }

}

