package com.xhs.service.impl;

import com.xhs.dto.Result;
import com.xhs.entity.SeckillVoucher;
import com.xhs.entity.VoucherOrder;
import com.xhs.mapper.VoucherOrderMapper;
import com.xhs.service.ISeckillVoucherService;
import com.xhs.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhs.utils.RedisIdWorker;
import com.xhs.utils.UserHolder;
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
	//用户id
	Long userId = UserHolder.getUser().getId();
	voucherOrder.setUserId(userId);
	//代金券id
	voucherOrder.setVoucherId(voucherId);
	save(voucherOrder);        //保存订单
	//7.返回订单id
	return Result.ok(orderId);        //返回订单id
    }
}
