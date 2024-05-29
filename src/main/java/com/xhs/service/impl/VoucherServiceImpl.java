package com.xhs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhs.dto.Result;
import com.xhs.entity.Voucher;
import com.xhs.mapper.VoucherMapper;
import com.xhs.entity.SeckillVoucher;
import com.xhs.service.ISeckillVoucherService;
import com.xhs.service.IVoucherService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.xhs.constants.RedisConstants.SECKILL_STOCK_KEY;


@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
	// 查询优惠券信息
	List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
	// 返回结果
	return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
	// 保存优惠券
	save(voucher);
	// 保存秒杀信息
	SeckillVoucher seckillVoucher = new SeckillVoucher();
	seckillVoucher.setVoucherId(voucher.getId());
	seckillVoucher.setStock(voucher.getStock());
	seckillVoucher.setBeginTime(voucher.getBeginTime());
	seckillVoucher.setEndTime(voucher.getEndTime());
	seckillVoucherService.save(seckillVoucher);

	//券存到redis
	stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), String.valueOf(voucher.getStock()));

    }
}
