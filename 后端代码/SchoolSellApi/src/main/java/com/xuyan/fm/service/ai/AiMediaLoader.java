package com.xuyan.fm.service.ai;

import com.xuyan.fm.common.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI 识图图片加载器（供 预填 / 合规质检 复用，避免两处安全校验代码漂移）。
 *
 * <p>与 FileController 相同的三重安全校验：文件名不允许路径分隔符与 ".."、扩展名白名单、
 * 解析后的 canonical 路径必须仍落在 pic 目录内。个别图片读取失败只跳过该张，不整单失败。
 */
@Component
public class AiMediaLoader {

    private static final Logger log = LoggerFactory.getLogger(AiMediaLoader.class);

    /** 单次参与识图的最大图片数 */
    public static final int MAX_IMAGES = 3;

    /**
     * 把服务器 pic 目录里的图片文件名加载为 Spring AI 多模态 Media。
     *
     * @return 成功加载的图片列表（读取失败的图片会被跳过并记日志）
     */
    public List<Media> load(List<String> imageNames) {
        List<Media> medias = new ArrayList<>();
        if (imageNames == null) {
            return medias;
        }
        int count = 0;
        for (String imageName : imageNames) {
            if (imageName == null || imageName.isBlank() || count >= MAX_IMAGES) {
                continue;
            }
            try {
                medias.add(new Media(MimeType.valueOf(mimeOf(imageName)),
                        new ByteArrayResource(readBytes(imageName))));
                count++;
            } catch (IOException e) {
                log.warn("AI 跳过无法读取的图片: {}，原因: {}", imageName, e.getMessage());
            }
        }
        return medias;
    }

    private byte[] readBytes(String imageName) throws IOException {
        // 校验一：不允许路径分隔符与 ".."（防路径穿越）
        if (imageName.contains("/") || imageName.contains("\\") || imageName.contains("..")) {
            throw new IOException("非法文件名: " + imageName);
        }
        // 校验二：扩展名白名单
        String lower = imageName.toLowerCase(Locale.ROOT);
        boolean extOk = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".webp");
        if (!extOk) {
            throw new IOException("非白名单图片格式: " + imageName);
        }
        // 校验三：canonical 路径必须仍落在 pic 目录内
        Path picDir = Paths.get(PathUtils.getClassLoadRootPath(), "pic").normalize();
        Path image = picDir.resolve(imageName).normalize();
        if (!image.startsWith(picDir)) {
            throw new IOException("图片路径越界: " + imageName);
        }
        if (!Files.exists(image) || !Files.isRegularFile(image)) {
            throw new IOException("图片不存在: " + imageName);
        }
        return Files.readAllBytes(image);
    }

    private String mimeOf(String imageName) {
        String lower = imageName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
