package com.xhs.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhs.constants.SystemConstants;
import com.xhs.dto.Result;
import com.xhs.entity.Blog;
import com.xhs.entity.User;
import com.xhs.mapper.BlogMapper;
import com.xhs.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhs.service.IUserService;
import com.xhs.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.xhs.constants.RedisConstants.BLOG_LIKED_KEY;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {


    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result queryBlogById(Long id) {
	//查询blog
	Blog blog = getById(id);
	if (blog == null) {
	    return Result.fail("笔记不存在");
	}
	//查询user
	queryBlogUser(blog);
	//判断是否点赞
	isBlogLiked(blog);
	return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
	//1.获取登录用户
	if(UserHolder.getUser() == null){
	    return;
	}
	Long userId = UserHolder.getUser().getId();
	//2.通过redis判断当前登录用户是否点过赞
	String key = BLOG_LIKED_KEY + blog.getId();
	Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
	blog.setIsLike(BooleanUtil.isTrue(isMember));
    }

    @Override
    public Result queryHotBlog(Integer current) {
	// 根据用户查询
	Page<Blog> page = query()
		.orderByDesc("liked")
		.page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
	// 获取当前页数据
	List<Blog> records = page.getRecords();
	// 查询用户
	records.forEach(blog -> {
	    this.queryBlogUser(blog);
	    this.isBlogLiked(blog);//判断是否点赞
	});
	return Result.ok(records);
    }

    private void queryBlogUser(Blog blog) {
	Long userId = blog.getUserId();
	User user = userService.getById(userId);
	blog.setName(user.getNickName());
	blog.setIcon(user.getIcon());
    }


    @Override
    public Result likeBlog(Long id) {
	//1.获取登录用户
	Long userId = UserHolder.getUser().getId();
	//2.通过redis判断当前登录用户是否点过赞
	String key = BLOG_LIKED_KEY + id;
	Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
	//3.如果没点过赞，可以点赞
	if (BooleanUtil.isFalse(isMember)) {
	    //点赞执行liked字段+1逻辑
	    boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
	    //如果成功，将用户id加入到redis中，表示已经点过赞
	    if (isSuccess) {
		stringRedisTemplate.opsForSet().add(key, userId.toString());
	    }

	} else {
	    //4.点过赞，需要取消点赞
	    boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
	    if (isSuccess) {
		stringRedisTemplate.opsForSet().remove(key, userId.toString());
	    }
	}

	return Result.ok();
    }


}
