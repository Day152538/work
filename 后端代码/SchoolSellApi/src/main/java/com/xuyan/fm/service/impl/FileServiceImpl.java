package com.xuyan.fm.service.impl;

import com.xuyan.fm.common.utils.PathUtils;
import com.xuyan.fm.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件上传服务实现。
 * 整改点：
 * 1. 修复路径穿越：上传文件名完全由服务端生成（UUID + 白名单扩展名），
 *    客户端原始文件名只提取扩展名并做白名单校验，杜绝 "../" 或绝对路径注入。
 * 2. 上传目录存在性校验改用 Files.createDirectories，并校验最终落点仍在 pic 目录内。
 * 3. 替换「先 delete 再 createNewFile」的竞态写法，直接 transferTo 覆盖写。
 */
@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    /** 允许上传的图片扩展名白名单 */
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};

    /** 单文件大小上限 5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    @Override
    public boolean checkFileExtension(String originalFilename) {
        if (originalFilename == null) {
            return false;
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return false;
        }
        String ext = originalFilename.substring(dotIndex + 1).toLowerCase();
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean checkFileSize(MultipartFile multipartFile) {
        return multipartFile != null && !multipartFile.isEmpty()
                && multipartFile.getSize() <= MAX_FILE_SIZE;
    }

    @Override
    public boolean uploadFile(MultipartFile multipartFile, String fileName) throws IOException {
        Path picDir = Paths.get(PathUtils.getClassLoadRootPath(), "pic");
        Files.createDirectories(picDir);

        Path target = picDir.resolve(fileName).normalize();
        // 双保险：最终落点必须仍在 pic 目录内（防路径穿越）
        if (!target.startsWith(picDir.normalize())) {
            log.warn("检测到非法上传路径: {}", fileName);
            return false;
        }

        File file = target.toFile();
        try {
            multipartFile.transferTo(file);
            return true;
        } catch (IOException e) {
            log.error("文件上传失败: {}", fileName, e);
            return false;
        }
    }
}
