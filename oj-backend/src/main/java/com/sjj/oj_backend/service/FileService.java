package com.sjj.oj_backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 本地文件服务
 */
public interface FileService {

    /**
     * 上传头像并返回访问 URL
     *
     * @param multipartFile 文件
     * @return 头像访问地址
     */
    String uploadAvatar(MultipartFile multipartFile);

    /**
     * 读取头像文件
     *
     * @param fileName 文件名
     * @return 头像资源
     */
    Resource loadAvatarAsResource(String fileName);

    /**
     * 上传帖子图片并返回访问 URL
     */
    String uploadPostImage(MultipartFile multipartFile);

    /**
     * 读取帖子图片文件
     */
    Resource loadPostImageAsResource(String fileName);
}