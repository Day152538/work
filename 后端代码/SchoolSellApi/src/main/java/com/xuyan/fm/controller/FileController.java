package com.xuyan.fm.controller;

import com.xuyan.fm.common.utils.PathUtils;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.utils.IdFactoryUtil;
import com.xuyan.fm.service.FileService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件上传 / 图片访问控制器。
 * 整改点（修复两个安全漏洞）：
 * 1. 路径穿越（Arbitrary File Read）：原 /image 接口直接拼接 imageName，
 *    传 "imageName=../../application.yml" 即可读取服务器任意文件。
 *    现在校验文件名仅含白名单扩展名、不含路径分隔符，且 canonical 路径必须落在 pic 目录内。
 * 2. 上传无类型限制：原来可上传 .jsp 等任意文件；现在扩展名白名单（jpg/jpeg/png/gif/webp）+ 5MB 大小限制，
 *    服务器端用 UUID 重命名，客户端文件名不参与最终落盘路径。
 * 3. 图片读取改用 Files.copy，替换不可靠的 available()/read() 组合，并按扩展名设置 Content-Type。
 */
@RestController
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/file")
    public ResultVo uploadFile(@RequestParam("file") MultipartFile multipartFile) {
        // 1. 扩展名白名单校验
        if (!fileService.checkFileExtension(multipartFile.getOriginalFilename())) {
            return ResultVo.fail(ErrorMsg.FILE_TYPE_ERROR);
        }
        // 2. 大小校验（5MB）
        if (!fileService.checkFileSize(multipartFile)) {
            return ResultVo.fail(ErrorMsg.FILE_SIZE_ERROR);
        }
        // 3. 服务端生成文件名：UUID 前缀 + 白名单扩展名，客户端文件名不参与路径
        String original = multipartFile.getOriginalFilename();
        String ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        String fileName = "file" + IdFactoryUtil.getFileId() + ext;
        try {
            if (fileService.uploadFile(multipartFile, fileName)) {
                return ResultVo.success("/image?imageName=" + fileName);
            }
        } catch (IOException e) {
            return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
        }
        return ResultVo.fail(ErrorMsg.FILE_UPLOAD_ERROR);
    }

    @GetMapping("/image")
    public void getImage(@RequestParam("imageName") String imageName,
                         HttpServletResponse response) throws IOException {
        // 安全校验一：文件名不允许包含路径分隔符
        if (imageName.contains("/") || imageName.contains("\\") || imageName.contains("..")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        // 安全校验二：扩展名必须在白名单内
        String lower = imageName.toLowerCase();
        boolean extOk = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".webp");
        if (!extOk) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Path picDir = Paths.get(PathUtils.getClassLoadRootPath(), "pic").normalize();
        Path image = picDir.resolve(imageName).normalize();
        // 安全校验三：canonical 路径必须仍在 pic 目录内（双重防穿越）
        if (!image.startsWith(picDir)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        File file = image.toFile();
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(image);
        response.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLengthLong(file.length());
        Files.copy(image, response.getOutputStream());
    }
}
