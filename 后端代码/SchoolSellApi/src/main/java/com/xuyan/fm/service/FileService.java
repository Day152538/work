package com.xuyan.fm.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileService {

    /**
     * 校验上传文件扩展名是否在白名单内（jpg/jpeg/png/gif/webp）
     * @param originalFilename 客户端原始文件名
     * @return 是否允许
     */
    boolean checkFileExtension(String originalFilename);

    /**
     * 校验上传文件大小（不超过 5MB 且非空）
     */
    boolean checkFileSize(MultipartFile multipartFile);

    /**
     * 上传文件到 pic 目录
     * @param multipartFile 上传的文件
     * @param fileName 服务端生成的安全文件名
     * @return 是否成功
     * @throws IOException IO 异常
     */
    boolean uploadFile(MultipartFile multipartFile, String fileName) throws IOException;
}
