package com.xhs.service;

import com.xhs.dto.Result;
import com.xhs.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 商铺服务接口
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据id查询商铺信息
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    Result queryById(Long id) throws InterruptedException;

    /**
     * 更新商铺信息
     *
     * @param shop 商铺数据
     * @return 无
     */
    Result update(Shop shop);
}
