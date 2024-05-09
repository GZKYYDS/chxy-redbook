package com.xhs.service;

import com.xhs.dto.Result;
import com.xhs.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 商店类型服务类
 */
public interface IShopTypeService extends IService<ShopType> {


    /**
     * 查询商店类型列表
     *
     * @return 商店类型列表
     */
    Result queryTypeList();
}
