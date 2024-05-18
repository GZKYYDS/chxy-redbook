package com.xhs.service;

import com.xhs.dto.Result;
import com.xhs.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀代金券
     * @param voucherId 代金券ID
     * @return Result
     */
    Result seckillVoucher(Long voucherId);
}
