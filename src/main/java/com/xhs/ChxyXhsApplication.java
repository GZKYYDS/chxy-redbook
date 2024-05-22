package com.xhs;

import com.xhs.constants.Address;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Slf4j
@MapperScan("com.xhs.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)	//开启AOP代理	//暴露代理对象
public class ChxyXhsApplication {

    public static void main(String[] args) {
	SpringApplication.run(ChxyXhsApplication.class, args);
	log.info("\n项目启动成功：" + Address.PROJECT_NGINX_TEST_ADDRESS + "\n");
    }

}
