package com.campus.idle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置
 * 1. 把上传的物资图片映射为静态资源: 访问 /files/xxx.jpg 实际读取上传目录
 * 2. 提供上传目录的初始化
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 上传文件存放目录(相对项目运行目录) */
    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    /** 静态资源映射: /files/** -> 本地上传目录 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();   // 目录不存在则自动创建
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + dir.getAbsolutePath() + File.separator);
    }
}
