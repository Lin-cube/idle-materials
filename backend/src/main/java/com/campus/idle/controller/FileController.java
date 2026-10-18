package com.campus.idle.controller;

import com.campus.idle.common.BusinessException;
import com.campus.idle.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传接口: 物资图片 / 归还拍照
 * 上传后返回可访问的URL(经WebConfig映射到 /files/**)
 * 上传目录从 application.yml 的 app.upload-dir 读取(绝对路径),
 * 与 WebConfig 的静态资源映射保持一致, 避免因启动目录不同导致图片找不到
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    /** 上传目录, 从application.yml读取(绝对路径), 与WebConfig保持一致 */
    @Value("${app.upload-dir}")
    private String uploadDir;

    /** 上传单张图片, 返回访问URL */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        // 1. 基本校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            throw new BusinessException("只支持上传图片文件(jpg/png/gif/webp)");
        }
        // 2. 生成不重复的文件名: uuid + 原始扩展名
        String ext = original.substring(original.lastIndexOf(".")).toLowerCase();
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        // 3. 保存到上传目录
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            file.transferTo(new File(dir, filename));
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
        // 4. 返回访问URL
        return Result.ok(Map.of("url", "/files/" + filename));
    }
}
