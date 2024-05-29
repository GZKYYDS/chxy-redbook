package com.xhs.service;

import com.xhs.dto.Result;
import com.xhs.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀代金券
     *
     * @param voucherId 代金券ID
     * @return Result
     */
    Result seckillVoucher(Long voucherId);

    /**
     * 创建代金券订单
     *
     * @param voucherOrder 代金券ID
     */
    void createVoucherOrder(VoucherOrder voucherOrder);
}
