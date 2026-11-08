package com.yunshu.zhixue;

import com.yunshu.zhixue.config.MinioProperties;
import com.yunshu.zhixue.security.JwtProperties;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 云枢智学后端启动类。
 *
 * <p>模块化单体：业务代码统一放在 {@code modules.<领域>} 下，
 * 模块间只经 Service 接口与 DTO 交互，禁止跨模块访问 Mapper/实体。</p>
 */
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, MinioProperties.class})
@MapperScan(basePackages = "com.yunshu.zhixue", annotationClass = Mapper.class)
public class YunshuZhixueApplication {

    public static void main(String[] args) {
        SpringApplication.run(YunshuZhixueApplication.class, args);
    }
}
