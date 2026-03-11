package com.sjj.oj_backend.utils;

import cn.hutool.json.JSONUtil;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * 帖子内容字段编解码
 * 统一处理 tags / images 与 JSON 字符串的相互转换
 */
public final class PostContentCodec {

    private PostContentCodec() {
    }

    /**
     * tags 列表编码为 JSON 字符串
     */
    public static String encodeTags(List<String> tags) {
        return encodeStringList(tags);
    }

    /**
     * images 列表编码为 JSON 字符串
     */
    public static String encodeImages(List<String> images) {
        return encodeStringList(images);
    }

    /**
     * tags JSON 字符串解码为列表
     */
    public static List<String> decodeTags(String tagsJson) {
        return decodeStringList(tagsJson);
    }

    /**
     * images JSON 字符串解码为列表
     */
    public static List<String> decodeImages(String imagesJson) {
        return decodeStringList(imagesJson);
    }

    private static String encodeStringList(List<String> values) {
        if (values == null) {
            return null;
        }
        return JSONUtil.toJsonStr(values);
    }

    private static List<String> decodeStringList(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(jsonStr, String.class);
    }
}
