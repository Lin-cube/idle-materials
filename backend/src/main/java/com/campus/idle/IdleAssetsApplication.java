package com.campus.idle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园闲置物资管理平台 · 后端启动类
 *
 * @MapperScan: 自动扫描 mapper 包下的所有 Mapper 接口并注册为 Bean
 * @EnableScheduling: 开启定时任务(逾期提醒每天8点执行)
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.campus.idle.mapper")
public class IdleAssetsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdleAssetsApplication.class, args);
        System.out.println("""
                
                ==========================================================
                  校园闲置物资管理平台后端启动成功!
                  接口地址: http://localhost:8081
                  接口文档: 见项目 README.md
                ==========================================================
                """);
    }
}
