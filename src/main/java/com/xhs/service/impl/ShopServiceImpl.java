package com.xhs.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xhs.dto.Result;
import com.xhs.entity.Shop;
import com.xhs.mapper.ShopMapper;
import com.xhs.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.xhs.constants.RedisConstants.CACHE_SHOP_KEY;

/**
 * 商铺服务实现类
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据id查询商铺信息
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @Override
    public Result queryById(Long id) {
	//1.从redis中查询商铺信息
	String key = CACHE_SHOP_KEY + id;
	String shopJson = stringRedisTemplate.opsForValue().get(key);
	//2.判断redis中是否存在
	if (StrUtil.isNotBlank(shopJson)) {
	    //3.如果存在，直接返回
	    //先将json字符串转为对象
	    Shop shop = JSONUtil.toBean(shopJson, Shop.class);
	    return Result.ok(shop);
	}
	//4.如果不存在，从数据库中查询
	Shop shop = getById(id);
	//5.数据库中如果不存在返回错误
	if (shop == null) {
	    return Result.fail("商铺不存在");
	}
	//6.如果存在，将数据写入redis
	stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));
	//7.返回数据
	return Result.ok(shop);
    }
}
