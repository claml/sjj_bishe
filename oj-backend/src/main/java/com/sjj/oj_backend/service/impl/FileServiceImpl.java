package com.sjj.oj_backend.service.impl;

import cn.hutool.core.io.FileUtil;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.service.FileService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 本地文件服务实现
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L;

    private static final long MAX_POST_IMAGE_SIZE = 5 * 1024 * 1024L;

    private static final List<String> ALLOWED_AVATAR_SUFFIX = Arrays.asList("jpg", "jpeg", "png", "webp");

    private static final List<String> ALLOWED_POST_IMAGE_SUFFIX = Arrays.asList("jpg", "jpeg", "png", "webp");

    private static final Path AVATAR_UPLOAD_DIR = Paths.get("uploads", "avatar");

    private static final Path POST_IMAGE_UPLOAD_DIR = Paths.get("uploads", "post");

    @Override
    public String uploadAvatar(MultipartFile multipartFile) {
        validateImageFile(multipartFile, MAX_AVATAR_SIZE, ALLOWED_AVATAR_SUFFIX, "文件大小不能超过 2MB");
        return storeFileAndGetUrl(multipartFile, AVATAR_UPLOAD_DIR, "/file/avatar/");
    }

    @Override
    public Resource loadAvatarAsResource(String fileName) {
        return loadImageAsResource(fileName, AVATAR_UPLOAD_DIR, "头像");
    }

    @Override
    public String uploadPostImage(MultipartFile multipartFile) {
        validateImageFile(multipartFile, MAX_POST_IMAGE_SIZE, ALLOWED_POST_IMAGE_SUFFIX, "文件大小不能超过 5MB");
        return storeFileAndGetUrl(multipartFile, POST_IMAGE_UPLOAD_DIR, "/file/post/");
    }

    @Override
    public Resource loadPostImageAsResource(String fileName) {
        return loadImageAsResource(fileName, POST_IMAGE_UPLOAD_DIR, "帖子图片");
    }

    private String storeFileAndGetUrl(MultipartFile multipartFile, Path uploadDir, String accessPath) {
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename()).toLowerCase();
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
        try {
            Files.createDirectories(uploadDir);
            Path targetPath = uploadDir.resolve(fileName).normalize();
            Files.copy(multipartFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(accessPath)
                    .path(fileName)
                    .toUriString();
        } catch (IOException e) {
            log.error("upload file failed, fileName={}", fileName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    private Resource loadImageAsResource(String fileName, Path uploadDir, String fileTypeName) {
        if (StringUtils.isBlank(fileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, fileTypeName + "文件名不能为空");
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法文件名");
        }

        try {
            Path filePath = uploadDir.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, fileTypeName + "不存在");
            }
            return resource;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("load file failed, fileName={}", fileName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文件失败");
        }
    }

    private void validateImageFile(MultipartFile multipartFile, long maxSize, List<String> allowedSuffix, String maxSizeMsg) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        if (multipartFile.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, maxSizeMsg);
        }
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        if (StringUtils.isBlank(suffix) || !allowedSuffix.contains(suffix.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 jpg、jpeg、png、webp 图片格式");
        }
    }
}
