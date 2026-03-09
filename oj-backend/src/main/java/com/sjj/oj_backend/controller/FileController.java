package com.sjj.oj_backend.controller;

import com.sjj.oj_backend.common.BaseResponse;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.common.ResultUtils;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.model.dto.file.UploadFileRequest;
import com.sjj.oj_backend.model.enums.FileUploadBizEnum;
import com.sjj.oj_backend.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @javax.annotation.Resource
    private FileService fileService;

    /**
     * 兼容旧文件上传接口（仅保留用户头像业务）
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile,
                                           UploadFileRequest uploadFileRequest) {
        if (uploadFileRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传参数不能为空");
        }
        FileUploadBizEnum bizEnum = FileUploadBizEnum.getEnumByValue(uploadFileRequest.getBiz());
        if (bizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的上传业务类型");
        }
        if (!FileUploadBizEnum.USER_AVATAR.equals(bizEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前仅支持用户头像上传");
        }
        String avatarUrl = fileService.uploadAvatar(multipartFile);
        return ResultUtils.success(avatarUrl);
    }

    /**
     * 上传头像
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestPart("file") MultipartFile multipartFile) {
        String avatarUrl = fileService.uploadAvatar(multipartFile);
        return ResultUtils.success(avatarUrl);
    }

    /**
     * 获取头像
     */
    @GetMapping("/avatar/{fileName}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String fileName) {
        Resource resource = fileService.loadAvatarAsResource(fileName);
        MediaType mediaType = resolveMediaType(fileName);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    private MediaType resolveMediaType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lowerFileName.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
