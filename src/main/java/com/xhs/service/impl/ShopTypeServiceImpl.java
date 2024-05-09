package com.xhs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhs.entity.ShopType;
import com.xhs.mapper.ShopTypeMapper;
import com.xhs.service.IShopTypeService;
import org.springframework.stereotype.Service;

/**
 * 商店类型服务实现类
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

//   @Resource
//    private StringRedisTemplate stringRedisTemplate;
//    @Override
//    public Result queryTypeList() {
//        //CACHE_SHOP_TYPE_KEY 是封装的字符串常量
//        String sort = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);
//        if (sort != null){
//            JSONArray objects = JSONUtil.parseArray(sort);
//            List<ShopType> shopTypes = JSONUtil.toList(objects, ShopType.class);
//            return Result.ok(shopTypes);
//        }
//
//        List<ShopType> typeList = query().orderByAsc("sort").list();
//
//        //如果数据库中没有则返回错误信息
//        if (typeList == null){
//            return Result.fail("没有店铺种类信息！");
//        }
//
//        //存入redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY,JSONUtil.toJsonStr(typeList));
//
////        List<ShopType> typeList = typeService
////                .query().orderByAsc("sort").list();
//
//        return Result.ok(typeList);
//    }
}
