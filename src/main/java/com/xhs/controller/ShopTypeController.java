package com.xhs.controller;


import com.xhs.dto.Result;
import com.xhs.entity.ShopType;
import com.xhs.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 商店类型控制器
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {

	List<ShopType> typeList = typeService
		.query().orderByAsc("sort").list();
	return Result.ok(typeList);
    }
}
